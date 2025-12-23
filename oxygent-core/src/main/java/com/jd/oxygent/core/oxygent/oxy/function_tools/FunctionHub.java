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
import com.jd.oxygent.core.oxygent.tools.ParamMetaAuto;
import com.jd.oxygent.core.oxygent.tools.Tool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * FunctionHub is a centralized registry and manager for dynamic function tools.
 * It provides functionality to register, manage, and execute various tools within the OxyGent framework.
 *
 * <p>This class serves as a hub for managing multiple function tools, allowing for:
 * <ul>
 *   <li>Dynamic tool registration and management</li>
 *   <li>Tool execution with parameter validation</li>
 *   <li>Thread-safe operations for concurrent environments</li>
 *   <li>Integration with the Multi-Agent System (MAS)</li>
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
public class FunctionHub extends BaseTool {

    /**
     * The name identifier for this function hub instance.
     */
    private String name;

    /**
     * Thread-safe map storing registered tools with their metadata.
     * Key: tool name, Value: tool metadata including function and parameters
     */
    private Map<String, ToolMeta> tools = new ConcurrentHashMap<>();

    /**
     * Gets the name of this function hub.
     *
     * @return the name of this function hub
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this function hub.
     *
     * @param name the name to set, must not be null or empty
     * @throws IllegalArgumentException if name is null or empty
     */
    @Override
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Function hub name cannot be null or empty");
        }
        this.name = name.trim();
    }

    /**
     * Gets an unmodifiable view of the registered tools.
     *
     * @return an unmodifiable map of tool names to their metadata
     */
    public Map<String, ToolMeta> getTools() {
        return Collections.unmodifiableMap(tools);
    }

    /**
     * Sets the tools map. This method validates that the tools map is not null.
     *
     * @param tools the tools map to set, must not be null
     * @throws IllegalArgumentException if tools is null
     */
    public void setTools(Map<String, ToolMeta> tools) {
        if (tools == null) {
            throw new IllegalArgumentException("Tools map cannot be null");
        }
        this.tools = new ConcurrentHashMap<>(tools);
    }

    /**
     * Constructs a FunctionHub with the specified name.
     *
     * @param name the name for this function hub, must not be null or empty
     * @throws IllegalArgumentException if name is null or empty
     */
    public FunctionHub(String name) {
        setName(name);
        // Automatically register all methods with @Tool annotation
        this.registerAnnotatedMethods();
    }

    /**
     * Initializes the function hub by registering all tools with the Multi-Agent System (MAS).
     * This method creates FunctionTool instances for each registered tool and adds them to the MAS.
     *
     * @throws IllegalStateException if MAS is not set or if tool registration fails
     */
    @Override
    public void init() {
        super.init();

        if (mas == null) {
            throw new IllegalStateException("MAS (Multi-Agent System) must be set before initialization");
        }

        log.debug("Initializing FunctionHub '{}' with {} tools", name, tools.size());

        // Iterate through registered tools and create FunctionTool instances
        for (Map.Entry<String, ToolMeta> entry : tools.entrySet()) {
            String toolName = entry.getKey();
            ToolMeta meta = entry.getValue();

            try {
                if (meta == null) {
                    log.warn("Skipping tool '{}' due to null metadata", toolName);
                    continue;
                }

                FunctionTool functionTool = new FunctionTool(
                        toolName,
                        meta.description,
                        meta.function,
                        meta.params
                );
                functionTool.setMas(mas);
                functionTool.apply();
                mas.addOxy(functionTool);

                log.debug("Successfully registered tool: {}", toolName);
            } catch (Exception e) {
                log.error("Failed to register tool '{}': {}", toolName, e.getMessage(), e);
                // Continue with other tools instead of failing completely
            }
        }

        log.debug("FunctionHub '{}' initialization completed", name);
    }

    /**
     * Register new tool to function hub
     * <p>
     * This method is one of the core functionalities of FunctionHub, responsible for registering
     * specific tool functions to the centralized tool manager. Registered tools can be discovered
     * and invoked by MAS (Multi-Agent System), enabling a dynamic tool ecosystem.
     *
     * <h3>Registration Process</h3>
     * <ol>
     *   <li><strong>Parameter Validation</strong>: Validate the validity of tool name, description and function implementation</li>
     *   <li><strong>Duplicate Check</strong>: Ensure tool name uniqueness to avoid conflicts</li>
     *   <li><strong>Metadata Encapsulation</strong>: Encapsulate tool information as ToolMeta object</li>
     *   <li><strong>Thread-Safe Storage</strong>: Use thread-safe mapping to store tool metadata</li>
     *   <li><strong>Logging</strong>: Record successful registration debug information</li>
     * </ol>
     *
     * <h3>Tool Metadata</h3>
     * <p>Each registered tool contains the following metadata:</p>
     * <ul>
     *   <li><strong>Tool Name</strong>: Unique identifier for tool lookup and invocation</li>
     *   <li><strong>Function Description</strong>: Human-readable function description to help understand tool purpose</li>
     *   <li><strong>Function Implementation</strong>: Actual business logic implementation, supports Lambda expressions</li>
     *   <li><strong>Parameter Definition</strong>: Detailed parameter type and description information</li>
     * </ul>
     *
     * <h3>Parameter Safety Handling</h3>
     * <ul>
     *   <li><strong>Null Protection</strong>: Automatically handle null parameter lists, create empty lists</li>
     *   <li><strong>Defensive Copying</strong>：Create parameter list copy to avoid external modification</li>
     *   <li><strong>Name Normalization</strong>：Automatically remove leading and trailing spaces from tool names</li>
     * </ul>
     *
     * <h3>Error Handling Strategy</h3>
     * <ul>
     *   <li><strong>Parameter Validation Failure</strong>：Throws IllegalArgumentException, clearly indicating the error cause</li>
     *   <li><strong>Name Conflict</strong>：Throws IllegalStateException to protect registered tools</li>
     *   <li><strong>Thread Safety</strong>：Use ConcurrentHashMap to ensure concurrent registration safety</li>
     * </ul>
     *
     * <h3>Usage Example</h3>
     * <pre>{@code
     * // Create parameter definition
     * List<ParamMeta> params = Arrays.asList(
     *     new ParamMeta("input", "string", "Input text", null),
     *     new ParamMeta("format", "string", "Output format", "json")
     * );
     *
     * // Register tool
     * functionHub.registerTool(
     *     "text_processor",
     *     "Text processing tool supporting multiple output formats",
     *     (args) -> processText((String)args[0], (String)args[1]),
     *     params
     * );
     * }</pre>
     *
     * @param toolName    Unique tool name, cannot be null or empty string, will be automatically trimmed
     * @param description Tool function description, cannot be null, used to help users understand tool purpose
     * @param function    Tool function implementation, cannot be null, supports Lambda expressions and method references
     * @param params      Parameter metadata list, can be null or empty list, automatically creates safe copy
     * @throws IllegalArgumentException If toolName is empty, description is null or function is null
     * @throws IllegalStateException    If a tool with the same name already exists, protects existing tools from being overwritten
     */
    public void registerTool(String toolName, String description, ToolFunction function, List<ParamMeta> params) {
        if (toolName == null || toolName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tool name cannot be null or empty");
        }
        if (description == null) {
            throw new IllegalArgumentException("Tool description cannot be null");
        }
        if (function == null) {
            throw new IllegalArgumentException("Tool function cannot be null");
        }

        String normalizedToolName = toolName.trim();
        if (tools.containsKey(normalizedToolName)) {
            throw new IllegalStateException("Tool with name '" + normalizedToolName + "' already exists");
        }

        List<ParamMeta> safeParams = params != null ? new ArrayList<>(params) : new ArrayList<>();
        tools.put(normalizedToolName, new ToolMeta(normalizedToolName, description, function, safeParams));

        log.debug("Registered tool: {} with {} parameters", normalizedToolName, safeParams.size());
    }

    /**
     * Calls a registered tool with the provided arguments.
     *
     * @param toolName the name of the tool to call, must not be null or empty
     * @param args     the arguments to pass to the tool function
     * @return the result of the tool execution
     * @throws IllegalArgumentException if toolName is null or empty
     * @throws IllegalStateException    if the tool is not found
     * @throws RuntimeException         if tool execution fails
     */
    public Object call(String toolName, Object... args) {
        if (toolName == null || toolName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tool name cannot be null or empty");
        }

        String normalizedToolName = toolName.trim();
        ToolMeta meta = tools.get(normalizedToolName);
        if (meta == null) {
            throw new IllegalStateException("Tool not found: " + normalizedToolName);
        }

        try {
            log.debug("Calling tool: {} with {} arguments", normalizedToolName, args != null ? args.length : 0);
            Object result = meta.function.apply(args);
            log.debug("Tool '{}' executed successfully", normalizedToolName);
            return result;
        } catch (Exception e) {
            log.error("Failed to execute tool '{}': {}", normalizedToolName, e.getMessage(), e);
            throw new RuntimeException("Tool execution failed for '" + normalizedToolName + "': " + e.getMessage(), e);
        }
    }

    /**
     * Returns an unmodifiable collection of all registered tool metadata.
     *
     * @return a collection of tool metadata for all registered tools
     */
    public Collection<ToolMeta> listTools() {
        return Collections.unmodifiableCollection(tools.values());
    }

    /**
     * Executes a tool request by delegating to the appropriate registered tool.
     *
     * @param oxyRequest the request containing tool name and arguments
     * @return the response from tool execution, or an error response if execution fails
     */
    @Override
    protected OxyResponse _execute(OxyRequest oxyRequest) {
        try {
            if (oxyRequest == null) {
                log.warn("Received null OxyRequest");
                return new OxyResponse(OxyState.FAILED, "Request cannot be null");
            }

            Map<String, Object> arguments = oxyRequest.getArguments();
            if (arguments == null || arguments.isEmpty()) {
                log.warn("No arguments provided in request");
                return new OxyResponse(OxyState.FAILED, "No tool arguments provided");
            }

            // Get the first argument entry as tool name and value
            Map.Entry<String, Object> first = arguments.entrySet().iterator().next();
            String toolName = first.getKey();
            Object toolArgs = first.getValue();

            log.debug("Executing tool '{}' via FunctionHub", toolName);
            Object result = this.call(toolName, toolArgs);

            return new OxyResponse(OxyState.COMPLETED, result);

        } catch (Exception e) {
            log.error("Failed to execute tool request: {}", e.getMessage(), e);
            return new OxyResponse(OxyState.FAILED, "Tool execution failed: " + e.getMessage());
        }
    }

    public void registerAnnotatedMethods() {
        Method[] methods = this.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Tool.class)) {
                Tool toolAnn = method.getAnnotation(Tool.class);
                String name = toolAnn.name();
                String description = toolAnn.description();
                ParamMetaAuto[] paramMetaAutos = toolAnn.paramMetas();

                List<ParamMeta> paramMetas = Arrays.stream(toolAnn.paramMetas())
                        .filter(Objects::nonNull)                     // Filter out null ParamMetaAuto
                        .map(paramAuto -> new ParamMeta(
                                paramAuto.name(),
                                paramAuto.type(),
                                paramAuto.description(),
                                null
                        ))
                        .collect(Collectors.toList());

                // Register Lambda: call this method
                this.registerTool(name, description, args -> {
                    try {
                        return method.invoke(this, args);
                    } catch (Exception e) {
                        log.error("Error invoking tool '" + name, e);
                        return "Error invoking tool '" + name + "': " + e.getMessage();
                    }
                }, paramMetas);
            }
        }
    }

    /**
     * Functional interface for tool implementations.
     * Tool functions should be thread-safe and handle their own parameter validation.
     */
    @FunctionalInterface
    public interface ToolFunction {
        /**
         * Applies the tool function with the given arguments.
         *
         * @param args the arguments to pass to the function
         * @return the result of the function execution
         * @throws Exception if the function execution fails
         */
        Object apply(Object... args) throws Exception;
    }

    // Tool metadata
    @NoArgsConstructor
    public static class ToolMeta {
        public String name;
        public String description;
        public ToolFunction function;
        public List<ParamMeta> params;

        public ToolMeta(String name, String description, ToolFunction function, List<ParamMeta> params) {
            this.name = name;
            this.description = description;
            this.function = function;
            this.params = params != null ? new ArrayList<>(params) : new ArrayList<>();
        }
    }

    // Parameter metadata
    public static class ParamMeta {
        public String name;
        public String nameType;
        public String description;
        public Object defaultValue;

        public ParamMeta(String name, String nameType, String description, Object defaultValue) {
            this.name = name;
            this.nameType = nameType;
            this.description = description;
            this.defaultValue = defaultValue;
        }
    }
}