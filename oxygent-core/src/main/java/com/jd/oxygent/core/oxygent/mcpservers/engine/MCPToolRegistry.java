package com.jd.oxygent.core.oxygent.mcpservers.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.core.oxygent.mcpservers.engine.metadata.ParameterMetadata;
import com.jd.oxygent.core.oxygent.mcpservers.engine.metadata.ToolMetadata;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
@Slf4j
public class MCPToolRegistry {

    private final Map<String, ToolMetadata> tools = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Register single tool
     */
    public void registerTool(ToolMetadata toolMetadata) {
        tools.put(toolMetadata.getName(), toolMetadata);
        log.info("[MCPToolRegistry] Registered tool: {}", toolMetadata.getName());
    }

    /**
     * Register multiple tools
     */
    public void registerTools(List<ToolMetadata> toolMetadatas) {
        for (ToolMetadata toolMetadata : toolMetadatas) {
            registerTool(toolMetadata);
        }
    }

    /**
     * Register tools to MCP server
     */
    public void registerToMCPServer(McpSyncServer server) {
        for (ToolMetadata toolMetadata : tools.values()) {
            McpSchema.Tool mcpTool = buildMcpTool(toolMetadata);
            McpServerFeatures.SyncToolSpecification toolSpec = createToolSpecification(toolMetadata, mcpTool);
            server.addTool(toolSpec);
            log.info("[MCPToolRegistry] Added tool to MCP server: {}", toolMetadata.getName());
        }

        log.info("[MCPToolRegistry] Total {} tools registered to MCP server", tools.size());
    }

    private McpSchema.Tool buildMcpTool(ToolMetadata toolMetadata) {
        // Build JSON Schema
        String jsonSchema = buildJsonSchema(toolMetadata);

        return McpSchema.Tool.builder()
                .name(toolMetadata.getName())
                .title(toolMetadata.getTitle())
                .description(toolMetadata.getDescription())
                .inputSchema(new JacksonMcpJsonMapper(objectMapper), jsonSchema)
                .build();
    }

    private String buildJsonSchema(ToolMetadata toolMetadata) {
        StringBuilder schema = new StringBuilder();
        schema.append("{\n");
        schema.append("  \"type\": \"object\",\n");
        schema.append("  \"properties\": {\n");

        List<ParameterMetadata> params = toolMetadata.getParameters();
        for (int i = 0; i < params.size(); i++) {
            ParameterMetadata param = params.get(i);
            schema.append("    \"").append(param.getName()).append("\": {\n");
            schema.append("      \"type\": \"").append(param.getType()).append("\",\n");
            schema.append("      \"description\": \"").append(escapeJson(param.getDescription())).append("\"");

            // Add enum values
            if (!param.getEnumValues().isEmpty()) {
                schema.append(",\n      \"enum\": ").append(param.getEnumValues());
            }

            schema.append("\n    }");
            if (i < params.size() - 1) {
                schema.append(",");
            }
            schema.append("\n");
        }

        schema.append("  },\n");

        // Add required fields
        List<String> requiredParams = new ArrayList<>();
        for (ParameterMetadata param : params) {
            if (param.isRequired()) {
                requiredParams.add("\"" + param.getName() + "\"");
            }
        }

        if (!requiredParams.isEmpty()) {
            schema.append("  \"required\": [").append(String.join(", ", requiredParams)).append("]\n");
        } else {
            schema.append("  \"required\": []\n");
        }

        schema.append("}");
        return schema.toString();
    }

    private String escapeJson(String text) {
        return text.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private McpServerFeatures.SyncToolSpecification createToolSpecification(
            ToolMetadata toolMetadata, McpSchema.Tool mcpTool) {

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(mcpTool)
                .callHandler((exchange, callToolRequest) -> {
                    try {
                        // Extract parameters
                        Map<String, Object> arguments = callToolRequest.arguments();

                        // Convert parameter types
                        Object[] methodArgs = convertArguments(arguments, toolMetadata);

                        // Call method
                        Object result = toolMetadata.invoke(methodArgs);

                        // Return result
                        return new McpSchema.CallToolResult(
                                Collections.singletonList(new McpSchema.TextContent(result.toString())),
                                false
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                        return new McpSchema.CallToolResult(
                                Collections.singletonList(new McpSchema.TextContent("Error: " + e.getMessage())),
                                true
                        );
                    }
                })
                .build();
    }

    private Object[] convertArguments(Map<String, Object> arguments, ToolMetadata toolMetadata) {
        List<ParameterMetadata> params = toolMetadata.getParameters();
        Object[] methodArgs = new Object[params.size()];

        for (int i = 0; i < params.size(); i++) {
            ParameterMetadata param = params.get(i);
            String paramName = param.getName();
            Object value = arguments.get(paramName);

            // If parameter doesn't exist, use default value
            if (value == null && !param.getDefaultValue().isEmpty()) {
                value = parseDefaultValue(param.getDefaultValue(), param.getJavaType());
            }

            // Type conversion
            methodArgs[i] = convertValue(value, param.getJavaType());
        }

        return methodArgs;
    }

    private Object parseDefaultValue(String defaultValue, Class<?> targetType) {
        try {
            if (targetType == String.class) return defaultValue;
            if (targetType == Integer.class || targetType == int.class)
                return Integer.parseInt(defaultValue);
            if (targetType == Double.class || targetType == double.class)
                return Double.parseDouble(defaultValue);
            if (targetType == Boolean.class || targetType == boolean.class)
                return Boolean.parseBoolean(defaultValue);
            if (targetType == Float.class || targetType == float.class)
                return Float.parseFloat(defaultValue);
            if (targetType == Long.class || targetType == long.class)
                return Long.parseLong(defaultValue);
            return defaultValue;
        } catch (Exception e) {
            return null;
        }
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;

        if (targetType.isInstance(value)) {
            return value;
        }

        String stringValue = value.toString();
        try {
            if (targetType == String.class) return stringValue;
            if (targetType == Integer.class || targetType == int.class)
                return Integer.parseInt(stringValue);
            if (targetType == Double.class || targetType == double.class)
                return Double.parseDouble(stringValue);
            if (targetType == Float.class || targetType == float.class)
                return Float.parseFloat(stringValue);
            if (targetType == Long.class || targetType == long.class)
                return Long.parseLong(stringValue);
            if (targetType == Boolean.class || targetType == boolean.class)
                return Boolean.parseBoolean(stringValue);
        } catch (NumberFormatException e) {
            // 类型转换失败，返回原始值
        }

        return value;
    }

    /**
     * Get all registered tools
     */
    public Collection<ToolMetadata> getAllTools() {
        return tools.values();
    }

    /**
     * Get specific tool
     */
    public ToolMetadata getTool(String name) {
        return tools.get(name);
    }

    /**
     * Get tool count
     */
    public int getToolCount() {
        return tools.size();
    }
}