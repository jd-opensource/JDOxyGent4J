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
package com.jd.oxygent.core.oxygent.schemas.exception;

/**
 * <h3>OxyGent System Custom Exception Class</h3>
 *
 * <p>This class is the unified exception handling class for the OxyGent intelligent agent framework,
 * inheriting from RuntimeException to encapsulate and handle various exception situations that occur during system runtime.
 * It provides a rich exception information passing mechanism, supporting exception chain tracking and detailed error context.</p>
 *
 * <h3>Exception Features:</h3>
 * <ul>
 *   <li><strong>Runtime Exception</strong>: Inherits from RuntimeException, no forced catching required</li>
 *   <li><strong>Exception Chain Support</strong>: Supports wrapping and passing of original exceptions</li>
 *   <li><strong>Rich Information</strong>: Provides detailed error description information</li>
 *   <li><strong>Unified Handling</strong>: Serves as the unified exit for system exceptions</li>
 * </ul>
 *
 * <h3>Usage Scenarios:</h3>
 * <ul>
 *   <li>Agent execution process exception encapsulation</li>
 *   <li>Tool invocation failure exception handling</li>
 *   <li>Configuration errors and parameter validation exceptions</li>
 *   <li>System resource access exceptions</li>
 *   <li>Third-party service invocation exception wrapping</li>
 * </ul>
 *
 * <h3>Exception Classification Suggestions:</h3>
 * <ul>
 *   <li><strong>Configuration Exception</strong>: "CONFIG_ERROR: Configuration file format error"</li>
 *   <li><strong>Execution Exception</strong>: "EXEC_ERROR: Tool execution failed"</li>
 *   <li><strong>Network Exception</strong>: "NETWORK_ERROR: Network connection timeout"</li>
 *   <li><strong>Permission Exception</strong>: "AUTH_ERROR: Insufficient access permissions"</li>
 * </ul>
 *
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * // Simple exception throwing
 * throw new OxyException("Tool execution failed: Parameter validation error");
 *
 * // Exception wrapping
 * try {
 *     riskyOperation();
 * } catch (Exception e) {
 *     throw new OxyException("Error occurred while executing external tool", e);
 * }
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class OxyException extends RuntimeException {

    /**
     * Construct simple exception
     * <p>Create an OxyGent exception object containing error description information</p>
     *
     * <h4>Usage Scenarios:</h4>
     * <ul>
     *   <li>Business logic validation failure</li>
     *   <li>Parameter format error</li>
     *   <li>State check exception</li>
     * </ul>
     *
     * @param message Exception description information, should clearly describe the error cause and context
     */
    public OxyException(String message) {
        super(message);
    }

    /**
     * Construct exception chain
     * <p>Create an OxyGent exception object containing original exception information, used for exception wrapping and conversion</p>
     *
     * <h4>Usage Scenarios:</h4>
     * <ul>
     *   <li>Third-party library exception wrapping</li>
     *   <li>Unified system exception handling</li>
     *   <li>Exception context enhancement</li>
     *   <li>Error information chain tracking</li>
     * </ul>
     *
     * <h4>Benefits of Exception Chain:</h4>
     * <ul>
     *   <li>Preserve complete information of original exception</li>
     *   <li>Provide better debugging experience</li>
     *   <li>Support complete exception stack tracking</li>
     * </ul>
     *
     * @param message Exception description information, describing the error situation in the current context
     * @param cause   Original exception object, maintaining the integrity of the exception chain
     */
    public OxyException(String message, Throwable cause) {
        super(message, cause);
    }
}
