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
import java.util.Random;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Function Hub Demo Class
 * Demonstrates how to create and configure custom function tools, including joke tool implementation
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class DemoFunctionHub {

    private static final Random RANDOM = new Random();
    private static final FunctionHub JOKE_TOOLS = new FunctionHub("joke_tools");

    /**
     * Initialize the function hub and register joke tools
     *
     * @throws IllegalArgumentException when tool registration parameters are invalid
     */
    private static void init() {
        // Set function hub description
        JOKE_TOOLS.setDesc("Tool collection for telling jokes");

        // Register joke tool with appropriate parameter definitions
        JOKE_TOOLS.registerTool(
                "joke_tool",
                "A tool that can generate various types of jokes",
                (args) -> {
                    try {
                        // Apply JDK17 var keyword to simplify local variable declaration
                        var jokeType = "any";
                        return jokeTool(jokeType);
                    } catch (Exception e) {
                        throw new RuntimeException("Joke tool execution failed: " + e.getMessage(), e);
                    }
                },
                Arrays.asList(
                        new FunctionHub.ParamMeta("joke_type", "String", "Type of the joke", "any")
                )
        );
    }

    /**
     * Joke tool implementation method
     * Returns a random joke based on the specified type
     *
     * @param jokeType joke type, currently supports any type
     * @return randomly selected joke string
     * @throws IllegalArgumentException when joke type is null
     */
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

    /**
     * Get default OxySpace configuration, including function hub and related agents
     *
     * @return BaseOxy list containing function tools and agents
     * @throws IllegalArgumentException when configuration parameters are invalid
     */
    @OxySpaceBean(value = "functionhubJavaOxySpace", defaultStart = true, query = "Please tell a joke")
    public static List<BaseOxy> getDefaultOxySpace() {
        init(); // Initialize function hub

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
                JOKE_TOOLS,
                ReActAgent.builder()
                        .name("joke_agent")
                        .llmModel("default_llm")
                        .additionalPrompt("You are a humorous assistant. When users need jokes, please use joke_tool to get jokes.")
                        .tools(Arrays.asList("joke_tool"))
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
