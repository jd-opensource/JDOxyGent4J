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
package com.jd.oxygent.core.oxygent.samples.examples.llms;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Demonstrates how to configure LLM response in json format
 *
 * @author OxyGent Team
 * @version 1.0.10.4
 * @since 1.0.10.4
 */
public class DemoLlmParams {

    /**
     * Get default OxySpace configuration with streaming chat functionality
     *
     * @return BaseOxy list containing streaming chat agent
     * @throws IllegalArgumentException when configuration parameters are invalid
     */
    @OxySpaceBean(value = "chatAgentStreamJavaOxySpace", defaultStart = true, query = "Extract the following information: Zhang San, 25 years old, skilled in Java and Python.")
    public static List<BaseOxy> getDefaultOxySpace() throws JsonMappingException {
        // Parameter validation
        var apiKey = EnvUtils.getEnv("OXY_LLM_API_KEY");
        var baseUrl = EnvUtils.getEnv("OXY_LLM_BASE_URL");
        var modelName = EnvUtils.getEnv("OXY_LLM_MODEL_NAME");

        ObjectMapper mapper = new ObjectMapper();
        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);

        // 1. 将 Java Bean 转换为 JSON Schema
        JsonSchema schema = schemaGen.generateSchema(UserProfile.class);

        // 2. 构建 response_format 结构 (以 Structured Outputs 格式为例)
        var responseFormat = mapper.createObjectNode();
        responseFormat.put("type", "json_schema");

        var jsonSchemaNode = responseFormat.putObject("json_schema");
        jsonSchemaNode.put("name", "user_profile_schema");
        jsonSchemaNode.put("strict", true);
        jsonSchemaNode.set("schema", mapper.valueToTree(schema));

        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .llmParams(Map.of("temperature", 0.01,
                                "response_format", jsonSchemaNode))
                        .build(),
                ChatAgent.builder()
                        .name("qa_agent")
                        .llmModel("default_llm")
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
        var currentClassName = Thread.currentThread().getStackTrace()[1].getClassName();
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(currentClassName);
        ServerApp.main(args);
    }
}

class UserProfile {
    @JsonPropertyDescription("The full name of the user")
    @JsonProperty(required = true)
    public String name;

    @JsonPropertyDescription("The age of the user, must be a positive integer")
    @JsonProperty(required = true)
    public int age;

    @JsonPropertyDescription("List of programming languages the user is proficient in")
    public String[] languages;
}
