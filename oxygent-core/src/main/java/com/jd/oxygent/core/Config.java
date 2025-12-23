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

package com.jd.oxygent.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * OxyGent System Global Configuration Management Class
 * <p>
 * This class is responsible for managing all configuration items of the OxyGent system,
 * using a three-layer configuration mechanism:
 * 1. First load default values in Java programs
 * 2. Then read configuration data from config.json file
 * 3. Finally read configuration data from application.yml
 * The latter will override the same configuration items of the former.
 * <p>
 * Configuration modules included:
 * - AppConfig: Application basic configuration
 * - LlmConfig: Large Language Model configuration
 * - RedisConfig: Redis database configuration
 * - MessageConfig: Message processing configuration
 * - EsConfig: Elasticsearch configuration
 * - ServerConfig: Server configuration
 * - AgentConfig: Agent configuration
 * - FileConfig: File storage configuration
 * - VearchConfig: Vector database configuration
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "oxygent")
@Slf4j
public class Config {

    /**
     * Configuration file path, can be an external absolute path or a file under the classpath
     */
    private static String configFilePath = EnvUtils.getEnv("CONFIG_FILE_PATH","config.json");
    /**
     * Configuration file environment variable, can be default, dev, test, prod, etc.
     */
    private static String configFileEnv = EnvUtils.getEnv("CONFIG_FILE_ENV","default");

    private static AppConfig app = new AppConfig();
    private static LlmConfig llm = new LlmConfig();
    private static RedisConfig redis = new RedisConfig();
    private static MessageConfig message = new MessageConfig();
    private static EsConfig es = new EsConfig();
    private static EsSettingsConfig esSettings = new EsSettingsConfig();
    private static ServerConfig server = new ServerConfig();
    private static AgentConfig agent = new AgentConfig();
    private static FileConfig xfile = new FileConfig();
    private static VearchConfig vearch = new VearchConfig();

    static {
        loadConfigFile();
    }

    private static void loadConfigFile() {
        boolean initializeDefaultConfigs = false;
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = Config.class.getClassLoader();
            }
            JsonNode rootNode = null;
            // Try to load config.json from classpath and external paths, with the highest priority being the external path set by the user through the loadConfigPath() method
            try (BufferedReader reader = new BufferedReader(new FileReader(configFilePath))) {
                if (reader != null) {
                    rootNode = JsonUtils.getObjectMapper().readTree(reader);
                }
            } catch (Exception e) {
                try (InputStream inputStream = cl.getResourceAsStream(configFilePath)) {
                    if (inputStream != null) {
                        rootNode = JsonUtils.getObjectMapper().readTree(inputStream);
                    }
                } catch (Exception e2) {
                    // do nothing
                }
            }
            if (rootNode == null) {
                log.warn("Config.json file format error or not found, using default configuration");
                initializeDefaultConfigs = true;
            } else {
                JsonNode defaultConf = rootNode.get(configFileEnv);
                if (defaultConf == null) {
                    log.warn("Default configuration node not found in config.json file, using default configuration");
                    initializeDefaultConfigs = true;
                } else {
                    // Safely load each configuration item
                    loadConfigSafely(defaultConf, "app", AppConfig.class, () -> app = new AppConfig(),
                            config -> app = config);
                    loadConfigSafely(defaultConf, "llm", LlmConfig.class, () -> llm = new LlmConfig(),
                            config -> llm = config);
                    loadConfigSafely(defaultConf, "redis", RedisConfig.class, () -> redis = new RedisConfig(),
                            config -> redis = config);
                    loadConfigSafely(defaultConf, "message", MessageConfig.class, () -> message = new MessageConfig(),
                            config -> message = config);
                    loadConfigSafely(defaultConf, "es", EsConfig.class, () -> es = new EsConfig(),
                            config -> es = config);
                    loadConfigSafely(defaultConf, "es_settings", EsSettingsConfig.class, () -> esSettings = new EsSettingsConfig(),
                            config -> esSettings = config);
                    loadConfigSafely(defaultConf, "server", ServerConfig.class, () -> server = new ServerConfig(),
                            config -> server = config);
                    loadConfigSafely(defaultConf, "agent", AgentConfig.class, () -> agent = new AgentConfig(),
                            config -> agent = config);
                    loadConfigSafely(defaultConf, "xfile", FileConfig.class, () -> xfile = new FileConfig(),
                            config -> xfile = config);
                    loadConfigSafely(defaultConf, "vearch", VearchConfig.class, () -> vearch = new VearchConfig(),
                            config -> vearch = config);
                    log.info("Finished reading config.json, path:{} env:{}", configFilePath, configFileEnv);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to read config.json file, using default configuration", e);
            initializeDefaultConfigs = true;
        }
        if (initializeDefaultConfigs) { // Ensure default configuration is available even if exceptions occur
            initializeDefaultConfigs();
        }
    }

    /**
     * Initialize default configurations
     * <p>
     * Provide default instances for all configuration modules to ensure the system can run normally
     * when configuration files are missing or exceptions occur
     */
    private static void initializeDefaultConfigs() {
        app = Objects.requireNonNullElse(app, new AppConfig());
        llm = Objects.requireNonNullElse(llm, new LlmConfig());
        redis = Objects.requireNonNullElse(redis, new RedisConfig());
        message = Objects.requireNonNullElse(message, new MessageConfig());
        es = Objects.requireNonNullElse(es, new EsConfig());
        server = Objects.requireNonNullElse(server, new ServerConfig());
        agent = Objects.requireNonNullElse(agent, new AgentConfig());
        xfile = Objects.requireNonNullElse(xfile, new FileConfig());
        vearch = Objects.requireNonNullElse(vearch, new VearchConfig());
    }

    public static void loadConfigPath(String configFilePath, String configFileEnv) {
        if (configFilePath != null) {
            Config.configFilePath = configFilePath;
        }
        if (configFileEnv != null) {
            Config.configFileEnv = configFileEnv;
        }
        loadConfigFile();
    }

    /**
     * Safely load configuration items
     */
    @SuppressWarnings("unchecked")
    private static <T> void loadConfigSafely(JsonNode defaultConf, String key, Class<T> clazz,
                                             Runnable defaultAction, java.util.function.Consumer<T> setter) {
        try {
            JsonNode configObj = defaultConf != null ? defaultConf.get(key) : null;
            if (configObj != null && !configObj.isNull()) {
                ObjectMapper objectMapper = JsonUtils.getObjectMapper();
                objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
                T config = objectMapper.treeToValue(configObj, clazz);
                if (config != null) {
                    setter.accept(config);
                } else {
                    log.warn("Failed to parse {} configuration, using default configuration", key);
                    defaultAction.run();
                }
            } else {
                defaultAction.run();
            }
        } catch (Exception e) {
            log.error("Exception occurred while loading {} configuration, using default configuration", key, e);
            defaultAction.run();
        }
    }

    /**
     * Agent Configuration Class
     * <p>
     * Contains basic configuration information required for agent operation, such as prompts, LLM models, input schemas, and memory management
     */
    @Data
    public static class AgentConfig {
        private String prompt = "";
        private String llmModel = "default_llm";
        private Map<String, Object> inputSchema = Map.of(
                "properties", Map.of(
                        "query", Map.of("description", "Query question")
                ),
                "required", List.of("query")
        );
        private int maxMemoryRounds = 5;
    }

    /**
     * Application Basic Configuration Class
     * <p>
     * Contains basic application information such as application name, version number, business type, and component scan paths
     */
    @Data
    public static class AppConfig {
        private String name = "app";
        private String version = "1.0.0";
        private String bizType = "oxygent";
        private String scanOxygentPath = "com.jd.oxygent";
    }

    @Data
    public static class EsConfig {
        private String type = "local";
        private List<String> hosts = new ArrayList();
        private String user = "";
        private String password = "";
    }

    @Data
    public static class EsSettingsConfig {
        private Integer numberOfShards;
        private Integer numberOfReplicas;
    }

    @Data
    public static class FileConfig {
        private String saveDir = "./cache_dir";
    }

    /**
     * Large Language Model Configuration Class
     * <p>
     * Contains connection configuration for LLM services, such as API endpoints, keys, model parameters, and generation configurations
     */
    @Data
    public static class LlmConfig {
        private String cls = "com.jd.oxygent.core.oxygent.oxy.llms.HttpLLm";
        private String baseUrl = "http://localhost:11434";
        private String apiKey = "YOUR-API-KEY";
        private String modelName = "gpt-4.1";
        private double temperature = 0.1;
        private int maxTokens = 4096;
        private double topP = 1.0;
    }

    @Data
    public static class MessageConfig {
        private boolean isSendToolCall = true;
        private boolean isDetailedToolCall = true;
        private boolean isSendObservation = true;
        private boolean isDetailedObservation = true;
        private boolean isSendThink = true;
        private boolean isSendAnswer = true;
        private boolean isStored = true;
        private boolean isShowInTerminal = false;
        private boolean isSendFullArguments = false;
    }

    @Data
    public static class RedisConfig {
        private String host = "local";
        private int port = 6379;
        private int database = 0;
        private String password = "";
        private int timeout = 5000; // ms
    }

    @Data
    public static class ServerConfig {
        private String host = "127.0.0.1";
        private int port = 8080;
        private boolean autoOpenWebpage = true;
        private String logLevel = "INFO";
        private String firstQuery = "What time is it now?";
        private String welcomeMessage = "Hi, I’m OxyGent. How can I assist you?";
    }

    @Data
    public static class VearchConfig {
        private boolean enabled = false;
    }

    public static String getAppName() {
        return app.getName();
    }

    public static String getBizType() {
        return app.getBizType();
    }

    public static AppConfig getApp() {
        return app;
    }

    public void setApp(AppConfig app) {
        Config.app = app;
    }

    public static LlmConfig getLlm() {
        return llm;
    }

    public void setLlm(LlmConfig llm) {
        Config.llm = llm;
    }

    public static RedisConfig getRedis() {
        return redis;
    }

    public void setRedis(RedisConfig redis) {
        Config.redis = redis;
    }

    public static MessageConfig getMessage() {
        return message;
    }

    public void setMessage(MessageConfig message) {
        Config.message = message;
    }

    public static EsConfig getEs() {
        return es;
    }

    public void setEs(EsConfig es) {
        Config.es = es;
    }

    public static EsSettingsConfig getEsSettings() {
        return esSettings;
    }

    public static void setEsSettings(EsSettingsConfig esSettings) {
        Config.esSettings = esSettings;
    }

    public static ServerConfig getServer() {
        return server;
    }

    public void setServer(ServerConfig server) {
        Config.server = server;
    }

    public static AgentConfig getAgent() {
        return agent;
    }

    public void setAgent(AgentConfig agent) {
        Config.agent = agent;
    }

    public static FileConfig getXfile() {
        return xfile;
    }

    public void setXfile(FileConfig xfile) {
        Config.xfile = xfile;
    }

    public static VearchConfig getVearch() {
        return vearch;
    }

    public void setVearch(VearchConfig vearch) {
        Config.vearch = vearch;
    }

    public static String getConfigFileEnv() {
        return configFileEnv;
    }
}
