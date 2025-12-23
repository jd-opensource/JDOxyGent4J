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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

/**
 * ReAct (Reasoning and Acting) Agent Demo Class
 * Demonstrates how to configure ReAct agents with reflection capabilities for tasks like mathematical calculations
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DemoReactAgent {

    /**
     * Master agent reflection function for validating response format and content
     * Ensures returned results conform to expected numeric format
     */
    public static final BiFunction<String, OxyRequest, String> MASTER_REFLEXION = (response, oxyRequest) -> {
        Objects.requireNonNull(response, "Response content cannot be null");
        Objects.requireNonNull(oxyRequest, "OxyRequest object cannot be null");

        // Apply JDK17 var keyword to simplify local variable declaration
        var numberPattern = "^[-+]?(\\d+(\\.\\d*)?|\\.\\d+)$";
        var pattern = Pattern.compile(numberPattern);
        // Apply JDK17 Pattern Matching to optimize instanceof checks
        if (!pattern.matcher(response.trim()).matches()) {
            return "Only answer with numbers";
        }

        return null; // Return null indicates validation passed
    };

    /**
     * Get default OxySpace configuration containing ReAct agent
     *
     * @return BaseOxy list containing ReAct agent
     * @throws IllegalArgumentException when configuration parameters are invalid
     */
    @OxySpaceBean(value = "reactAgentJavaOxySpace", defaultStart = true, query = "What is 1+1")
    public static List<BaseOxy> getDefaultOxySpace() {
        // Apply JDK17 var keyword and parameter validation
        var apiKey = EnvUtils.getEnv("OXY_LLM_API_KEY");
        var baseUrl = EnvUtils.getEnv("OXY_LLM_BASE_URL");
        var modelName = EnvUtils.getEnv("OXY_LLM_MODEL_NAME");

        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .build(),
                ReActAgent.builder()
                        .name("master_agent")
                        .llmModel("default_llm")
                        .funcReflexion(MASTER_REFLEXION)
                        .additionalPrompt("Please provide the optimal answer based on my question")
                        .build()


        );
    }

    /**
     * Application main entry point
     * Initialize ReAct agent and start Spring Boot application
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
