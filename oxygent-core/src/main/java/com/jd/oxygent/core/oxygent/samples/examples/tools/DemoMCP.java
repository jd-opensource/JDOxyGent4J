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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

/**
 * MCP (Model Context Protocol) Tool Demo Class
 * Demonstrates how to configure and use MCP client for time query and other functions
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DemoMCP {

    /**
     * Get default OxySpace configuration, including MCP tools and related agents
     *
     * @return BaseOxy list containing MCP tools and agents
     * @throws IllegalArgumentException when configuration parameters are invalid
     */
    @OxySpaceBean(value = "MCPToolJavaOxySpace", defaultStart = true, query = "What time is it")
    public static List<BaseOxy> getDefaultOxySpace() {
        // Apply JDK17 var keyword and parameter validation
        var apiKey = EnvUtils.getEnv("OXY_LLM_API_KEY");
        var baseUrl = EnvUtils.getEnv("OXY_LLM_BASE_URL");
        var modelName = EnvUtils.getEnv("OXY_LLM_MODEL_NAME");

        // Create time MCP tool client
        var mcpCommand = "uvx";
        var mcpArgs = Arrays.asList(
                "mcp-server-time",
                "--local-timezone=Asia/Shanghai"
        );

        var timeMcpTools = new StdioMCPClient("time", mcpCommand, mcpArgs);

        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .build(),
                timeMcpTools,
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

        // Apply JDK17 var keyword
        var currentClassName = Thread.currentThread().getStackTrace()[1].getClassName();
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(currentClassName);
        ServerApp.main(args);
    }
}
