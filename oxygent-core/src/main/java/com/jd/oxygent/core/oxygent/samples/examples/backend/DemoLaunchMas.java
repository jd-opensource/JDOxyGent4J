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
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import lombok.extern.log4j.Log4j2;

import java.util.*;

/**
 * Demo class for launching MAS (Multi-Agent System)
 *
 * <p>This class demonstrates how to manually create and launch a MAS instance,
 * showing various ways to interact with agents and tools:</p>
 * <ul>
 *   <li><strong>Manual MAS Creation</strong>: Create MAS instance without Spring Boot framework</li>
 *   <li><strong>Direct Agent Calls</strong>: Call agents and tools directly using mas.call()</li>
 *   <li><strong>Chat Interface</strong>: Use chatWithAgent() for conversational interaction</li>
 *   <li><strong>CLI Mode</strong>: Launch command-line interface for interactive testing</li>
 *   <li><strong>Batch Processing</strong>: Process multiple queries in batch</li>
 *   <li><strong>Web Service</strong>: Start web service through ServerApp integration</li>
 * </ul>
 *
 * <h3>Features</h3>
 * <ul>
 *   <li><strong>Time Tools Integration</strong>: MCP client for time-related operations</li>
 *   <li><strong>ReAct Agent</strong>: Reasoning and Acting agent with tool access</li>
 *   <li><strong>Multiple Interaction Modes</strong>: CLI, batch processing, and web service</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Log4j2
public class DemoLaunchMas {

    /**
     * Get default OxySpace configuration for MAS launch demo
     *
     * @return List of BaseOxy containing HTTP LLM, MCP client and ReAct agent
     */
    @OxySpaceBean(value = "launchMasJavaOxySpace", defaultStart = true, query = "What time it is?")
    public static List<BaseOxy> getDefaultOxySpace() {

        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .build(),
                new StdioMCPClient("time_tools", "uvx", Arrays.asList("mcp-server-time", "--local-timezone=Asia/Shanghai")),
                ReActAgent.builder()
                        .name("time_agent")
                        .tools(Arrays.asList("time_tools"))
                        .build()
        );
    }

    /**
     * Application main entry point - demonstrates manual MAS creation and various interaction modes
     *
     * @param args Command line arguments
     * @throws Exception When MAS operations fail
     */
    public static void main(String[] args) throws Exception {

        Mas mas = new Mas("app", getDefaultOxySpace());
        mas.init();

        Map<String, Object> callRequest = new HashMap<>();
        //callRequest.put("callee", "time_tools");
        //callRequest.put("arguments", Map.of("query","What time it is?"));
        Map<String, Object> arguments = new HashMap<>(Map.of("query", "What time it is?"));
        mas.call("time_agent", arguments);

        arguments = new HashMap<>(Map.of("timezone", "Asia/Shanghai"));
//        arguments = new HashMap<>(Map.of("arguments", Map.of("timezone", "Asia/Shanghai")));
        mas.call("get_current_time", arguments);

        arguments = new HashMap<>(Map.of("arguments",
                Map.of(
                        "messages", Arrays.asList(
                                Map.of("role", "system", "content", "You are a helpful assistant."),
                                Map.of("role", "user", "content", "hello")
                        ),
                        "llm_params", Map.of("temperature", 0.2)
                )));
        mas.call("default_llm", arguments);

        // Call Master Agent
        Map<String, Object> payload = new HashMap<>(Map.of("query", "What time it is?"));
        OxyResponse oxyResponse = mas.chatWithAgent(payload);
        log.info("--- Master Agent oxyResponse --- :{}", oxyResponse);

        // Start CLI mode
        mas.startCliMode("What time it is?");

        List<String> queries = Collections.nCopies(10, "What time it is?");
        List<Object> results = mas.startBatchProcessing(queries, false);

        // Java doesn't implement start_web_service
        //mas.start_web_service("What time it is?");
        // Use the following approach instead:
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);

    }

}
