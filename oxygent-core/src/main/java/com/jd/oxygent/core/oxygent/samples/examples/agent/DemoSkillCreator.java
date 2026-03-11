/*
 * Copyright 2025 JD.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this project except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.oxygent.core.oxygent.samples.examples.agent;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.SkillAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.llms.MockLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.MasFactoryRegistry;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.schemas.memory.Memory;
import com.jd.oxygent.core.oxygent.schemas.memory.Message;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.tools.PresetTools;
import com.jd.oxygent.core.oxygent.utils.CommonUtils;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Skill Creator Demo Class
 * Demonstrates how to configure and use SkillAgent with skill-creator functionality
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Log4j2
public class DemoSkillCreator {

    private static final Pattern SKILL_ACTIVATED_PATTERN = Pattern.compile(
            "\\[SKILL ACTIVATED:\\s*([^\\]]+)\\]"
    );
    private static final Pattern PENDING_SKILL_PATTERN = Pattern.compile(
            "\\[PENDING_SKILL_INIT:\\s*([^\\]]+)\\]"
    );

    /**
     * Builds default LLM for skill creator
     */
    private static BaseOxy buildDefaultLlm() {
        String apiKey = EnvUtils.getEnv("DEFAULT_LLM_API_KEY");
        String baseUrl = EnvUtils.getEnv("DEFAULT_LLM_BASE_URL");
        String modelName = EnvUtils.getEnv("DEFAULT_LLM_MODEL_NAME");

        if (baseUrl != null && !baseUrl.isEmpty() && modelName != null && !modelName.isEmpty()) {
            return HttpLlm.builder()
                    .name("default_llm")
                    .apiKey(apiKey)
                    .baseUrl(baseUrl)
                    .modelName(modelName)
                    .llmParams(Map.of("stream",false))
                    .build();
        }

        return MockLlm.builder()
                .name("default_llm")
                .funcMockProcess(DemoSkillCreator::offlineProcess)
                .build();
    }

    /**
     * Offline process for skill creator demo
     * @param oxyRequest
     * @return
     */
    private static String offlineProcess(OxyRequest oxyRequest) {
        // Get request messages
        Object messagesObj = oxyRequest.getArguments().get("messages");
        // System prompt
        String systemText = "";
        // User prompt
        String query = "";
        // Extract prompts
        if (messagesObj instanceof Memory) {
            Memory memory = (Memory) messagesObj;
            List<Message> messages = memory.getMessages();
            if (messages != null && !messages.isEmpty()) {
                Message first = messages.get(0);
                if (first != null && "system".equals(first.getRole()) && first.getContent() != null) {
                    systemText = first.getContent().toString();
                }
                Message last = messages.get(messages.size() - 1);
                if (last != null && last.getContent() != null) {
                    query = last.getContent().toString().trim();
                }
            }
        }
        // Get active skill from system prompt
        Matcher m = SKILL_ACTIVATED_PATTERN.matcher(systemText);
        String activeSkill = m.find() ? m.group(1).trim() : "";

        // Collect all historical records
        StringBuilder historyText = new StringBuilder();
        if (messagesObj instanceof Memory) {
            Memory memory = (Memory) messagesObj;
            List<Message> messages = memory.getMessages();
            if (messages != null) {
                for (Message msg : messages) {
                    if (msg != null && msg.getContent() != null) {
                        historyText.append(msg.getContent());
                    }
                }
            }
        }

        // Check if there is a pending skill initialization request
        boolean pending = historyText.toString().contains("[PENDING_SKILL_INIT:");
        if (pending) {
            // Extract pending skill configuration information
            Matcher m2 = PENDING_SKILL_PATTERN.matcher(historyText.toString());
            String payload = m2.find() ? m2.group(1).trim() : "";
            
            // Convert user query to lowercase for matching
            String lowerQ = query.toLowerCase();
            
            // Handle user confirmation to create skill
            if (lowerQ.matches("[y]|yes|confirm|是|好|ok")) {
                // Parse pending skill configuration data
                Map<String, Object> data = JsonUtils.parseObject(payload, Map.class);

                // Build parameters for skill creation script
                Map<String, Object> args = new HashMap<>();
                args.put("skill_name", "skill-creator");
                args.put("script_relpath", "init_skill.java");
                args.put("args", Arrays.asList(data.get("skill_name"), "--path", data.get("path")));

                // Build result for skill script tool call
                Map<String, Object> result = new HashMap<>();
                result.put("tool_name", "run_skill_script");
                result.put("arguments", args);

                // Return JSON-formatted tool call result
                return JsonUtils.toJSONString(result);
            }
            
            // Handle user cancellation of skill creation
            if (lowerQ.matches("[n]|no|cancel|否|不")) {
                return "Cancelled.";
            }
            
            // Prompt user for confirmation
            return "Please confirm: reply 'yes' to create, or 'no' to cancel.";
        }

        // Activate skill for skill-creator command input extraction
        if ("skill-creator".equals(activeSkill)) {
            // Example: query = "init hello-skill --path .claude/skills"
            String[] parts = query.split("\\s+");
            if (parts.length >= 2 && "init".equals(parts[0])) {
                String skillName = parts[1];
                String outPath = ".claude/skills";
                for (int i = 2; i < parts.length - 1; i++) {
                    if ("--path".equals(parts[i])) {
                        outPath = parts[i + 1];
                        break;
                    }
                }
                Map<String, Object> data = new HashMap<>();
                data.put("skill_name", skillName);
                data.put("path", outPath);
                return "Reply 'yes' to confirm skill creation, or 'no' to cancel.\n\n" + "[PENDING_SKILL_INIT: " + JsonUtils.toJSONString(data) + "]";
            }
        }

        return "(offline demo) Try:\n"
                + "- /skill-creator init hello-skill --path .claude/skills\n"
                + "- /agent-browser 获取墨迹天气的今日温度";
    }

    @OxySpaceBean(value = "skillCreatorJavaOxySpace", defaultStart = true, query = "/skill-creator init hello-skill --path .claude/skills")
    public static List<BaseOxy> getDefaultOxySpace() {
        boolean hasRealLlm = EnvUtils.getEnv("DEFAULT_LLM_BASE_URL") != null  && EnvUtils.getEnv("DEFAULT_LLM_MODEL_NAME") != null;

        return Arrays.asList(
                buildDefaultLlm(),
                PresetTools.FILE_TOOLS,
                PresetTools.SHELL_TOOLS,
                PresetTools.SKILL_TOOLS,
                SkillAgent.builder()
                        .name("master_agent")
                        .llmModel("default_llm")
                        .enableSelector(hasRealLlm)
                        .tools(Arrays.asList("file_tools", "shell_tools", "skill_tools"))
                        .additionalPrompt(
                                "If the active skill is skill-creator: ask only missing questions; "
                                + "ALWAYS require an explicit yes/no confirmation before calling run_skill_script. "
                                + "Default output path is .claude/skills (project-local).\n"
                                + "If the active skill describes CLI commands (e.g. agent-browser ...), execute them via "
                                + "run_shell_command(command=\"<full cli command>\")."
                        ).build()
        );
    }

    public static void main(String[] args) throws Exception {
        boolean useWeb = false;
        for (String arg : args) {
            if ("--web".equals(arg)) {
                useWeb = true;
                break;
            }
        }

        // Start framework and get mas instance
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        Mas mas = MasFactoryRegistry.getFactory().createMas();

        if (useWeb) {
            ServerApp.main(args);
        } else {
            System.out.println(
                    "\nExamples:\n"
                    + "- /skill-creator init hello-skill --path .claude/skills\n"
                    + "- Help me create a new skill named hello-skill\n"
                    + "- /agent-browser Get today's temperature from Moji Weather\n"
                    + "Type 'exit' to quit.\n"
            );

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            String fromTraceId = "";

            while (true) {
                System.out.print("You: ");
                String query = reader.readLine();
                if (query == null) {
                    break;
                }
                query = query.trim();
                if (query.isEmpty()) {
                    continue;
                }
                if (query.matches("(?i)exit|quit|bye")) {
                    break;
                }

                Map<String, Object> arguments = new HashMap<>();
                arguments.put("query", query);
                arguments.put("from_trace_id", fromTraceId);
                arguments.put("request_id", CommonUtils.generateShortUUID());

                OxyResponse resp = mas.chatWithAgent(arguments);
                fromTraceId = resp.getOxyRequest() != null ? resp.getOxyRequest().getCurrentTraceId() : "";
                System.out.println("LLM: " + resp.getOutput());
            }
        }
    }
}
