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
package com.jd.oxygent.core.oxygent.samples.examples.backend;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.MasFactoryRegistry;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Demo class for data scope functionality
 *
 * <p>This class ports functionality from the Python version, demonstrating how to implement in Java:</p>
 * <ul>
 *   <li><strong>Data Scope Management</strong>: Usage of shared_data, group_data, global_data</li>
 *   <li><strong>Input Processing Function</strong>: Process request data through func_process_input</li>
 *   <li><strong>MCP Tool Integration</strong>: Integrate external tools using StdioMCPClient</li>
 *   <li><strong>Multi-Agent Collaboration</strong>: Collaboration mode between master and sub agents</li>
 *   <li><strong>Trace ID Management</strong>: Support session continuation with from_trace_id</li>
 * </ul>
 *
 * <h3>Python Version Correspondence</h3>
 * <pre>{@code
 * # Python version
 * def process_input(oxy_request: OxyRequest) -> OxyRequest:
 *     print("--- agent name --- :", oxy_request.callee)
 *     print("--- arguments --- :", oxy_request.get_arguments())
 *     print("--- shared_data --- :", oxy_request.get_shared_data())
 *     print("--- group_data --- :", oxy_request.get_group_data())
 *     print("--- global_data --- :", oxy_request.get_global_data())
 *     return oxy_request
 *
 * oxy_space = [
 *     oxy.HttpLLM(...),
 *     oxy.StdioMCPClient(
 *         name="time_tools",
 *         params={
 *             "command": "uvx",
 *             "args": ["mcp-server-time", "--local-timezone=Asia/Shanghai"],
 *         },
 *     ),
 *     oxy.ReActAgent(
 *         name="master_agent",
 *         is_master=True,
 *         llm_model="default_llm",
 *         sub_agents=["time_agent"],
 *         func_process_input=process_input,
 *     ),
 *     oxy.ReActAgent(
 *         name="time_agent",
 *         desc="A tool for time query.",
 *         tools=["time_tools"],
 *         func_process_input=process_input,
 *     ),
 * ]
 * }</pre>
 *
 * <h3>Java Version Implementation</h3>
 * <ul>
 *   <li><strong>Function&lt;OxyRequest, OxyRequest&gt;</strong>: Use Function interface to implement input processing</li>
 *   <li><strong>StdioMCPClient</strong>: Create MCP client directly using constructor</li>
 *   <li><strong>subAgents()</strong>: Set sub-agent list</li>
 *   <li><strong>funcProcessInput()</strong>: Set input processing function</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class DemoDataScope {

    /**
     * Input processing function
     *
     * <p>Corresponds to Python version's process_input function:</p>
     * <pre>{@code
     * def process_input(oxy_request: OxyRequest) -> OxyRequest:
     *     print("--- agent name --- :", oxy_request.callee)
     *     print("--- arguments --- :", oxy_request.get_arguments())
     *     print("--- shared_data --- :", oxy_request.get_shared_data())
     *     print("--- group_data --- :", oxy_request.get_group_data())
     *     print("--- global_data --- :", oxy_request.get_global_data())
     *     return oxy_request
     * }</pre>
     *
     * <p>This function is used to print the contents of various data scopes before agent processes requests,
     * facilitating debugging and understanding data flow.</p>
     */
    private static final Function<OxyRequest, OxyRequest> PROCESS_INPUT_FUNCTION = oxyRequest -> {
        try {
            log.info("--- agent name --- : {}", oxyRequest.getCallee());
            log.info("--- arguments --- : {}", oxyRequest.getArguments());
            log.info("--- shared_data --- : {}", oxyRequest.getSharedData());
            log.info("--- group_data --- : {}", oxyRequest.getGroupData());
            log.info("--- global_data --- : {}", oxyRequest.getGlobalData());

            return oxyRequest;

        } catch (Exception e) {
            log.error("Input processing function execution failed: {}", e.getMessage(), e);
            return oxyRequest; // Return original request even if error occurs to ensure process continues
        }
    };

    /**
     * Get default OxySpace configuration
     *
     * <p>Corresponds to Python version's oxy_space configuration:</p>
     * <pre>{@code
     * oxy_space = [
     *     oxy.HttpLLM(
     *         name="default_llm",
     *         api_key=os.getenv("OXY_LLM_API_KEY"),
     *         base_url=os.getenv("OXY_LLM_BASE_URL"),
     *         model_name=os.getenv("OXY_LLM_MODEL_NAME"),
     *     ),
     *     oxy.StdioMCPClient(
     *         name="time_tools",
     *         params={
     *             "command": "uvx",
     *             "args": ["mcp-server-time", "--local-timezone=Asia/Shanghai"],
     *         },
     *     ),
     *     oxy.ReActAgent(
     *         name="master_agent",
     *         is_master=True,
     *         llm_model="default_llm",
     *         sub_agents=["time_agent"],
     *         func_process_input=process_input,
     *     ),
     *     oxy.ReActAgent(
     *         name="time_agent",
     *         desc="A tool for time query.",
     *         tools=["time_tools"],
     *         func_process_input=process_input,
     *     ),
     * ]
     * }</pre>
     *
     * @return List of BaseOxy containing LLM, MCP client and ReActAgent
     */
    @OxySpaceBean(value = "dataScopeJavaOxySpace", defaultStart = true, query = "What time is it")
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
                // 1. HTTP LLM Configuration - corresponds to Python version's oxy.HttpLLM
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .timeout(30)
                        .build(),

                // 2. MCP Client - corresponds to Python version's oxy.StdioMCPClient
                new StdioMCPClient(
                        "time_tools",
                        "uvx",
                        List.of("mcp-server-time", "--local-timezone=Asia/Shanghai")
                ),

                // 3. Master Agent - corresponds to Python version's master_agent
                ReActAgent.builder()
                        .name("master_agent")
                        .isMaster(true)
                        // corresponds to Python version's is_master=True
                        .llmModel("default_llm")
                        .subAgents(List.of("time_agent"))
                        // corresponds to Python version's sub_agents=["time_agent"]
                        .funcProcessInput(PROCESS_INPUT_FUNCTION)
                        // corresponds to Python version's func_process_input=process_input
                        .build(),

                // 4. Time Agent - corresponds to Python version's time_agent
                ReActAgent.builder()
                        .name("time_agent")
                        .desc("A tool for time query.")
                        // corresponds to Python version's desc
                        .tools(List.of("time_tools"))
                        // corresponds to Python version's tools=["time_tools"]
                        .funcProcessInput(PROCESS_INPUT_FUNCTION)
                        // corresponds to Python version's func_process_input=process_input
                        .build()
        );
    }

    /**
     * Demonstrate data scope functionality
     *
     * <p>Corresponds to test logic in Python version's main function:</p>
     *
     * @throws Exception When MAS operations fail
     */
    public static void demonstrateDataScope() throws Exception {
        log.info("Starting demonstration of data scope functionality");

        try {
            // Create MAS instance, Java version doesn't support passing globalData directly in createMas
            // Need to set global data through other methods after creation
            Mas mas = MasFactoryRegistry.getFactory().createMas();

            // Set global data (if Mas class supports it)
            // Note: This may need adjustment based on actual Mas class API
            Map<String, Object> globalData = Map.of("global_key", "global_value");
            mas.setGlobalData(globalData);
            // If this method exists

            // round 1-1: Basic query
            log.info("=== Round 1-1: Basic query ===");
            Map<String, Object> payload1 = new HashMap<>(2);
            payload1.put("query", "What time is it");
            OxyResponse response1 = mas.chatWithAgent(payload1);
            log.info("Round 1-1 response: {}", response1.getOutput());

            // round 2-1: Query with shared_data and group_data
            log.info("=== Round 2-1: Query with shared_data and group_data ===");
            Map<String, Object> payload2 = new HashMap<>(2);
            payload2.put("query", "What time is it");

            Map<String, Object> shareData = new HashMap<>(2);
            shareData.put("shared_key", "shared_value");
            Map<String, Object> groupData = new HashMap<>(2);
            groupData.put("group_key", "group_value");

            payload2.put("shared_data", shareData);
            payload2.put("group_data", groupData);

            OxyResponse response2 = mas.chatWithAgent(payload2);
            log.info("Round 2-1 response: {}", response2.getOutput());

            // round 2-2: Continue session using trace_id
            if (response2.getOxyRequest() != null && response2.getOxyRequest().getCurrentTraceId() != null) {
                log.info("=== Round 2-2: Continue session using trace_id ===");
                String traceId = response2.getOxyRequest().getCurrentTraceId();
                log.info("Using Trace ID: {}", traceId);

                Map<String, Object> payload3 = new HashMap<>(2);
                payload3.put("query", "What time is it");
                payload3.put("from_trace_id", traceId);

                OxyResponse response3 = mas.chatWithAgent(payload3);
                log.info("Round 2-2 response: {}", response3.getOutput());
            } else {
                log.warn("Unable to get trace_id, skipping Round 2-2");
            }

            log.info("Data scope demonstration completed");

        } catch (Exception e) {
            log.error("Data scope demonstration failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Application main entry point
     *
     * <p>Corresponds to Python version's main function:</p>
     * <pre>{@code
     * async def main():
     *     global_data = {"global_key": "global_value"}
     *     async with MAS(oxy_space=oxy_space, global_data=global_data) as mas:
     *         # Execute multiple rounds of testing...
     *
     * if __name__ == "__main__":
     *     asyncio.run(main())
     * }</pre>
     *
     * <p>Java version provides two running modes:</p>
     * <ol>
     *   <li><strong>Spring Boot Web Service</strong>: Start through OpenOxySpringBootApplication</li>
     *   <li><strong>Data Scope Demo</strong>: Demonstrate through demonstrateDataScope() method</li>
     * </ol>
     *
     * @param args Command line arguments
     * @throws Exception When application startup fails
     */
    public static void main(String[] args) throws Exception {
        // Get current class name and register OxySpace
        var currentClassName = Thread.currentThread().getStackTrace()[1].getClassName();
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(currentClassName);

        log.info("DemoDataScope starting...");
        log.info("Feature descriptions:");
        log.info("1. Demonstrate data scopes of shared_data, group_data, global_data");
        log.info("2. Show usage of input processing functions");
        log.info("3. Demonstrate MCP tool integration");
        log.info("4. Show multi-agent collaboration mode");
        log.info("5. Demonstrate session continuation functionality with trace_id");

        // Choose running mode:
        // Mode 1: Start Spring Boot Web Service (default)
//        OpenOxySpringBootApplication.main(args);

        // Mode 2: Demonstrate data scope functionality (uncomment the code below)
        demonstrateDataScope();

        log.info("Application startup completed, you can test data scope functionality through the web interface");
    }
}