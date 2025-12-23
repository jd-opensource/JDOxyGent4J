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
package com.jd.oxygent.core.oxygent.samples.examples;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.tools.PresetTools;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

/**
 * Demo example class from README documentation, demonstrating how to configure and use basic features of OxyGent framework
 * Contains complete configuration examples for LLM models, tools and agents
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DemoInReadme {

    /**
     * Get default OxySpace configuration
     * Create complete configuration space containing LLM, tools and agents
     *
     * @return BaseOxy list containing all necessary components
     * @throws IllegalArgumentException when configuration parameters are invalid
     */
    @OxySpaceBean(value = "defaultJavaOxySpace", defaultStart = true, query = "What time is it now Asia/Shanghai? Please save it into time.txt.")
    public static List<BaseOxy> getDefaultOxySpace() {
        // Using JDK17 var keyword to simplify local variable declaration
        var apiKey = EnvUtils.getEnv("OXY_LLM_API_KEY");
        var baseUrl = EnvUtils.getEnv("OXY_LLM_BASE_URL");
        var modelName = EnvUtils.getEnv("OXY_LLM_MODEL_NAME");

        return Arrays.asList(
                // 1. HTTP LLM configuration
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .llmParams(Map.of("temperature", 0.01)) // Using Map.of to create immutable Map
                        .timeout(30)
                        .build(),

                // 2. Time tools
                PresetTools.TIME_TOOLS,

                // 3. Time agent
                ReActAgent.builder()
                        .name("time_agent")
                        .desc("Tool agent capable of querying time")
                        .tools(Arrays.asList("time_tools")) // Tool name list
                        .build(),

                // 4. File tools
                PresetTools.FILE_TOOLS,

                // 5. File agent
                ReActAgent.builder()
                        .name("file_agent")
                        .desc("Tool agent capable of file system operations")
                        .tools(Arrays.asList("file_tools"))
                        .build(),

                // 6. Math tools
                PresetTools.MATH_TOOLS,

                // 7. Math agent
                ReActAgent.builder()
                        .name("math_agent")
                        .desc("Tool agent capable of mathematical calculations")
                        .tools(Arrays.asList("math_tools"))
                        .build(),

                // 8. Master Agent
                ReActAgent.builder()
                        .isMaster(true) // Set as master agent
                        .name("master_agent")
                        .llmModel("default_llm")
                        .subAgents(Arrays.asList("time_agent", "file_agent", "math_agent")) // Sub-agent list
                        .build()
        );
    }

    /**
     * Application main entry point
     * Initialize OxySpace mapping and start Spring Boot application
     *
     * @param args command line arguments
     * @throws Exception when application startup fails
     */
    public static void main(String[] args) throws Exception {
        Objects.requireNonNull(args, "Command line arguments cannot be null");

        // Using JDK17 var keyword
        var currentClassName = Thread.currentThread().getStackTrace()[1].getClassName();
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(currentClassName);
        ServerApp.main(args);
    }
}
