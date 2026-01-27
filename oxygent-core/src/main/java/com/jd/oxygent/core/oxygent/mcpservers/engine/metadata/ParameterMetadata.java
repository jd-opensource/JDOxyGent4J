package com.jd.oxygent.core.oxygent.mcpservers.engine.metadata;

/**
 * Metadata class representing a parameter for MCP tools.
 * Contains information about parameter name, description, type, requirements, and constraints.
 */
public class ParameterMetadata {
    private final String name;
    private final String description;
    private final String type;
    private final boolean required;
    private final String defaultValue;
    private final String enumValues;
    private final Class<?> javaType;

    /**
     * Constructs a new ParameterMetadata instance.
     *
     * @param name The parameter name
     * @param description The parameter description
     * @param type The parameter type (e.g., "string", "number", "boolean")
     * @param required Whether the parameter is required
     * @param defaultValue The default value as a JSON string
     * @param enumValues The enum values as a JSON array string
     * @param javaType The Java type of the parameter
     */
    public ParameterMetadata(String name, String description, String type,
                             boolean required, String defaultValue,
                             String enumValues, Class<?> javaType) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.required = required;
        this.defaultValue = defaultValue;
        this.enumValues = enumValues;
        this.javaType = javaType;
    }

    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public boolean isRequired() { return required; }
    public String getDefaultValue() { return defaultValue; }
    public String getEnumValues() { return enumValues; }
    public Class<?> getJavaType() { return javaType; }
}