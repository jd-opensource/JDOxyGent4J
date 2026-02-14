package com.jd.oxygent.oxybank.core.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.jd.oxygent.oxybank.core.model.embedding.EmbeddingFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.util.StringUtils;

/**
 * Configuration management for knowledge base platform.
 */
@Slf4j
public class ServiceConfig {

    private static final Path ROOT_DIR = Paths.get("").toAbsolutePath().getParent();

    private static final ServiceConfig INSTANCE = new ServiceConfig();

    private final ElasticsearchConfig esConfig;

    private final VearchConfig vearchConfig;

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
        // fixme: 从 .env 或 Spring 配置中加载配置
        this.appName = "OxyGentKnowledgeBank";
        this.host = "0.0.0.0";
        this.port = 8000;
        this.apiBaseUrl = null;
        this.esConfig = new ElasticsearchConfig();
        this.vearchConfig = new VearchConfig();
        this.embeddingConfig = new EmbeddingConfig();
        this.annotationConfig = new AnnotationConfig();
        this.initEsClient();
        this.initVearchClient();
        this.initEmbeddingModel();
    }

    public static ServiceConfig getInstance() {
        return INSTANCE;
    }

    private void initEsClient() {
        try {
            List<String> hosts = esConfig.getHosts();
            List<HttpHost> hostLists = new ArrayList<>();
//            String[] hostList = esProperties.getClusterNodes().split(";");
//            for (String addr : hostList) {
//                if (StringUtils.isEmpty(addr)) {
//                    continue;
//                }
//                String[] addrDetail = addr.split(":");
//                String host = addrDetail[0];
//                String port = addrDetail[1];
//                hostLists.add(new HttpHost(host, Integer.parseInt(port), esProperties.getSchema()));
//            }
            HttpHost[] httpHost = hostLists.toArray(new HttpHost[]{});
            // Build connection object
            RestClientBuilder builder = RestClient.builder(httpHost);
            this.esClient = new RestHighLevelClient(builder);
        } catch (Exception e) {
            log.error("初始化 Elasticsearch 客户端失败", e);
            // fixme: 根据需要决定是否抛出运行时异常
        }
    }

    private void initVearchClient() {
        try {
            // fixme: 根据 vearchConfig 构造 Vearch Config 和客户端，等价于 Python 中 Vearch(config)
            this.vearchClient = null;
        } catch (Exception e) {
            log.error("初始化 Vearch 客户端失败", e);
        }
    }

    private void initEmbeddingModel() {
        try {
            String embedding_type = embeddingConfig.getType().toLowerCase();
            if ("glm".equals(embedding_type)) {
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
                                embedding_type
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

    public ElasticsearchConfig getEsConfig() {
        return esConfig;
    }

    public VearchConfig getVearchConfig() {
        return vearchConfig;
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

    @Data
    public static class ElasticsearchConfig {

        /**
         * Corresponds to Python:
         * hosts: Union[List[str], str] = ["localhost:9200"]
         * username: str = ""
         * password: str = ""
         */
        private List<String> hosts = List.of("localhost:9200");

        private String username = "";

        private String password = "";

        public ElasticsearchConfig() {
            // fixme: 从环境变量 ELASTICSEARCH_* 中加载配置
        }
    }

    @Data
    public static class VearchConfig {

        /**
         * host: str = ""
         * token: str | None = None
         * dbName: str = ""
         * vectorDimension: int = 1024
         */
        private String host = "";

        private String token;

        private String dbName = "";

        private int vectorDimension = 1024;

        public VearchConfig() {
            // fixme: 从环境变量 VEARCH_* 中加载配置
        }
    }

    /**
     * Embedding model configuration class.
     */
    @Data
    public static class EmbeddingConfig {

        /**
         * type: str = "glm"  # Default type: glm
         * apiKey: str = ""
         * glmModelName: str = "embedding-2"
         * glmApiBase: str = "https://open.bigmodel.cn/api/paas/v4/embeddings"
         */
        private String type = "glm";

        private String apiKey = "";

        private String glmModelName = "embedding-2";

        private String glmApiBase = "https://open.bigmodel.cn/api/paas/v4/embeddings";

        public EmbeddingConfig() {
            // fixme: 从环境变量 EMBEDDING_* 中加载配置
        }
    }

    /**
     * Annotation platform configuration class
     *
     * The annotation platform is responsible for collecting, annotating, approving, and injecting QA data into knowledge bases.
     *
     * Knowledge base structured field description:
     * ===========================================
     * When annotation data is injected into a knowledge base, a knowledge base chunk containing the following structured fields will be created:
     *
     * All fields (defined by knowledge base schema):
     * - question: str              # Question/input content
     * - answer: str                # Answer/output content
     * - caller: str                # Caller (e.g., user, agent_name)
     * - callee: str                # Callee (e.g., agent_name, tool_name)
     * - score: float               # Quality score (0-1)
     * - remark: str                # Remarks
     * - source_trace_id: str       # Original trace_id (for trace tracking)
     * - source_request_id: str     # Original request_id (for request tracking)
     * - data_type: str             # Data type (e2e/agent/llm/tool/custom)
     * - priority: int              # Priority (0-4, P0=0)
     * - category: str              # Data category
     *
     * Notes:
     * 1. The target knowledge base must support these structured fields
     * 2. If the knowledge base schema does not support certain fields, injection will fail
     * 3. Please ensure the target knowledge base schema includes all the above fields
     */
    @Data
    public static class AnnotationConfig {

        /**
         * esIndexPrefix: str = "qa_annotation"
         */
        private String esIndexPrefix = "qa_annotation";

        /**
         * kbEnabled: bool = True
         * kbId: str = ""
         */
        private boolean kbEnabled = true;

        private String kbId = "";

        /**
         * kbAutoIngest: bool = False
         * kbTimeout: int = 30
         * kbRetryTimes: int = 3
         * kbRetryInterval: int = 5
         */
        private boolean kbAutoIngest = false;

        private int kbTimeout = 30;

        private int kbRetryTimes = 3;

        private int kbRetryInterval = 5;

        /**
         * batchSize: int = 100
         */
        private int batchSize = 100;

        /**
         * defaultDataType: str = "custom"
         */
        private String defaultDataType = "custom";

        /**
         * defaultPriority: int = 4
         */
        private int defaultPriority = 4;

        public AnnotationConfig() {
            // fixme: 从环境变量 ANNOTATION_* 中加载配置
        }

        public void validate() {
            if (batchSize < 0) {
                throw new IllegalArgumentException("Must be a positive integer");
            }
            if (kbTimeout < 0 || kbRetryTimes < 0 || kbRetryInterval < 0) {
                throw new IllegalArgumentException("Must be a positive integer");
            }
            List<String> valid_types = List.of("e2e", "agent", "llm", "tool", "custom");
            if (!valid_types.contains(defaultDataType)) {
                throw new IllegalArgumentException(
                        String.format(
                                """
                                Invalid data type, must be one of: %s
                                """,
                                String.join(", ", valid_types)
                        )
                );
            }
            if (defaultPriority < 0 || defaultPriority > 4) {
                throw new IllegalArgumentException("Priority must be between 0 and 4");
            }
            if (kbEnabled && (kbId == null || kbId.trim().isEmpty())) {
                String msg = """
                        KB injection functionality is enabled (ANNOTATION_KB_ENABLED=true), \
                        but knowledge base ID is not configured.
                        Please configure in .env file: ANNOTATION_KB_ID=<your-knowledge-base-id>
                        Note: The target knowledge base schema must support all the following structured fields:
                          - question (Question)
                          - answer (Answer)
                          - caller (Caller)
                          - callee (Callee)
                          - score (Score)
                          - remark (Remarks)
                          - source_trace_id (Trace ID)
                          - source_request_id (Request ID)
                          - data_type (Data Type)
                          - priority (Priority)
                          - category (Category)
                        """;
                throw new IllegalArgumentException(msg);
            }
        }
    }
}