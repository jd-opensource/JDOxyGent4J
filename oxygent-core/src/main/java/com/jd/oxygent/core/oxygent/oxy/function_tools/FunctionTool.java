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

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseTool;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * FunctionTool is a wrapper that adapts functional interfaces to the OxyGent tool framework.
 * It allows dynamic function registration and execution within the Multi-Agent System.
 *
 * <p>This class provides:
 * <ul>
 *   <li>Dynamic function wrapping and execution</li>
 *   <li>Parameter schema generation and validation</li>
 *   <li>Integration with the OxyGent tool ecosystem</li>
 *   <li>Robust error handling and logging</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0
 * @since 1.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@Slf4j
public class FunctionTool extends BaseTool {
    private FunctionHub.ToolFunction funcProcess;
    private List<FunctionHub.ParamMeta> params;

    public FunctionTool(String name, String desc, FunctionHub.ToolFunction funcProcess, List<FunctionHub.ParamMeta> params) {
        this.setName(name);
        this.setDesc(desc);
        this.funcProcess = funcProcess;
        this.params = params != null && !params.isEmpty() ? params : new ArrayList<>();
        this.inputSchema = extractInputSchema();
        super.setDescForLlm();

        log.debug("Created FunctionTool '{}' with {} parameters", name, this.params.size());
    }


    /**
     * Extract and build the input parameter schema for the tool
     * <p>
     * This method automatically generates parameter definitions compliant with JSON Schema specifications
     * based on the tool's parameter metadata. The generated schema is used for parameter validation,
     * LLM tool invocation guidance, and development documentation generation.
     *
     * <h3>Schema Generation Process</h3>
     * <ol>
     *   <li><strong>Metadata Validation</strong>: Check the completeness and validity of parameter metadata</li>
     *   <li><strong>Parameter Traversal</strong>: Traverse all parameter definitions, extract type and description information</li>
     *   <li><strong>Required Assessment</strong>: Current version marks all parameters as required</li>
     *   <li><strong>Property Construction</strong>: Create detailed property definitions for each parameter</li>
     *   <li><strong>Schema Encapsulation</strong>: Encapsulate all information in standard JSON Schema format</li>
     * </ol>
     *
     * <h3>Schema Structure</h3>
     * <p>The generated schema contains the following standard fields:</p>
     * <ul>
     *   <li><strong>type</strong>: Fixed as "object", representing the parameter container</li>
     *   <li><strong>properties</strong>: Parameter property mapping, key is parameter name, value is parameter definition</li>
     *   <li><strong>required</strong>: List of required parameter names</li>
     * </ul>
     *
     * <h3>Parameter Property Definition</h3>
     * <p>Each parameter's properties include:</p>
     * <ul>
     *   <li><strong>type</strong>: Parameter data type (string, number, boolean, etc.)</li>
     *   <li><strong>description</strong>: Parameter function description for LLM understanding</li>
     *   <li><strong>default</strong>: Default value (if exists)</li>
     * </ul>
     *
     * <h3>Data Safety Handling</h3>
     * <ul>
     *   <li><strong>Null Value Check</strong>: Automatically skip invalid parameter metadata</li>
     *   <li><strong>Default Value Handling</strong>: Provide default values for missing types and descriptions</li>
     *   <li><strong>Name Normalization</strong>: Automatically remove leading and trailing spaces from parameter names</li>
     * </ul>
     *
     * <h3>Extensibility Design</h3>
     * <p>This method is designed with good extensibility:</p>
     * <ul>
     *   <li><strong>Optional Parameter Support</strong>: Reserved implementation space for optional parameters</li>
     *   <li><strong>Complex Type Support</strong>: Can be extended to support arrays, nested objects and other complex types</li>
     *   <li><strong>Validation Rules</strong>: Can add format validation, value range and other advanced rules</li>
     * </ul>
     *
     * <h3>Schema Example</h3>
     * <pre>{@code
     * {
     *   "type": "object",
     *   "required": ["input", "format"],
     *   "properties": {
     *     "input": {
     *       "type": "string",
     *       "description": "Input text to be processed"
     *     },
     *     "format": {
     *       "type": "string",
     *       "description": "Output format type",
     *       "default": "json"
     *     }
     *   }
     * }
     * }</pre>
     *
     * @return JSON Schema Map object containing complete parameter definitions
     */
    private Map<String, Object> extractInputSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        List<String> requiredParams = new ArrayList<>();
        Map<String, Object> properties = new LinkedHashMap<>();

        for (FunctionHub.ParamMeta param : params) {
            if (param == null || param.name == null || param.name.trim().isEmpty()) {
                log.warn("Skipping invalid parameter metadata");
                continue;
            }

            String paramName = param.name.trim();
            String typeName = param.nameType != null ? param.nameType : "string";
            String description = param.description != null ? param.description : "No description available";

            // For now, all parameters are considered required
            // This can be enhanced later with optional parameter support
            requiredParams.add(paramName);

            Map<String, Object> prop = new HashMap<>();
            prop.put("description", description);
            prop.put("type", typeName);

            // Add default value if present
            if (param.defaultValue != null) {
                prop.put("default", param.defaultValue);
            }

            properties.put(paramName, prop);
        }

        schema.put("type", "object");
        schema.put("required", requiredParams);
        schema.put("properties", properties);

        log.debug("Generated input schema with {} properties", properties.size());
        return schema;
    }

    // Parameter schema description
    public static class ParamSchema {
        public String name;
        public String type;
        public String description;
        public boolean required;

        public ParamSchema(String name, String type, String description, boolean required) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.required = required;
        }
    }

    @Override
    public void setMas(Mas mas) {
        this.mas = mas;
        log.debug("MAS set for tool '{}'", getName());
    }

    // Temporary test class
    public void apply() throws Exception {
    }

    /**
     * Executes the wrapped function with the provided request parameters.
     * This method maps the request arguments to function parameters and invokes the wrapped function.
     *
     * @param oxyRequest the request containing arguments for function execution
     * @return the response containing the function result or error information
     */
    @Override
    protected OxyResponse _execute(OxyRequest oxyRequest) {
        try {
            if (oxyRequest == null) {
                log.warn("Received null OxyRequest for tool '{}'", getName());
                return new OxyResponse(OxyState.FAILED, "Request cannot be null");
            }

            Map<String, Object> requestArgs = oxyRequest.getArguments();
            if (requestArgs == null) {
                requestArgs = new HashMap<>();
            }

            log.debug("Executing tool '{}' with {} request arguments", getName(), requestArgs.size());

            // Prepare function arguments based on parameter metadata
            Object[] args = new Object[params.size()];
            for (int i = 0; i < params.size(); i++) {
                FunctionHub.ParamMeta param = params.get(i);
                if (param == null || param.name == null) {
                    log.warn("Invalid parameter metadata at index {} for tool '{}'", i, getName());
                    args[i] = null;
                    continue;
                }

                String paramName = param.name;
                Object value = requestArgs.get(paramName);
                if ((value == null || (value instanceof Map && ((Map) value).size() == 0)) && "OxyRequest".equals(param.nameType)) {
                    value = oxyRequest;
                }
                // Use default value if parameter is missing and default is available
                if (value == null && param.defaultValue != null) {
                    value = param.defaultValue;
                    log.debug("Using default value for parameter '{}' in tool '{}'", paramName, getName());
                }

                args[i] = value;
            }

            // Execute the wrapped function
            Object result = funcProcess.apply(args);

            log.debug("Tool '{}' executed successfully", getName());
            return new OxyResponse(OxyState.COMPLETED, result);

        } catch (Exception e) {
            log.error("Failed to execute tool '{}': {}", getName(), e.getMessage(), e);
            return new OxyResponse(OxyState.FAILED, "Tool execution failed: " + e.getMessage());
        }
    }
}
