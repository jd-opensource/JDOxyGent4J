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
        //获取请求消息
        Object messagesObj = oxyRequest.getArguments().get("messages");
        //系统提示词
        String systemText = "";
        //用户提示词
        String query = "";
        //提取提示词
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
        //系统提示词中获取激活技能
        Matcher m = SKILL_ACTIVATED_PATTERN.matcher(systemText);
        String activeSkill = m.find() ? m.group(1).trim() : "";

        //收集历史所有历史记录
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

        // 检查是否存在待处理的技能初始化请求
        boolean pending = historyText.toString().contains("[PENDING_SKILL_INIT:");
        if (pending) {
            // 提取待处理技能的配置信息
            Matcher m2 = PENDING_SKILL_PATTERN.matcher(historyText.toString());
            String payload = m2.find() ? m2.group(1).trim() : "";
            
            // 将用户查询转换为小写以便匹配
            String lowerQ = query.toLowerCase();
            
            // 处理用户确认创建技能的请求
            if (lowerQ.matches("[y]|yes|确认|是|好|ok")) {
                // 解析待处理技能的配置数据
                Map<String, Object> data = JsonUtils.parseObject(payload, Map.class);

                // 构建技能创建脚本的参数
                Map<String, Object> args = new HashMap<>();
                args.put("skill_name", "skill-creator");
                args.put("script_relpath", "init_skill.java");
                args.put("args", Arrays.asList(data.get("skill_name"), "--path", data.get("path")));

                // 构建调用技能脚本工具的结果
                Map<String, Object> result = new HashMap<>();
                result.put("tool_name", "run_skill_script");
                result.put("arguments", args);

                // 返回JSON格式的工具调用结果
                return JsonUtils.toJSONString(result);
            }
            
            // 处理用户取消创建技能的请求
            if (lowerQ.matches("[n]|no|取消|否|不")) {
                return "Cancelled.";
            }
            
            // 提示用户进行确认操作
            return "Please confirm: reply 'yes' to create, or 'no' to cancel.";
        }

        // 激活技能为skil-creator指令输入抽取
        if ("skill-creator".equals(activeSkill)) {
            //案例 ： query = "init hello-skill --path .claude/skills"
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

        // 启动框架 获取 mas 实例
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        Mas mas = MasFactoryRegistry.getFactory().createMas();

        if (useWeb) {
            ServerApp.main(args);
        } else {
            System.out.println(
                    "\nExamples:\n"
                    + "- /skill-creator init hello-skill --path .claude/skills\n"
                    + "- 帮我创建一个新的 skill，名字叫 hello-skill\n"
                    + "- /agent-browser 获取墨迹天气的今日温度\n"
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
