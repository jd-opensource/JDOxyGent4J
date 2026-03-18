package com.jd.oxygent.core.oxygent.oxy.agents;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jd.oxygent.core.oxygent.schemas.LLM.LLMResponse;
import com.jd.oxygent.core.oxygent.schemas.LLM.LLMState;
import com.jd.oxygent.core.oxygent.schemas.memory.Memory;
import com.jd.oxygent.core.oxygent.schemas.memory.Message;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyState;
import com.jd.oxygent.core.oxygent.utils.SmartCharsetReader;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jd.oxygent.core.oxygent.utils.CommonUtils.cleanAnsiCodes;

@Slf4j
@SuperBuilder
@Data
public class ShellUseAgent extends ReActAgent {

    private AuthInfo authInfo;
    @JsonIgnore
    private Session session;

    @Override
    public void init() {
        super.init();
        try {
            JSch jsch = new JSch();
            if (authInfo.keyPath != null) {
                jsch.addIdentity(authInfo.keyPath);
            }
            session = jsch.getSession(authInfo.username, authInfo.hostname, authInfo.port);
            session.setPassword(authInfo.password);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no"); // Skip fingerprint confirmation
            config.put("kex", "curve25519-sha256,diffie-hellman-group-exchange-sha256"); // Prioritize modern algorithms
            session.setConfig(config);

            // Set heartbeat to prevent disconnection during long-running commands (e.g., oxybank migration)
            session.setServerAliveInterval(30000);
            session.connect();
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setErrStream(System.out, true);

            InputStream in = channel.getInputStream();
            channel.connect();
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            // Real-time, non-blocking byte reading for real-time log printing
            byte[] buffer = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(buffer, 0, 1024);
                    if (i < 0) break;
                    line = new String(buffer, 0, i, StandardCharsets.UTF_8);
                    System.out.print(line);
                    stringBuilder.append(line);
                    System.out.flush();
                }
                if (channel.isClosed()) {
                    if (in.available() > 0) continue;
                    break;
                }
                Thread.sleep(50);
            }
            int exitStatus = channel.getExitStatus();
            this.mas.getGlobalData().put("hello_terminal", cleanAnsiCodes(stringBuilder.toString()));
            this.mas.getGlobalData().put("ssh_channel", channel);
            in.close();
            channel.disconnect();
            if (exitStatus == 0) {
                log.info("SSH channel initialized");
            } else {
                log.error("SSH channel initialization failed, exitStatus:{}", exitStatus);
            }
        } catch (Exception e) {
            log.error("Error initializing SSH client", e);
        }
    }

    public static LLMResponse parseLLMResponse(String oriResponse, OxyRequest oxyRequest) {
        try {
            // Handle think model format
            if (oriResponse.contains("</think>")) {
                oriResponse = oriResponse.split("</think>")[1].strip();
            }

            // Extract shell code segment
            Pattern pattern = Pattern.compile("```[\\n]*shell(.*?)```", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(oriResponse);
            List<String> jsonTexts = new ArrayList<>();

            while (matcher.find()) {
                jsonTexts.add(matcher.group(1).strip());
            }

            if (jsonTexts.isEmpty()) {
                LLMState state = oriResponse.startsWith("python3 send_email.py") ?
                        LLMState.ANSWER : LLMState.TOOL_CALL;
                return new LLMResponse(state, oriResponse, oriResponse);
            }

            String jsonText = jsonTexts.isEmpty() ? oriResponse : jsonTexts.get(0);
            if (jsonText.startsWith("python3 send_email.py")) {
                return new LLMResponse(LLMState.ANSWER, jsonText, oriResponse);
            } else {
                return new LLMResponse(LLMState.TOOL_CALL, jsonText, oriResponse);
            }
        } catch (Exception e) {
            log.warn("Error parsing LLM response", e);
            return new LLMResponse(LLMState.ERROR_PARSE, e.getMessage(), oriResponse);
        }
    }

    // Mock receive email function
    private static String mockReceiveEmail(String query) {
        return "python3 receive_email.py Bob\n" + query + "\nvboxuser@ubuntu:~$ ";
    }

    @Override
    public OxyResponse execute(OxyRequest oxyRequest) {
        oxyRequest.setQuery(ShellUseAgent.mockReceiveEmail(oxyRequest.getQuery()));
        oxyRequest.setArguments("hello_terminal", oxyRequest.getGlobalData("hello_terminal"));

        Memory reactMemory = new Memory();
        for (int currentRound = 0; currentRound <= getMaxReactRounds(); currentRound++) {
            // Build complete message context
            StringBuilder terminalHistory = new StringBuilder();
            // Implement memory handling
            for (Map<String, Object> memory : oxyRequest.getShortMemory()) {
                if (!memory.get("content").toString().startsWith("cmd: ")) {
                    terminalHistory.append(memory.get("content").toString());
                }
            }
            terminalHistory.append(oxyRequest.getQuery());
            for (Message message : reactMemory.getMessages()) {
                if ("role".equals(message.getRole())) {
                    terminalHistory.append(message.getContent());
                }
            }
            oxyRequest.setArguments("terminal_history", terminalHistory);

            Memory tempMemory = new Memory();
            tempMemory.addMessage(Message.systemMessage(buildInstruction(oxyRequest.getArguments())));
            tempMemory.addMessage(Message.userMessage("Please continue working on the Ubuntu terminal to complete the boss's task."));

            OxyResponse oxyResponse = oxyRequest.call(Map.of(
                    "arguments", Map.of("messages", tempMemory),
                    "callee", llmModel));
            oxyRequest.setArguments("full_memory", tempMemory);
            LLMResponse llmResponse = getFuncParseLlmResponse().apply(oxyResponse.getOutput().toString(), oxyRequest);

            if (LLMState.ANSWER.equals(llmResponse.getState())) {
                return OxyResponse.builder()
                        .state(OxyState.COMPLETED)
                        .output(llmResponse.getOutput().toString() + "\nvboxuser@ubuntu:~$")
                        .extra(new HashMap<>(Map.of("react_memory", reactMemory.toDictList())))
                        .build();
            } else if (LLMState.TOOL_CALL.equals(llmResponse.getState())) {
                OxyResponse toolResponse = oxyRequest.call(Map.of(
                        "callee", "ssh_tool",
                        "arguments", Map.of(
                                "shell_command", llmResponse.getOutput().toString(),
                                "session", session)

                ));
                reactMemory.addMessage(Message.assistantMessage("cmd: " + llmResponse.getOutput()));
                reactMemory.addMessage(Message.userMessage(toolResponse.getOutput()));
            } else {
                log.info("Format error, adding to react_memory: {}", llmResponse.getOriResponse());
                reactMemory.addMessage(Message.assistantMessage(llmResponse.getOriResponse()));
                reactMemory.addMessage(Message.userMessage(llmResponse.getOutput()));
            }
        }

        // Fallback mechanism when max rounds reached
        // Extract tool call results for final summary
        int tid = 1;
        List<String> toolCallResults = new ArrayList<>();
        for (Message message : reactMemory.getMessages()) {
            if (!"user".equals(message.getRole())) {
                continue;
            }
            toolCallResults.add(tid + ". " + message.getContentAsString());
            tid++;
        }
        String toolCallResultsStr = String.join("\n\n", toolCallResults);

        // Generate final answer based on accumulated results
        String query = oxyRequest.getQuery();
        Memory finalAnswerMemory = new Memory();
        finalAnswerMemory.addMessage(Message.systemMessage("Please answer the user's question based on the given tool execution results."));
        finalAnswerMemory.addMessage(Message.userMessage("User question: " + query + "\n---\nTool execution results: " + toolCallResultsStr));

        OxyResponse finalResponse = oxyRequest.call(Map.of(
                "callee", getLlmModel(),
                "arguments", Map.of("messages", finalAnswerMemory)));
        return OxyResponse.builder()
                .state(OxyState.COMPLETED)
                .output(finalResponse.getOutput())
                .extra(new HashMap<>(Map.of("react_memory", reactMemory.toDictList())))
                .build();
    }

    public record AuthInfo(String hostname, int port, String username, String password, String keyPath) {
    }
}

