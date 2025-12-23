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

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.tools.ParamMetaAuto;
import com.jd.oxygent.core.oxygent.tools.Tool;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Function Hub Demo Class
 * Demonstrates how to create and configure custom function tools using annotation decoration, including joke tool implementation
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class DemoFunctionHubAnnotation {

    public static class JokeTool extends FunctionHub {
        private static final Random RANDOM = new Random();

        public JokeTool() {
            super("joke_tools");
            this.setDesc("Tool collection for telling jokes");
        }

        // Register joke tool with appropriate parameter definitions
        @Tool(
                name = "joke_tool",
                description = "A tool that can generate various types of jokes",
                paramMetas = {@ParamMetaAuto(name = "joke_type", type = "String", description = "Type of the joke, default value is any")}
        )
        public static String jokeTool(String jokeType) {
            Objects.requireNonNull(jokeType, "Joke type cannot be null");
            // Apply JDK17 var keyword and immutable list
            var jokes = List.of(
                    "Why don't scientists trust atoms? Because they make up everything!",
                    "Why did the scarecrow win an award? Because he was outstanding in his field!",
                    "Why don't eggs tell jokes? They'd crack each other up!"
            );

            log.info("Joke type: " + jokeType);
            return jokes.get(RANDOM.nextInt(jokes.size()));
        }
    }

    /**
     * Get default OxySpace configuration, including function hub and related agents
     *
     * @return BaseOxy list containing function tools and agents
     * @throws IllegalArgumentException when configuration parameters are invalid
     */
    @OxySpaceBean(value = "functionHubJavaOxySpace", defaultStart = true, query = "Please tell a joke")
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
                new DemoFunctionHubAnnotation.JokeTool(),
                ReActAgent.builder()
                        .name("joke_agent")
                        .llmModel("default_llm")
                        .additionalPrompt("You are a humorous assistant. When users need jokes, please use joke_tool to get jokes.")
                        .tools(Arrays.asList("joke_tools"))
                        .build()
        );
    }

    /**
     * Application main entry point
     * Initialize function hub and start Spring Boot application
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
