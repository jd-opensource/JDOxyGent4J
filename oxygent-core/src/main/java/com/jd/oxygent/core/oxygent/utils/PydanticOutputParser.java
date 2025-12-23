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
package com.jd.oxygent.core.oxygent.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Java implementation of Pydantic Output Parser.
 * Used to generate formatting instructions based on specified Java Bean classes and parse JSON output from LLM.
 *
 * @param <T> Expected output type, must be a Java Bean.
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class PydanticOutputParser<T> {

    private static final String PYDANTIC_FORMAT_TMPL = """
            Here's a JSON schema to follow:
            %s
            
            Output a valid JSON object but do not repeat the schema.
            Omit any markdown formatting.
            Do not include any other text than the JSON object.
            Do not include any preamble or explanation.
            Do not repeat the schema.
            """;

    private final Class<T> outputClass;
    private final Set<String> excludedSchemaKeysFromFormat;
    private final String pydanticFormatTmpl;
    private final ObjectMapper objectMapper;
    private final JsonSchemaGenerator schemaGenerator;

    /**
     * Constructor.
     *
     * @param outputClass                  Expected output type.
     * @param excludedSchemaKeysFromFormat List of keys to exclude from Schema.
     * @param pydanticFormatTmpl           Formatting template.
     */
    public PydanticOutputParser(
            Class<T> outputClass,
            List<String> excludedSchemaKeysFromFormat,
            String pydanticFormatTmpl) {

        this.outputClass = outputClass;
        this.excludedSchemaKeysFromFormat = excludedSchemaKeysFromFormat != null ?
                Set.copyOf(excludedSchemaKeysFromFormat) : Set.of();
        this.pydanticFormatTmpl = pydanticFormatTmpl != null ? pydanticFormatTmpl : PYDANTIC_FORMAT_TMPL;

        // Configure ObjectMapper
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // Create Schema generator
        this.schemaGenerator = new JsonSchemaGenerator(objectMapper);
    }

    /**
     * Get expected output type.
     */
    public Class<T> getOutputClass() {
        return outputClass;
    }

    /**
     * Get formatting string (for appending to LLM prompts).
     * By default, JSON braces are escaped to prevent conflicts with prompt templates.
     */
    public String getFormatString() {
        return getFormatString(true);
    }

    /**
     * Get formatting string.
     *
     * @param escapeJson Whether to escape { and } in JSON.
     * @return Formatting instruction string.
     */
    public String getFormatString(boolean escapeJson) {
        try {
            JsonSchema jsonSchema = schemaGenerator.generateSchema(outputClass);
            Map<String, Object> schemaMap = objectMapper.convertValue(jsonSchema, Map.class);

            // Remove keys that need to be excluded
            for (String key : excludedSchemaKeysFromFormat) {
                schemaMap.remove(key);
            }

            String schemaStr = objectMapper.writeValueAsString(schemaMap);
            String outputStr = String.format(pydanticFormatTmpl, schemaStr);

            if (escapeJson) {
                return outputStr.replace("{", "{{").replace("}", "}}");
            } else {
                return outputStr;
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to generate JSON schema for class: " + outputClass.getName(), e);
        }
    }

    /**
     * Parse LLM text output, extract and validate JSON, return corresponding Java Bean object.
     *
     * @param text LLM raw text output.
     * @return Parsed Java Bean object.
     * @throws RuntimeException If parsing or validation fails.
     */
    public T parse(String text) {
        String jsonStr = CommonUtils.extractJsonStr(text);
        try {
            return objectMapper.readValue(jsonStr, outputClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON string: " + jsonStr, e);
        }
    }

    /**
     * Combine user query with formatting instructions to generate final LLM prompt.
     *
     * @param query User's original query.
     * @return Complete prompt with structured output instructions.
     */
    public String format(String query) {
        return query + "\n\n" + getFormatString(true);
    }


    /**
     * Test class
     */
    static class TodoItem {
        private String title;
        private String description;
        private boolean completed;

        // Constructor, Getter, Setter
        public TodoItem() {
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }
    }

    public static void main(String[] args) {
        // 1. Create Parser
        PydanticOutputParser<TodoItem> parser = new PydanticOutputParser<>(
                TodoItem.class,
                null, // Don't exclude any Schema keys
                null  // Use default template
        );

        // 2. Generate formatting instructions
        String instruction = parser.getFormatString();
        System.out.println("=== LLM Instruction ===");
        System.out.println(instruction);

        // 3. Generate complete prompt
        String userQuery = "Create a todo item for buying groceries.";
        String fullPrompt = parser.format(userQuery);
        System.out.println("\n=== Full Prompt ===");
        System.out.println(fullPrompt);

        // 4. Simulate LLM output
        String llmOutput = "Some text before the JSON... {\"title\": \"Buy Groceries\", \"description\": \"Milk, Bread, Eggs\", \"completed\": false} ... some text after.";

        // 5. Parse LLM output
        TodoItem todoItem = parser.parse(llmOutput);
        System.out.println("\n=== Parsed Object ===");
        System.out.println("Title: " + todoItem.getTitle());
        System.out.println("Description: " + todoItem.getDescription());
        System.out.println("Completed: " + todoItem.isCompleted());
    }
}
