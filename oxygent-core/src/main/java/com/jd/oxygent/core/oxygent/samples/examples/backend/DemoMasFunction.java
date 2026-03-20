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
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import com.jd.oxygent.core.oxygent.utils.OSUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo class for demonstrating MAS (Multi-Agent System) function customization and message processing
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class DemoMasFunction {

    /**
     * Get default OxySpace configuration for MAS demonstration
     *
     * <p>This method defines the core components of the MAS environment, including:
     * <ul>
     *   <li>HTTP-based language model with environment-specific configuration</li>
     *   <li>Time tools MCP client for time-related operations</li>
     *   <li>ReAct agent with access to time tools</li>
     * </ul>
     * </p>
     *
     * @return List of BaseOxy components forming the MAS environment
     */
    @OxySpaceBean(value = "demoMasFunction", defaultStart = true, query = "What time it is?")
    public static List<BaseOxy> getDefaultOxySpace() {

        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .build(),
                OSUtil.isWindows() ?
                        new StdioMCPClient("time_tools", "cmd.exe", Arrays.asList("/c", "uvx", "mcp-server-time", "--local-timezone=Asia/Shanghai"))
                        :
                        new StdioMCPClient("time_tools", "uvx", Arrays.asList("mcp-server-time", "--local-timezone=Asia/Shanghai")),
                ReActAgent.builder()
                        .name("time_agent")
                        .tools(Arrays.asList("time_tools"))
                        .isMaster(true)
                        .build()
        );
    }

    /**
     * Application main entry point - demonstrates MAS initialization and custom message processing
     *
     * <p>This method shows the complete workflow of:
     * <ul>
     *   <li>Creating a MAS instance with custom OxySpace</li>
     *   <li>Initializing the MAS environment</li>
     *   <li>Registering a custom message body processor</li>
     *   <li>Calling an agent with structured arguments</li>
     * </ul>
     * </p>
     *
     * @param args Command line arguments (not used in this demo)
     * @throws Exception If any MAS operation fails
     */
    public static void main(String[] args) throws Exception {

        Mas mas = new Mas("demoMasFunction", getDefaultOxySpace());
        mas.init();
        mas.setFuncProcessMessageBody(DemoMasFunction::funcProcessMessageBody);

        Map<String, Object> arguments = new HashMap<>(Map.of("query", "What time it is?", "abandoned_field", "Do not save it"));
        mas.call("time_agent", arguments);
    }

    /**
     * Custom message body processor to filter and modify request content
     *
     * @param body The original message body map
     * @return The processed message body with filtered arguments
     */
    public static Map funcProcessMessageBody(Map body) {
        if (body.get("content") instanceof Map contentMap) {
            if (contentMap.get("arguments") instanceof Map argumentsMap) {
                Map _arguments = argumentsMap;
                _arguments.remove("abandoned_field");
            }
        }
        return body;
    }
}
