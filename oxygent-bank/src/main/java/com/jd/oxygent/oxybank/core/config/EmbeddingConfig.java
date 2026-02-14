package com.jd.oxygent.oxybank.core.config;

import lombok.Data;

/**
 * Embedding model configuration.
 * type, apiKey, glmModelName, glmApiBase
 */
@Data
public class EmbeddingConfig {

    private String type = "glm";

    private String apiKey = "";

    private String glmModelName = "embedding-2";

    private String glmApiBase = "https://open.bigmodel.cn/api/paas/v4/embeddings";

    public EmbeddingConfig() {
        // Load from env EMBEDDING_* when needed
    }
}
