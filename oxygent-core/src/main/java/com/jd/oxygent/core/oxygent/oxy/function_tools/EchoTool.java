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
package com.jd.oxygent.core.oxygent.oxy.function_tools;

import com.jd.oxygent.core.oxygent.oxy.BaseTool;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * <h3>Echo Tool - Message Echo Functionality Tool</h3>
 *
 * <p>EchoTool is an example tool class in the OxyGent framework that provides simple message echo functionality.
 * This tool is primarily used to demonstrate standard patterns and best practices for OxyGent tool development,
 * and is also a useful tool for system debugging and testing.</p>
 *
 * <h3>Technical Architecture</h3>
 * <ul>
 *   <li><strong>Inheritance System</strong>: Inherits from BaseTool, following the standard architecture of the OxyGent tool framework</li>
 *   <li><strong>Parameter Validation</strong>: Complete input parameter validation and error handling mechanisms</li>
 *   <li><strong>Schema Definition</strong>: Dynamically generates JSON Schema, supporting automatic LLM invocation</li>
 *   <li><strong>State Management</strong>: Complete execution state tracking and response encapsulation</li>
 * </ul>
 *
 * <h3>Core Features</h3>
 * <ul>
 *   <li><strong>Message Echo</strong>: Receives input messages and returns them with optional prefix</li>
 *   <li><strong>Prefix Customization</strong>: Supports custom message prefix, defaults to "Echo: "</li>
 *   <li><strong>Metadata Collection</strong>: Collects and returns additional information such as original message, used prefix, message length</li>
 *   <li><strong>Error Handling</strong>: Gracefully handles various exception scenarios, providing clear error messages</li>
 * </ul>
 *
 * <h3>Parameter Definition</h3>
 * <ul>
 *   <li><strong>message</strong> (required): The message content to echo, string type</li>
 *   <li><strong>prefix</strong> (optional): Message prefix, defaults to "Echo: ", string type</li>
 * </ul>
 *
 * <h3>Usage Scenarios</h3>
 * <ul>
 *   <li><strong>Tool Development Example</strong>: Provides standard implementation templates for new tool development</li>
 *   <li><strong>System Testing</strong>: Verifies the correctness of tool invocation chains and parameter passing</li>
 *   <li><strong>Debugging Assistance</strong>: Serves as intermediate steps in complex workflows to verify data transmission</li>
 *   <li><strong>LLM Interaction Testing</strong>: Tests LLM's understanding and invocation capabilities for tools</li>
 * </ul>
 *
 * <h3>Design Patterns</h3>
 * <p>This tool follows the following design patterns and principles:</p>
 * <ul>
 *   <li><strong>Template Method Pattern</strong>: Implements abstract methods of BaseTool, following framework conventions</li>
 *   <li><strong>Builder Pattern</strong>: Uses Lombok @SuperBuilder to support chained construction</li>
 *   <li><strong>Defensive Programming</strong>: Comprehensive parameter validation and exception handling</li>
 *   <li><strong>Single Responsibility Principle</strong>: Focuses on the single function of message echoing</li>
 * </ul>
 *
 * <h3>JSON Schema Example</h3>
 * <pre>{@code
 * {
 *   "type": "object",
 *   "properties": {
 *     "message": {
 *       "type": "string",
 *       "description": "The message to echo back"
 *     },
 *     "prefix": {
 *       "type": "string",
 *       "description": "Optional prefix to add to the echoed message",
 *       "default": "Echo: "
 *     }
 *   },
 *   "required": ["message"]
 * }
 * }</pre>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Create Echo tool instance
 * EchoTool echoTool = new EchoTool();
 *
 * // Build request parameters
 * Map<String, Object> args = Map.of(
 *     "message", "Hello, OxyGent!",
 *     "prefix", "System: "
 * );
 * OxyRequest request = new OxyRequest();
 * request.setArguments(args);
 *
 * // Execute tool
 * OxyResponse response = echoTool.execute(request);
 * System.out.println(response.getOutput()); // Output: "System: Hello, OxyGent!"
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@Slf4j
public class EchoTool extends BaseTool {

    /**
     * Initialize Echo tool instance
     * <p>
     * The constructor is responsible for completing the complete initialization process of the tool,
     * including basic property setting, parameter schema construction, LLM description generation
     * and other key steps. Ensures the tool instance is fully usable.
     *
     * <h3>Initialization Steps</h3>
     * <ol>
     *   <li>Call parent constructor to initialize BaseTool basic functionality</li>
     *   <li>Set tool name to "echo" for tool registration and identification</li>
     *   <li>Set tool description explaining the tool's functionality and purpose</li>
     *   <li>Build JSON Schema definition for input parameters</li>
     *   <li>Generate tool description suitable for LLM understanding</li>
     *   <li>Record initialization completion debug log</li>
     * </ol>
     *
     * <h3>Tool Properties</h3>
     * <ul>
     *   <li><strong>Name</strong>: echo - concise and clear tool identifier</li>
     *   <li><strong>Description</strong>: detailed explanation of tool functionality, supporting LLM understanding and invocation</li>
     *   <li><strong>Schema</strong>: complete parameter definition ensuring type safety</li>
     * </ul>
     */
    public EchoTool() {
        super();
        setName("echo");
        setDesc("A simple echo tool that returns the input message with optional prefix");
        setupInputSchema();
        setDescForLlm();
        log.debug("EchoTool initialized");
    }

    /**
     * Build JSON Schema definition for tool input parameters
     * <p>
     * This method creates parameter definitions compliant with JSON Schema specifications,
     * used for parameter validation and LLM invocation guidance. The schema defines parameter
     * types, descriptions, default values and requirements, ensuring accuracy of tool invocation.
     *
     * <h3>Schema Structure</h3>
     * <ul>
     *   <li><strong>type</strong>: object - parameter container type</li>
     *   <li><strong>properties</strong>: specific parameter definition mapping</li>
     *   <li><strong>required</strong>: required parameter list</li>
     * </ul>
     *
     * <h3>Parameter Definition Details</h3>
     * <ul>
     *   <li><strong>message parameter</strong>:
     *     <ul>
     *       <li>Type: string</li>
     *       <li>Required: yes</li>
     *       <li>Description: message content to echo back</li>
     *     </ul>
     *   </li>
     *   <li><strong>prefix parameter</strong>:
     *     <ul>
     *       <li>Type: string</li>
     *       <li>Required: no</li>
     *       <li>Default value: "Echo: "</li>
     *       <li>Description: optional message prefix</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <h3>LLM Integration</h3>
     * <p>The generated schema will be used by LLM to help it understand how to correctly invoke this tool.
     * Clear parameter descriptions and type definitions ensure LLM can accurately construct invocation parameters.</p>
     */
    private void setupInputSchema() {
        Map<String, Object> schema = new HashMap<>();

        // Properties
        Map<String, Object> properties = new HashMap<>();

        // Message property
        Map<String, Object> messageProperty = new HashMap<>();
        messageProperty.put("type", "string");
        messageProperty.put("description", "The message to echo back");
        properties.put("message", messageProperty);

        // Prefix property (optional)
        Map<String, Object> prefixProperty = new HashMap<>();
        prefixProperty.put("type", "string");
        prefixProperty.put("description", "Optional prefix to add to the echoed message");
        prefixProperty.put("default", "Echo: ");
        properties.put("prefix", prefixProperty);

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", new String[]{"message"});

        setInputSchema(schema);
    }

    /**
     * Executes the echo tool functionality.
     *
     * <p>This method processes the input request, extracts the message and optional prefix
     * parameters, validates the input, and returns an echoed response.
     *
     * @param oxyRequest the request containing the parameters for the echo operation
     * @return OxyResponse containing the echoed message or error information
     * @throws IllegalArgumentException if the request or required parameters are invalid
     */
    @Override
    protected OxyResponse _execute(OxyRequest oxyRequest) {
        log.debug("Executing EchoTool with request: {}", oxyRequest);

        try {
            // Validate request
            if (oxyRequest == null) {
                log.error("OxyRequest is null");
                return createErrorResponse("Request cannot be null");
            }

            Map<String, Object> arguments = oxyRequest.getArguments();
            if (arguments == null) {
                log.error("Request arguments are null");
                return createErrorResponse("Request arguments cannot be null");
            }

            // Extract and validate message parameter
            Object messageObj = arguments.get("message");
            if (messageObj == null) {
                log.error("Message parameter is missing");
                return createErrorResponse("Message parameter is required");
            }

            String message = messageObj.toString().trim();
            if (message.isEmpty()) {
                log.error("Message parameter is empty");
                return createErrorResponse("Message cannot be empty");
            }

            // Extract optional prefix parameter with default value
            String prefix = "Echo: ";
            Object prefixObj = arguments.get("prefix");
            if (prefixObj != null) {
                prefix = prefixObj.toString();
            }

            // Process the message
            String echoedMessage = prefix + message;
            log.debug("Generated echoed message: {}", echoedMessage);

            // Create successful response
            OxyResponse response = new OxyResponse();
            response.setState(OxyState.COMPLETED);
            response.setOutput(echoedMessage);

            // Add extra information
            Map<String, Object> extra = new HashMap<>();
            extra.put("original_message", message);
            extra.put("prefix_used", prefix);
            extra.put("message_length", message.length());
            response.setExtra(extra);

            log.debug("EchoTool execution completed successfully");
            return response;

        } catch (Exception e) {
            log.error("Unexpected error during echo tool execution", e);
            return createErrorResponse("Error processing echo request: " + e.getMessage());
        }
    }

    /**
     * Creates an error response with the specified error message.
     *
     * @param errorMessage the error message to include in the response
     * @return OxyResponse with FAILED state and the error message
     */
    private OxyResponse createErrorResponse(String errorMessage) {
        OxyResponse errorResponse = new OxyResponse();
        errorResponse.setState(OxyState.FAILED);
        errorResponse.setOutput(errorMessage);
        return errorResponse;
    }
}