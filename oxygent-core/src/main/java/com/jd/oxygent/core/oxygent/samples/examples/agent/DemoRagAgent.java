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
import java.util.function.Function;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.RAGAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * RAG (Retrieval-Augmented Generation) Agent Demo Class
 * Demonstrates how to configure and use RAG agents for knowledge retrieval and question answering
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class DemoRagAgent {

    /**
     * Knowledge retrieval function for retrieving relevant knowledge based on query content
     * In actual implementation, this would query vector databases, search engines, or knowledge bases
     */
    public static final Function<OxyRequest, String> FUNC_RETRIEVE_KNOWLEDGE = (oxyRequest) -> {
        Objects.requireNonNull(oxyRequest, "OxyRequest object cannot be null");

        // Apply JDK17 var keyword to simplify local variable declaration
        var query = Objects.requireNonNull(oxyRequest.getQuery(), "Query content cannot be null");
        log.info("Retrieving knowledge for query: " + query);

        // Simulate knowledge retrieval - in real implementation, this would query vector databases, search engines, or knowledge bases
        return """
                Pi is 3.141592653589793238462643383279502.
                """;
    };

    /**
     * Get default OxySpace configuration containing RAG agent
     *
     * @return BaseOxy list containing RAG agent
     * @throws IllegalArgumentException when configuration parameters are invalid
     */
    @OxySpaceBean(value = "ragAgentJavaOxySpace", defaultStart = true, query = "Please calculate the 20 positions of Pi")
    public static List<BaseOxy> getDefaultOxySpace() {
        var apiKey = EnvUtils.getEnv("OXY_LLM_API_KEY");
        var baseUrl = EnvUtils.getEnv("OXY_LLM_BASE_URL");
        var modelName = EnvUtils.getEnv("OXY_LLM_MODEL_NAME");

        // Apply JDK17 Text Blocks for handling multi-line strings
        var promptTemplate = """
                You are a helpful assistant! You can refer to the following knowledge to answer questions:
                ${knowledge}
                """;

        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .isMultimodalSupported(false)
                        .modelName(modelName)
                        .build(),
                RAGAgent.builder()
                        .name("qa_agent")
                        .llmModel("default_llm")
                        .prompt(promptTemplate)
                        .isMultimodalSupported(false)
                        .knowledgePlaceholder("knowledge")
                        .funcRetrieveKnowledge(FUNC_RETRIEVE_KNOWLEDGE)
                        .build()
        );
    }

    /**
     * Application main entry point
     * Initialize RAG agent and start Spring Boot application
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
