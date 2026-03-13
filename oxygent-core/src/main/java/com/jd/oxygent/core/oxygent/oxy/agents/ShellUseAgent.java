package com.jd.oxygent.core.oxygent.oxy.agents;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyState;
import com.jd.oxygent.core.oxygent.tools.SshTools;
import com.jd.oxygent.core.oxygent.utils.SmartCharsetReader;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@SuperBuilder
public class ShellUseAgent extends ReActAgent {

    private AuthInfo authInfo;

    @Override
    public void init() {
        super.init();

        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(authInfo.username, authInfo.hostname, authInfo.port);
            session.setPassword(authInfo.password);
            session.setConfig("kex", "curve25519-sha256,curve25519-sha256@libssh.org,diffie-hellman-group-exchange-sha256"); // Prioritize modern algorithms in JSch
            session.setConfig("StrictHostKeyChecking", "no"); // to mute error com.jcraft.jsch.JSchUnknownHostKeyException: UnknownHostKey:
            session.connect(15000);

            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("ls -la");

            ((ChannelExec) channel).setErrStream(System.err);
            channel.connect();
            StringBuilder output = new StringBuilder();
            try (SmartCharsetReader reader = new SmartCharsetReader(channel.getInputStream())) {
                BufferedReader br = new BufferedReader(reader);

                String line;
                while ((line = br.readLine()) != null) {
                    output.append(line);
                }
            }
            channel.disconnect();
            session.disconnect();
            if (output.toString().length() > 0) {
                log.info("SSH channel initialized");
            } else {
                log.error("SSH channel initialization failed");
            }
        } catch (Exception e) {
            log.error("Error initializing SSH client", e);
        }
    }

    public LLMResponse parseLLMResponse(String oriResponse, OxyRequest oxyRequest) {
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
         oxyRequest.getArguments().put("hello_terminal", oxyRequest.getGlobalData("hello_terminal"));

        Memory reactMemory = new Memory();
        for (int currentRound = 0; currentRound <= getMaxReactRounds(); currentRound++) {
            // Build complete message context
            String terminalHistory = "";
            // FIXME: Implement memory handling
            // terminalHistory += "".join(
            //     [message["content"] for message in oxyRequest.getShortMemory() 
            //      if not message["content"].startsWith("cmd: ")]
            // );
            // terminalHistory += oxyRequest.getQuery();
            // terminalHistory += "".join(
            //     [message.content for message in reactMemory.messages 
            //      if message.role == "user"]
            // );
            // oxyRequest.setArguments("terminal_history", terminalHistory);

            Memory tempMemory = new Memory();
            // FIXME: Implement message handling
            // tempMemory.addMessage(Message.systemMessage(buildInstruction(oxyRequest.arguments)));
            // tempMemory.addMessage(Message.userMessage("Please continue working on the Ubuntu terminal to complete the boss's task."));

            // FIXME: Implement LLM call
            // List<Map<String, Object>> fullMemory = tempMemory.toDictList();
            // Object oxyResponse = oxyRequest.call(
            //     callee = getLlmModel(),
            //     arguments = Map.of("messages", fullMemory)
            // );
            // oxyRequest.arguments.put("full_memory", fullMemory);
            // LLMResponse llmResponse = parseLLMResponse(oxyResponse.output, oxyRequest);

            // Execute based on LLM decision
            // if (llmResponse.state == LLMState.ANSWER) {
            //     return new OxyResponse(
            //         state = OxyState.COMPLETED,
            //         output = llmResponse.output + "\nvboxuser@ubuntu:~$",
            //         extra = Map.of("react_memory", reactMemory.toDictList())
            //     );
            // } else if (llmResponse.state == LLMState.TOOL_CALL) {
            //     Object toolResponse = oxyRequest.call(
            //         callee = "ssh_tool",
            //         arguments = Map.of("shell_command", llmResponse.output)
            //     );
            //     reactMemory.addMessage(Message.assistantMessage("cmd: " + llmResponse.output));
            //     reactMemory.addMessage(Message.userMessage(toolResponse.output));
            // } else {
            //     log.info("Format error, adding to react_memory: {}", llmResponse.oriResponse);
            //     reactMemory.addMessage(Message.assistantMessage(llmResponse.oriResponse));
            //     reactMemory.addMessage(Message.userMessage(llmResponse.output));
            // }
        }

        // Fallback mechanism when max rounds reached
        int tid = 1;
        List<String> toolCallResults = new ArrayList<>();
        // FIXME: Implement memory processing
        // for (Map<String, Object> message : reactMemory.toDictList()) {
        //     if (!"user".equals(message.get("role"))) {
        //         continue;
        //     }
        //     toolCallResults.add(tid + ". " + message.get("content"));
        //     tid++;
        // }
        String toolCallResultsStr = String.join("\n\n", toolCallResults);

        // Generate final answer based on accumulated results
        // String query = oxyRequest.getQuery();
        // List<Message> tempMessages = new ArrayList<>();
        // tempMessages.add(Message.systemMessage("Please answer the user's question based on the given tool execution results."));
        // tempMessages.add(Message.userMessage("User question: " + query + "\n---\nTool execution results: " + toolCallResultsStr));
        // Object finalResponse = oxyRequest.call(
        //     callee = getLlmModel(),
        //     arguments = Map.of("messages", tempMessages.stream().map(Message::toDict).collect(Collectors.toList()))
        // );
        return OxyResponse.builder()
                .state(OxyState.COMPLETED).output("Task completed")
                .extra(new HashMap<>(Map.of("react_memory", new ArrayList<>()))).build();
    }

    // Inner classes for LLM response handling
    public static class LLMResponse {
        private LLMState state;
        private String output;
        private String oriResponse;

        public LLMResponse(LLMState state, String output, String oriResponse) {
            this.state = state;
            this.output = output;
            this.oriResponse = oriResponse;
        }

        public LLMState getState() { return state; }
        public String getOutput() { return output; }
        public String getOriResponse() { return oriResponse; }
    }

    public enum LLMState {
        ANSWER,
        TOOL_CALL,
        ERROR_PARSE
    }

    public static class Memory {
        private List<Message> messages = new ArrayList<>();

        public void addMessage(Message message) {
            messages.add(message);
        }

        public List<Message> getMessages() { return messages; }
    }

    public static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public static Message systemMessage(String content) {
            return new Message("system", content);
        }

        public static Message userMessage(String content) {
            return new Message("user", content);
        }

        public static Message assistantMessage(String content) {
            return new Message("assistant", content);
        }

        public String getRole() { return role; }
        public String getContent() { return content; }
    }

    public record AuthInfo(String hostname, int port, String username, String password){}
}

