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
package com.jd.oxygent.core.oxygent.schemas.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Tool Call Information Wrapper Class
 *
 * <p>This class is used to encapsulate tool call information in the agent system, including call ID, type, and specific function information.
 * It is typically used in the interaction process between LLM models and external tools to record and transfer detailed information of tool calls.</p>
 *
 * <p>Main Functions:</p>
 * <ul>
 *     <li>Uniquely identify tool call instances</li>
 *     <li>Support different types of tool calls (default is function type)</li>
 *     <li>Encapsulate specific function call information</li>
 *     <li>Facilitate tool call tracking and result association</li>
 * </ul>
 *
 * <p>Usage Scenarios:</p>
 * <ul>
 *     <li>LLM model requests to call external tools</li>
 *     <li>Tool chain calls in agent systems</li>
 *     <li>Tool call records in conversation messages</li>
 *     <li>Association tracking of tool execution results</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>
 * // Create tool call
 * ToolCall toolCall = ToolCall.builder()
 *     .id("call_123456")
 *     .type("function")
 *     .function(Function.of("searchTool", "{\"query\":\"user query\"}"))
 *     .build();
 * </pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCall {

    /**
     * Unique identifier for the tool call
     */
    private String id;

    /**
     * Tool call type, default is "function"
     */
    @Builder.Default
    private String type = "function";

    /**
     * Specific function call information
     */
    private Function function;

}
