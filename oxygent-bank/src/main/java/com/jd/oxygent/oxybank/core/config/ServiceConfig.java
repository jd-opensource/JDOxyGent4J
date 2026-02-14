package com.jd.oxygent.oxybank.core.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import com.jd.oxygent.oxybank.core.model.embedding.EmbeddingFactory;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

/**
 * Configuration management for knowledge base platform.
 */
@Slf4j
public class ServiceConfig {

    private static final Path ROOT_DIR = Paths.get("").toAbsolutePath().getParent();

    private static final ServiceConfig INSTANCE = new ServiceConfig();

    private final EmbeddingConfig embeddingConfig;

    private final AnnotationConfig annotationConfig;

    private final String appName;

    private final String host;

    private final int port;

    private String apiBaseUrl;

    private RestHighLevelClient esClient;

    private Object vearchClient;

    private Object embeddingModel;

    private ServiceConfig() {
        this.appName = "OxyGentKnowledgeBank";
        this.host = "0.0.0.0";
        this.port = 8000;
        this.apiBaseUrl = null;
        this.embeddingConfig = new EmbeddingConfig();
        this.annotationConfig = new AnnotationConfig();
        this.initEmbeddingModel();
    }

    public static ServiceConfig getInstance() {
        return INSTANCE;
    }

    private void initEmbeddingModel() {
        try {
            String embeddingType = embeddingConfig.getType().toLowerCase();
            if ("glm".equals(embeddingType)) {
                // 对应 Python: EmbeddingFactory.create_embedding(...)
                this.embeddingModel = EmbeddingFactory.createEmbedding(
                        "glm",
                        embeddingConfig.getApiKey(),
                        Map.of()
//                        embeddingConfig.getGlmModelName(),
//                        embeddingConfig.getGlmApiBase()
                );
            } else {
                throw new IllegalArgumentException(
                        String.format(
                                """
                                Unsupported embedding type: %s. Supported type: glm
                                """,
                                embeddingType
                        )
                );
            }
        } catch (Exception e) {
            log.error("初始化向量模型失败", e);
        }
    }

    public RestHighLevelClient getEsClient() {
        return esClient;
    }

    public Object getVearchClient() {
        return vearchClient;
    }

    public Object getEmbeddingModel() {
        return embeddingModel;
    }

    public EmbeddingConfig getEmbeddingConfig() {
        return embeddingConfig;
    }

    public AnnotationConfig getAnnotationConfig() {
        return annotationConfig;
    }

    public String getAppName() {
        return appName;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    /**
     * Get API base URL, compute from host:port if not set.
     */
    public String getApiBaseUrl() {
        if (this.apiBaseUrl != null && !this.apiBaseUrl.isEmpty()) {
            return this.apiBaseUrl.replaceAll("/+$", "");
        }
        return String.format("http://%s:%d", this.host, this.port);
    }

    /**
     * Allow setting api_base_url from env config.
     */
    public void setApiBaseUrl(String value) {
        this.apiBaseUrl = value;
    }
}