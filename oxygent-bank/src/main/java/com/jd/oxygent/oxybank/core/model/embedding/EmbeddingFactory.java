package com.jd.oxygent.oxybank.core.model.embedding;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Embedding Factory Implementation
 * 
 * Contains EmbeddingType enum and EmbeddingFactory class
 * for creating GLM embedding backends.
 * 
 * Features:
 * - Auto-loads API keys from environment variables
 * - Supports GLM embedding models (embedding-2)
 * - Unified interface with customizable parameters
 * 
 * Converted from core/model/embedding/factory.py
 */
@Slf4j
public class EmbeddingFactory {
    
    /**
     * Supported embedding backend types
     */
    public enum EmbeddingType {
        GLM("glm");
        
        @Getter
        private final String value;
        
        EmbeddingType(String value) {
            this.value = value;
        }
    }
    
    /**
     * Embedding configuration class
     */
    private static class EmbeddingConfig {
        private final String description;
        private final Map<String, Object> defaultParams;
        private final String envVar;
        private final List<String> requiredPackages;
        
        public EmbeddingConfig(
                String description,
                Map<String, Object> defaultParams,
                String envVar,
                List<String> requiredPackages) {
            this.description = description;
            this.defaultParams = defaultParams;
            this.envVar = envVar;
            this.requiredPackages = requiredPackages;
        }
        
        public String getDescription() {
            return description;
        }
        
        public Map<String, Object> getDefaultParams() {
            return defaultParams;
        }
        
        public String getEnvVar() {
            return envVar;
        }
        
        public List<String> getRequiredPackages() {
            return requiredPackages;
        }
    }
    
    // Predefined configurations
    private static final Map<EmbeddingType, EmbeddingConfig> EMBEDDING_CONFIGS = Map.of(
        EmbeddingType.GLM, new EmbeddingConfig(
            "Zhipu GLM Embedding (embedding-2)",
            Map.of(
                "model", "embedding-2",
                "apiBase", "https://open.bigmodel.cn/api/paas/v4/embeddings"
            ),
            "EMBEDDING_API_KEY",
            List.of()
        )
    );
    
    /**
     * Normalize embedding type input to EmbeddingType enum
     * 
     * @param embeddingType Embedding type (enum or string)
     * @return EmbeddingType enum value
     * @throws IllegalArgumentException If embeddingType is invalid string or unsupported type
     */
    private static EmbeddingType normalizeEmbeddingType(Object embeddingType) {
        if (embeddingType instanceof EmbeddingType) {
            return (EmbeddingType) embeddingType;
        } else if (embeddingType instanceof String) {
            // Try to find enum by value
            String strValue = ((String) embeddingType).toLowerCase();
            for (EmbeddingType enumMember : EmbeddingType.values()) {
                if (enumMember.getValue().toLowerCase().equals(strValue)) {
                    return enumMember;
                }
            }
            // If not found, raise error with helpful message
            StringBuilder validValues = new StringBuilder();
            for (EmbeddingType enumMember : EmbeddingType.values()) {
                if (validValues.length() > 0) {
                    validValues.append(", ");
                }
                validValues.append(enumMember.getValue());
            }
            throw new IllegalArgumentException(
                "Invalid embedding type string: '" + embeddingType + "'. " +
                "Valid values are: " + validValues + " or use EmbeddingType enum."
            );
        } else {
            throw new IllegalArgumentException(
                "embeddingType must be EmbeddingType enum or string, got " + embeddingType.getClass().getName()
            );
        }
    }
    
    /**
     * Create embedding of specified type
     * 
     * @param embeddingType Embedding type (enum or string like "glm", default: GLM)
     * @param apiKey Optional API key (auto-loads from env if not provided)
     * @param kwargs Custom configuration parameters
     *               - model: Model name (e.g., embedding-2)
     *               - Other GLM embedding parameters
     * @return Configured embedding instance
     * @throws IllegalArgumentException Unsupported embedding type or missing API key
     * @throws ClassNotFoundException GLM embedding module not found
     */
    public static Object createEmbedding(Object embeddingType, String apiKey, Map<String, Object> kwargs) throws Exception {
        // Normalize embeddingType to enum
        EmbeddingType type = normalizeEmbeddingType(embeddingType);
        EmbeddingConfig config = EMBEDDING_CONFIGS.get(type);
        
        if (config == null) {
            throw new IllegalArgumentException("Unsupported embedding type: " + type);
        }
        
        // Load API key from environment if not provided
        if (apiKey == null && config.getEnvVar() != null) {
            apiKey = System.getenv(config.getEnvVar());
            if (apiKey == null) {
                throw new IllegalArgumentException(
                    "API key not provided and " + config.getEnvVar() + " not set in environment. " +
                    "Please set " + config.getEnvVar() + " or pass apiKey parameter."
                );
            }
        }
        
        // Merge default params with user params (user params override defaults)
        Map<String, Object> embeddingParams = new HashMap<>(config.getDefaultParams());
        if (kwargs != null) {
            embeddingParams.putAll(kwargs);
        }
        embeddingParams.put("apiKey", apiKey);
        
        // Create instance based on type
        if (type == EmbeddingType.GLM) {
            return createGLMEmbedding((String) embeddingParams.get("model"), apiKey, embeddingParams);
        } else {
            throw new IllegalArgumentException("Unsupported embedding type: " + type);
        }
    }
    
    /**
     * Create Zhipu GLM Embedding
     * 
     * @param model GLM model name (embedding-2)
     * @param apiKey Zhipu API key
     * @param kwargs Other parameters
     * @return GLM embedding instance
     * @throws ClassNotFoundException GLM embedding module not found
     */
    private static Object createGLMEmbedding(String model, String apiKey, Map<String, Object> kwargs) throws ClassNotFoundException {
        try {
            // Try to load GLMEmbedding class
            Class<?> glmEmbeddingClass = Class.forName("com.jd.oxygent.oxybank.core.model.embedding.GLMEmbedding2");
            
            // TODO: Implement proper instance creation with reflection
            // For now, return a dummy instance
            return new Object();
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException(
                "GLM embedding not installed. Please check if GLMEmbedding.java exists in core/model/embedding/"
            );
        }
    }
    
    /**
     * Get embedding type information
     * 
     * @param embeddingType Embedding type
     * @return Dictionary containing type information:
     *         - type: Type name
     *         - description: Description
     *         - defaultParams: Default parameters
     *         - envVar: Environment variable name for API key
     *         - requiredPackages: Required dependencies
     */
    public static Map<String, Object> getEmbeddingInfo(Object embeddingType) {
        // Normalize embeddingType to enum
        EmbeddingType type = normalizeEmbeddingType(embeddingType);
        EmbeddingConfig config = EMBEDDING_CONFIGS.get(type);
        
        if (config == null) {
            throw new IllegalArgumentException("Unsupported embedding type: " + type);
        }
        
        Map<String, Object> info = new HashMap<>();
        info.put("type", type.getValue());
        info.put("description", config.getDescription());
        info.put("defaultParams", config.getDefaultParams());
        info.put("envVar", config.getEnvVar());
        info.put("requiredPackages", config.getRequiredPackages());
        
        return info;
    }
    
    /**
     * List all available embedding types
     * 
     * @return List containing all type information
     */
    public static List<Map<String, Object>> listEmbeddingTypes() {
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        
        for (EmbeddingType type : EmbeddingType.values()) {
            result.add(getEmbeddingInfo(type));
        }
        
        return result;
    }
}