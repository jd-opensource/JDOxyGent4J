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
package com.jd.oxygent.core.oxygent.samples.examples.flows;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.flows.PlanAndSolve;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.samples.examples.tools.DemoFunctionHubAnnotation;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Plan and Solve Flow Demo Class
 * Demonstrates how to configure and use the PlanAndSolve flow for planning and executing complex tasks.
 * Contains a complete example of multiple agents collaborating to accomplish tasks.
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class DemoPlanAndSolve {

    static DemoFunctionHubAnnotation.JokeTool fh = new DemoFunctionHubAnnotation.JokeTool();
    static String INTENTION_PROMPT = """
            You are an expert in intention understanding, skilled at understanding the intentions of conversations. The following is a daily chat scenario. Please describe the merchant's current question intention with clear and concise language based on the historical conversation. Specific requirements are as follows:
            1. Based on the historical conversation, think step by step about the current question, analyze the core semantics of the question, infer the core intention of the question, and then describe the thinking process with concise text;
            2. Based on the thinking process and conversation information, describe the intention using declarative sentences. Only output the intention, and prohibit outputting irrelevant expressions like "the current intention is";
            3. Intention understanding should be faithful to the semantics of the current question and historical conversation. Prohibit outputting content that does not exist in the historical conversation and current question, and prohibit directly answering the question.
            4. If what the user says is not a specific question or need, but casual chat or statement of relevant rules, you need to retain the information of these expressions and summarize them, but prohibit outputting irrelevant expressions like 'the user is chatting casually';
            5. When expressing intentions, retain the subject information related to the intention in the context.
            """;


    @OxySpaceBean(value = "planAndSolveNewJavaOxySpace1", defaultStart = true, query = "What time is it now? Please save it to the file log.txt under the local_file folder.")
    public static List<BaseOxy> getDefaultOxySpace() {

        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .llmParams(Map.of("temperature", 0.01))
                        .build(),
                ChatAgent.builder()
                        .name("intent_agent")
                        .prompt(INTENTION_PROMPT)
                        .llmModel("default_llm")
                        .build(),
                fh,
                new StdioMCPClient("time_tools", "uvx", Arrays.asList("mcp-server-time", "--local-timezone=Asia/Shanghai")),
                // Mac or Linux syntax
                new StdioMCPClient("file_tools", "npx", Arrays.asList("-y", "@modelcontextprotocol/server-filesystem", "./local_file")),
                // Windows syntax
//                new StdioMCPClient("file_tools", "cmd", Arrays.asList("/c", "npx", "-y", "@modelcontextprotocol/server-filesystem", "local_file")),
//                new StdioMCPClient("math_tools", "uv", Arrays.asList("--directory", "D:\\ProjectsCode\\PythonProjects\\GitHub\\OxyGent\\mcp_servers", "run", "math_tools.py")),
                ChatAgent.builder()
                        .name("planner_agent")
                        .desc("An agent capable of making plans")
                        .llmModel("default_llm")
                        .prompt("""
                                        For a given goal, create a simple and step-by-step executable plan. \
                                        The plan should be concise, with each step being an independent and complete functional module—not an atomic function—to avoid over-fragmentation. \
                                        The plan should consist of independent tasks that, if executed correctly, will lead to the correct answer. \
                                        Ensure that each step is actionable and includes all necessary information for execution. \
                                        The result of the final step should be the final answer. Make sure each step contains all the information required for its execution. \
                                        Do not add any redundant steps, and do not skip any necessary steps.
                                """)
                        .build(),
                ReActAgent.builder()
                        .name("executor_agent")
                        .desc("An agent capable of executing tools")
                        .subAgents(Arrays.asList("time_agent",
                                "time_agent_b",
                                "time_agent_c",
                                "file_agent"
                        ))
                        .tools(Arrays.asList("joke_tools"))
                        .llmModel("default_llm")
                        .timeout(100)
                        .prompt("""
                                        You are a helpful assistant who can use the following tools:
                                        ${tools_description}
                                
                                        You only need to complete the **current step** in the plan—do not do anything extra.
                                        Respond strictly according to the requirements of the current step.
                                        If a tool is required, select one from the tools listed above. Do not choose any other tool.
                                        If multiple tool calls are needed, call only **one** tool at a time. You will receive the result and continue after that.
                                        If no tool is needed, respond directly—**do not output anything else**.
                                
                                        Important Instructions:
                                        1. When you have collected enough information to answer the user's question, respond using the following format:
                                        <think>Your reasoning (if necessary)</think>
                                        Your actual response
                                
                                        2. When the user's question lacks necessary information, you may ask a clarification question. Use the format:
                                        <think>Your reasoning (if necessary)</think>
                                        Your clarification question
                                
                                        3. When you need to use a tool, you must respond with the following **exact** JSON object format, and **nothing else**:
                                        ```json
                                        {
                                            "think": "Your reasoning (if necessary)",
                                            "tool_name": "Tool name",
                                            "arguments": {
                                                "parameter_name": "parameter_value"
                                            }
                                        }
                                        ```
                                
                                        After receiving the tool's response:
                                        1. Convert the raw data into a natural conversational reply
                                        2. Be concise but informative
                                        3. Focus on the most relevant information
                                        4. Use appropriate context from the user's question
                                        5. Avoid simply repeating the raw data
                                
                                        Only use the explicitly defined tools above. If no tool is needed, reply directly—**do not output anything else**.
                                """)
                        .build(),
                PlanAndSolve.builder()
                        .name("master_agent")
                        .isDetailedObservation(true)
                        .llmModel("default_llm")
                        .isMaster(true)
                        .plannerAgentName("planner_agent")
                        .executorAgentName("executor_agent")
                        .enableReplanner(false)
                        .timeout(100)
                        .build(),
                ReActAgent.builder()
                        .name("time_agent")
                        .desc("A tool for querying the time")
                        .tools(Arrays.asList("time_tools"))
                        .llmModel("default_llm")
                        .timeout(100)
                        .build(),
                ReActAgent.builder()
                        .name("time_agent_b")
                        .desc("A tool for querying the time")
                        .tools(Arrays.asList("time_tools"))
                        .llmModel("default_llm")
                        .timeout(100)
                        .build(),
                ReActAgent.builder()
                        .name("time_agent_c")
                        .desc("A tool for querying the time")
                        .tools(Arrays.asList("time_tools"))
                        .llmModel("default_llm")
                        .timeout(100)
                        .build(),
                ReActAgent.builder()
                        .name("file_agent")
                        .desc("A tool for operating the file system")
                        .tools(Arrays.asList("file_tools"))
                        .llmModel("default_llm")
                        .build()
//                ,
//                WorkflowAgent.builder()
//                        .name("math_agent")
//                        .desc("A tool for querying the value of pi")
//                        .subAgents(Arrays.asList("time_agent"))
//                        .tools(Arrays.asList("math_tools"))
//                        .llmModel("default_llm")
//                        .isRetainMasterShortMemory(true)
//                        .funcWorkflow(x -> {
//                            //Get memory
//                            List<Map<String, Object>> shortMemory = x.getShortMemory(false);
//                            log.info("--- History --- :{}", shortMemory);
//                            List<Map<String, Object>> masterShortMemory = x.getShortMemory(true);
//                            log.info("--- User-level History  --- :{}", masterShortMemory);
//
//                            //Get query
//                            String masterQuery = x.getQuery(true);
//                            log.info("--- user query:--- :{}", masterQuery);
//
//                            //Send message
//                            //x.sendMessage(Map.of("msg", "send message"));
//
//                            //Call Agent
//                            OxyResponse callChatAgent = x.call(new HashMap<>(Map.of("callee", "time_agent", "arguments", new HashMap<>(Map.of("query", "What time is it now?")))));
//                            log.info("--- Current Time ---:{}", callChatAgent.getOutput());
//
//                            // Use regular expressions to find all numbers
//                            Pattern pattern = Pattern.compile("\\d+");
//                            Matcher matcher = pattern.matcher(callChatAgent.getOutput().toString());
//
//                            String n = null;
//                            // Find all matches, get the last number
//                            while (matcher.find()) {
//                                n = matcher.group();
//                            }
//
//                            if (n != null) {
//                                // Call calc_pi method
//                                OxyResponse callCalcPiResponse = x.call(new HashMap<>(Map.of("callee", "calc_pi", "arguments", new HashMap<>(Map.of("prec", n)))));
//                                return String.format("Save %s positions: %s", n, callCalcPiResponse.getOutput());
//                            } else {
//                                return CompletableFuture.completedFuture(
//                                        "Save 2 positions: 3.14, or you could ask me to save how many positions you want."
//                                );
//                            }
//
//                        })
//                        .llmModel("default_llm")
//                        .build()
        );
    }

    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);
    }
}
