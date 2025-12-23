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
package com.jd.oxygent.core.oxygent.samples.examples.tools;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.SSEMCPClient;
import com.jd.oxygent.core.oxygent.oxy.mcp.StreamableMCPClient;
import com.jd.oxygent.core.oxygent.utils.CommonUtils;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.*;

/**
 * MCP (Model Context Protocol) Tool Demo Class
 * Demonstrates how to configure and use MCP client for time query and other functions - Header authentication parameter passing
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DemoMCPToolAuthorization {

    /**
     * Get default OxySpace configuration, including MCP tools and related agents
     *
     * @return BaseOxy list containing MCP tools and agents
     * @throws IllegalArgumentException when configuration parameters are invalid
     */
    @OxySpaceBean(value = "MCPToolAuthorizationJavaOxySpace", defaultStart = true, query = "What time is it")
    public static List<BaseOxy> getDefaultOxySpace() {
        // Apply JDK17 var keyword and parameter validation
        var apiKey = EnvUtils.getEnv("OXY_LLM_API_KEY");
        var baseUrl = EnvUtils.getEnv("OXY_LLM_BASE_URL");
        var modelName = EnvUtils.getEnv("OXY_LLM_MODEL_NAME");

        var timeStreamableMcpTools = new StreamableMCPClient("time", "http://127.0.0.1:8100", "/mcp", Map.of(
                "is_dynamic_headers", true,
                "is_inherit_headers", true,
                "headers", Map.of(
                        "ukInfo", "jtest",
                        "Authorization", "xm authorization xxxxxx "
                )
        ));

        var timeSseMcpTools = new SSEMCPClient("time", "http://127.0.0.1:8100", "/sse", Map.of(
                "is_dynamic_headers", true,
                "is_inherit_headers", true,
                "headers", Map.of(
                        "ukInfo", "jtest",
                        "Authorization", "xm authorization xxxxxx "
                )
        ));

        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .build(),
                timeStreamableMcpTools,
                ReActAgent.builder()
                        .name("time_agent")
                        .desc("Tool agent capable of querying time")
                        .additionalPrompt("Do not send any information other than time information.")
                        .tools(Arrays.asList("time")) // Tool name list
                        .trustMode(false)
                        .build(),
                ReActAgent.builder()
                        .isMaster(true) // Set as master agent
                        .name("master_agent")
                        .subAgents(Arrays.asList("time_agent")) // Sub-agent list
                        .build()
        );
    }

    /**
     * Application main entry point
     * Initialize MCP tools and start Spring Boot application
     *
     * @param args command line arguments
     * @throws Exception when application startup fails
     */
    public static void main(String[] args) throws Exception {
        Objects.requireNonNull(args, "Command line arguments cannot be null");

        Map<String, Object> headers = new HashMap<>();
        headers.put("headers", Map.of("ukInfo2", "jtest2", "ukInfo3", "jtest3"));
        headers.put("_headers", Map.of("ukInfo4", "jtest4", "ukInfo5", "jtest5"));

        Mas mas = new Mas("app", getDefaultOxySpace());
        mas.init();

        // Simulate passing header parameters through HTTP request shared data
        Map<String, Object> arguments = new HashMap<>(Map.of("query", "What time is it", "shared_data", headers, "request_id", CommonUtils.generateShortUUID()));
        mas.chatWithAgent(arguments);

    }
}
