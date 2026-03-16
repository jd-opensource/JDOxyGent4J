package com.jd.oxygent.core.oxygent.mcpservers.engine.metadata;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * Metadata class representing an MCP tool.
 * Contains information about the tool's name, description, parameters, and the method to invoke.
 */
public class ToolMetadata {
    private final String name;
    private final String description;
    private final String title;
    private final Method method;
    private final Object bean;
    private final List<ParameterMetadata> parameters;

    /**
     * Constructs a new ToolMetadata instance.
     *
     * @param annotation The @MCPTool annotation containing tool metadata
     * @param method The method to be invoked when the tool is called
     * @param bean The object instance containing the method
     */
    public ToolMetadata(MCPTool annotation, Method method, Object bean) {
        this.name = annotation.name();
        this.description = annotation.description();
        this.title = annotation.title().isEmpty() ? annotation.name() : annotation.title();
        this.method = method;
        this.bean = bean;
        this.parameters = extractParameters(method);
    }

    /**
     * Extracts parameter metadata from the method's parameters.
     * Uses @ToolParam annotations if present, otherwise uses default values.
     *
     * @param method The method to extract parameters from
     * @return List of ParameterMetadata objects
     */
    private List<ParameterMetadata> extractParameters(Method method) {
        List<ParameterMetadata> params = new ArrayList<>();
        Parameter[] parameters = method.getParameters();

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            ToolParam toolParam = param.getAnnotation(ToolParam.class);

            if (toolParam != null) {
                params.add(new ParameterMetadata(
                        param.getName(),
                        toolParam.description(),
                        toolParam.type(),
                        toolParam.required(),
                        toolParam.defaultValue(),
                        toolParam.enumValues(),
                        param.getType()
                ));
            } else {
                // If no annotation, use default values
                params.add(new ParameterMetadata(
                        param.getName(),
                        "Parameter " + param.getName(),
                        inferType(param.getType()),
                        true,
                        "",
                        "",
                        param.getType()
                ));
            }
        }

        return params;
    }

    /**
     * Infers the MCP parameter type from the Java class type.
     *
     * @param type The Java class type
     * @return The corresponding MCP type string ("string", "number", or "boolean")
     */
    private String inferType(Class<?> type) {
        if (type == String.class) return "string";
        if (type == Integer.class || type == int.class ||
                type == Long.class || type == long.class ||
                type == Double.class || type == double.class ||
                type == Float.class || type == float.class) return "number";
        if (type == Boolean.class || type == boolean.class) return "boolean";
        return "string";
    }

    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getTitle() { return title; }
    public Method getMethod() { return method; }
    public Object getBean() { return bean; }
    public List<ParameterMetadata> getParameters() { return parameters; }

    /**
     * Invokes the tool method with the provided arguments.
     *
     * @param args The arguments to pass to the method
     * @return The result of the method invocation
     * @throws Exception if the method invocation fails
     */
    public Object invoke(Object[] args) throws Exception {
        return method.invoke(bean, args);
    }
}