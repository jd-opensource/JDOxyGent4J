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
package com.jd.oxygent.core.oxygent.schemas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

/**
 * <h3>Large Language Model (LLM) State Management Module</h3>
 *
 * <p>This class defines the execution states and response formats for large language models (LLMs) in the OxyGent system,
 * standardizing the handling of LLM invocation results. It includes state enums and response data structures,
 * supporting multiple execution states such as tool invocation, direct answer, parse error, and call error.</p>
 *
 * <h3>Main Components:</h3>
 * <ul>
 *   <li><strong>LLMState</strong>: LLM execution state enum, defines four basic states</li>
 *   <li><strong>LLMResponse</strong>: LLM response data model, encapsulates execution results</li>
 * </ul>
 *
 * <h3>Status Transition Description:</h3>
 * <ul>
 *   <li>TOOL_CALL: LLM decides to invoke an external tool</li>
 *   <li>ANSWER: LLM gives a direct answer</li>
 *   <li>ERROR_PARSE: Response parsing failed</li>
 *   <li>ERROR_CALL: Invocation execution failed</li>
 * </ul>
 *
 * <h3>Usage Scenarios:</h3>
 * <ul>
 *   <li>Agent and LLM interaction result handling</li>
 *   <li>Status management for tool invocation chains</li>
 *   <li>Standardization of LLM response content</li>
 *   <li>Unified error state handling</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class LLM {

    /**
     * <h3>LLM Execution State Enum</h3>
     *
     * <p>Defines four basic states of large language models during execution,
     * used to identify the type of LLM response and subsequent processing logic.</p>
     *
     * <h4>Status Description:</h4>
     * <ul>
     *   <li><strong>TOOL_CALL</strong>: Tool invocation state, LLM decides to call an external tool</li>
     *   <li><strong>ANSWER</strong>: Direct answer state, LLM provides the final answer directly</li>
     *   <li><strong>ERROR_PARSE</strong>: Parse error state, the LLM response content cannot be parsed correctly</li>
     *   <li><strong>ERROR_CALL</strong>: Call error state, an exception occurred during the LLM call</li>
     * </ul>
     */
    @Getter
    @AllArgsConstructor
    public enum LLMState {

        /**
         * Tool invocation state - LLM decides to call an external tool
         */
        TOOL_CALL("tool_call"),

        /**
         * Direct answer state - LLM provides the final answer
         */
        ANSWER("answer"),

        /**
         * Parse error state - response content parsing failed
         */
        ERROR_PARSE("error_parse"),

        /**
         * Call error state - exception occurred during invocation
         */
        ERROR_CALL("error_call");

        /**
         * State value, used for serialization and identification
         */
        private final String value;
    }

    /**
     * <h3>LLM Response Data Model</h3>
     *
     * <p>Encapsulates the complete response information of large language models,
     * including execution state, output content, and original response data.
     * Provides a unified data structure to handle different types of LLM response results.</p>
     *
     * <h4>Data Components:</h4>
     * <ul>
     *   <li><strong>state</strong>: Execution state, identifies the response type</li>
     *   <li><strong>output</strong>: Output content, supports multiple data types</li>
     *   <li><strong>oriResponse</strong>: Original response, retains the complete reply from the LLM</li>
     * </ul>
     *
     * <h4>Output Type Description:</h4>
     * <ul>
     *   <li>String: Text answer or error message</li>
     *   <li>List: Tool call list or structured data</li>
     *   <li>Map: Complex structured response data</li>
     * </ul>
     */
    @Data
    @AllArgsConstructor
    public static class LLMResponse {

        /**
         * LLM execution state
         * <p>Identifies the current response type and determines subsequent processing logic</p>
         */
        private LLMState state;

        /**
         * LLM output content
         * <p>Supports output of multiple data types, including String, List, and Map</p>
         * <p>The specific type depends on the LLM response state and content</p>
         */
        private Object output; // Union[str, list, dict]

        /**
         * LLM original response
         * <p>Saves the complete original content returned by the LLM for debugging and issue tracking</p>
         * <p>Defaults to an empty string</p>
         */
        private String oriResponse = "";
    }
}