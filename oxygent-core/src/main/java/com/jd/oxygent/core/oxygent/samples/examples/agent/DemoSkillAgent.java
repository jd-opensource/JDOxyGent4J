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
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.SkillAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.llms.MockLlm;
import com.jd.oxygent.core.oxygent.oxy.skills.SkillMetadata;
import com.jd.oxygent.core.oxygent.oxy.skills.SkillRegistry;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.MasFactoryRegistry;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.schemas.memory.Memory;
import com.jd.oxygent.core.oxygent.schemas.memory.Message;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.utils.CommonUtils;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.LinkOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Skill Agent Demo Class
 * Demonstrates basic SkillAgent usage with skill discovery and activation
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Log4j2
public class DemoSkillAgent {

    // A_Skill directories
    private static String AGENT_ASKILL_DIR;
    // B_Skill directories
    private static String AGENT_BSKILL_DIR;

    /*
     * Builds default LLM for skill activation
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
                    .build();
        }

        return MockLlm.builder()
                .name("default_llm")
                .funcMockProcess(DemoSkillAgent::offlineResponse)
                .build();
    }

    /**
     * Offline response for skill activation
     * @param oxyRequest
     * @return
     */
    private static String offlineResponse(OxyRequest oxyRequest) {
        Object messagesObj = oxyRequest.getArguments().get("messages");
        String query = "";
        String systemText = "";

        if (messagesObj instanceof Memory) {
            Memory memory = (Memory) messagesObj;
            List<Message> messages = memory.getMessages();
            if (messages != null && !messages.isEmpty()) {
                Message last = messages.get(messages.size() - 1);
                if (last != null && last.getContent() != null) {
                    query = last.getContent().toString();
                }
                Message first = messages.get(0);
                if (first != null && "system".equals(first.getRole()) && first.getContent() != null) {
                    systemText = first.getContent().toString();
                }
            }
        }

        String skillName = "";
        String marker = "[SKILL ACTIVATED: ";
        int markerIndex = systemText.indexOf(marker);
        if (markerIndex != -1) {
            int start = markerIndex + marker.length();
            int end = systemText.indexOf("]", start);
            if (end != -1) {
                skillName = systemText.substring(start, end).trim();
            }
        }

        if (!skillName.isEmpty()) {
            return "(offline demo) skill activated: " + skillName + "\n\nYou said: " + query;
        }
        return "(offline demo) SkillAgent is running. "
                + "Set DEFAULT_LLM_BASE_URL/DEFAULT_LLM_MODEL_NAME to enable selector.\n\n"
                + "You said: " + query;
    }

    @OxySpaceBean(value = "skillAgentJavaOxySpace", defaultStart = true, query = "Use prefix a: or b: in your query to indicate desired agent scope.\\n Example: a: /skill-creator init hello-skill --path .oxygent/skills")
    public static List<BaseOxy> getDefaultOxySpace() {

        String baseUrl = EnvUtils.getEnv("DEFAULT_LLM_BASE_URL");
        String modelName = EnvUtils.getEnv("DEFAULT_LLM_MODEL_NAME");
        boolean selectorEnabled = baseUrl != null && !baseUrl.isEmpty() && modelName != null && !modelName.isEmpty();

        return Arrays.asList(
                buildDefaultLlm(),
                ReActAgent.builder()
                        .name("master_agent")
                        .llmModel("default_llm")
                        .subAgents(List.of("agent_a", "agent_b"))
                        .additionalPrompt(
                                "Delegate skill-specific requests to agent_a or agent_b when appropriate."
                                + "Use agent_a for scope A and agent_b for scope B."
                        ).build(),
                SkillAgent.builder()
                        .name("agent_a")
                        .llmModel("default_llm")
                        .enableSelector(selectorEnabled)
                        .skillDirs(List.of(AGENT_ASKILL_DIR))
                        .additionalPrompt(
                                "Skills are NOT tools. Never use a skill name as tool_name. "
                                + "Only call tools that appear in tools_description. "
                                + "When a skill is activated, mention its name briefly before answering."
                                + "You are agent_a and must only use skills from your configured scope."
                        ).build(),
                SkillAgent.builder()
                        .name("agent_b")
                        .llmModel("default_llm")
                        .enableSelector(selectorEnabled)
                        .skillDirs(List.of(AGENT_BSKILL_DIR))
                        .additionalPrompt(
                                "Skills are NOT tools. Never use a skill name as tool_name. "
                                + "Only call tools that appear in tools_description."
                                + "When a skill is activated, mention its name briefly before answering. "
                                + "You are agent_b and must only use skills from your configured scope."
                        ).build()
        );
    }

    /**
     * 确保路径是绝对路径且是存在的目录
     * @param pathValue
     * @param argName
     * @return
     * @throws IOException
     */
    private static String ensureAbsDir(String pathValue, String argName) throws IOException {
        java.nio.file.Path path = java.nio.file.Paths.get(pathValue);
        if (!path.isAbsolute()) {
            throw new IllegalArgumentException(argName + " 必须是绝对路径: " + pathValue);
        }
        if (!java.nio.file.Files.isDirectory(path)) {
            throw new IllegalArgumentException(argName + " 必须是已存在的目录: " + pathValue);
        }
        return path.toRealPath().toString();
    }

    public static void main(String[] args) throws Exception {
        // 解析命令行参数
        String agentASkillDir = null;
        String agentBSkillDir = null;
        boolean webMode = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--agent-a-skill-dir":
                    if (i + 1 < args.length) {
                        agentASkillDir = args[++i];
                    } else {
                        throw new IllegalArgumentException("--agent-a-skill-dir 需要指定路径");
                    }
                    break;
                case "--agent-b-skill-dir":
                    if (i + 1 < args.length) {
                        agentBSkillDir = args[++i];
                    } else {
                        throw new IllegalArgumentException("--agent-b-skill-dir 需要指定路径");
                    }
                    break;
                case "--web":
                    webMode = true;
                    break;
                default:
                    // 忽略其他参数
            }
        }

        // 验证 agent-a-skill-dir 参数
        if (agentASkillDir == null || agentASkillDir.isEmpty()) {
            throw new IllegalArgumentException("--agent-a-skill-dir 为必填参数");
        }
        AGENT_ASKILL_DIR = ensureAbsDir(agentASkillDir, "--agent-a-skill-dir");

        // 验证 agent-b-skill-dir 参数
        if (agentBSkillDir == null || agentBSkillDir.isEmpty()) {
            throw new IllegalArgumentException("--agent-b-skill-dir 为必填参数");
        }
        AGENT_BSKILL_DIR = ensureAbsDir(agentBSkillDir, "--agent-b-skill-dir");

        // 启动框架 获取 mas 实例
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());

        Mas mas = MasFactoryRegistry.getFactory().createMas();

        //输出 agent_a ,agent_b 技能目录
        System.out.println("\nagent_a skill dir: " + AGENT_ASKILL_DIR);
        System.out.println("agent_b skill dir: " + AGENT_BSKILL_DIR);

        // 输出 agent_a ,agent_b 各自的技能列表
        for (String callee : Arrays.asList("agent_a", "agent_b")) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("callee", callee);
            payload.put("query", "list skills");
            
            OxyResponse resp = mas.chatWithAgent(payload);
            
            System.out.println("\n[" + callee + "] discovered scoped skills:");
            System.out.println(resp.getOutput());
        }

        //启动网页版本
        if (webMode){
            ServerApp.main(args);
        } else {
            // 启动中断版本
            System.out.println(
                    "\nEnter queries below.\n"+
                    "Routing prefixes:\n"+
                    "- a: <query>  (send to agent_a)\n"+
                    "- b: <query>  (send to agent_b)\n"+
                    "- m: <query>  (send to master_agent)\n"+
                    "No prefix -> master_agent.\n"+
                    "\nExamples:\n"+
                    "- a: list skills\n"+
                    "- b: list skills\n"+
                    "- a: /skill-creator init hello-skill --path .oxygent/skills\n"+
                    "Type 'exit' to quit.\n"
            );

            Map<String, String> traceByCallee = new HashMap<>();
            traceByCallee.put("master_agent", "");
            traceByCallee.put("agent_a", "");
            traceByCallee.put("agent_b", "");

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.print("You: ");
                String query = reader.readLine();
                if (query == null) {
                    break;
                }
                query = query.trim();
                if ("exit".equalsIgnoreCase(query) || "quit".equalsIgnoreCase(query) || "bye".equalsIgnoreCase(query)) {
                    break;
                }
                if (query.isEmpty()) {
                    continue;
                }

                String callee = "master_agent";
                String actualQuery = query;
                if (query.startsWith("a:")) {
                    callee = "agent_a";
                    actualQuery = query.substring(2).trim();
                } else if (query.startsWith("b:")) {
                    callee = "agent_b";
                    actualQuery = query.substring(2).trim();
                } else if (query.startsWith("m:")) {
                    callee = "master_agent";
                    actualQuery = query.substring(2).trim();
                }

                Map<String, Object> payload = new HashMap<>();
                payload.put("query", actualQuery);
                payload.put("callee", callee);
                payload.put("from_trace_id", traceByCallee.getOrDefault(callee, ""));

                OxyResponse resp = mas.chatWithAgent(payload);

                traceByCallee.put(callee, resp.getOxyRequest().getCurrentTraceId());

                if (resp.getExtra().containsKey("skill_selection")) {
                    String sel = (String) resp.getExtra().get("skill_selection");
                    System.out.println("[skill selection] " + sel);
                }
                if (resp.getExtra().containsKey("skill_activation")) {
                    String act = (String) resp.getExtra().get("skill_activation");
                    System.out.println("[skill activated] " + act);
                }

                System.out.println("LLM(" + callee + "): " + resp.getOutput());
            }
        }
    }
}