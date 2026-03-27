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
package com.jd.oxygent.core.oxygent.samples.examples.function_hubs;

import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.tools.PresetTools;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Chat Agent Stream Processing Demo Class
 * Demonstrates how to configure chat agents with streaming output support
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class FunctionHubsChartDemo {

    /**
     * Get default OxySpace configuration with streaming chat functionality
     *
     * @return BaseOxy list containing streaming chat agent
     * @throws IllegalArgumentException when configuration parameters are invalid
     */
    @OxySpaceBean(value = "functionHubsChartDemo", defaultStart = true, query = "hello")
    public static List<BaseOxy> getDefaultOxySpace() {

        Config.getMessage().setShowInTerminal(true);

        // Apply JDK17 var keyword to simplify local variable declaration
        var streamParams = new HashMap<String, Object>();
        streamParams.put("stream", true);

        // Parameter validation
        var apiKey = EnvUtils.getEnv("OXY_LLM_API_KEY");
        var baseUrl = EnvUtils.getEnv("OXY_LLM_BASE_URL");
        var modelName = EnvUtils.getEnv("OXY_LLM_MODEL_NAME");

        return Arrays.asList(
                PresetTools.FLOW_IMAGE_GEN_TOOLS,
                PresetTools.OPEN_CHART_TOOLS,
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .llmParams(streamParams) // Enable streaming output
                        .build(),
                ReActAgent.builder()
                        .name("qa_agent")
                        .llmModel("default_llm")
                        .tools(Arrays.asList("flow_image_gen_tools", "open_chart_tools"))
                        .build()
        );
    }

    /**
     * Application main entry point
     * Initialize streaming chat agent and start Spring Boot application
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