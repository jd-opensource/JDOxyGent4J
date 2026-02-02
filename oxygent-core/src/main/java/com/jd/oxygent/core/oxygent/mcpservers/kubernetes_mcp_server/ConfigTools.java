package com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kubernetes MCP Server - config toolset
 * <p>
 * Provides read-only kubeconfig inspection capabilities:
 * - configuration_contexts_list: List all contexts and corresponding servers, marking the current context
 * - configuration_view: Return kubeconfig content (defaults to minimized fragments related to the current context)
 */
public class ConfigTools {

    /**
     * Return the first existing kubeconfig path:
     * - Prioritize environment variable KUBECONFIG (can contain multiple paths, separated by system path separator)
     * - Otherwise default to ~/.kube/config
     */
    private static String firstExistingKubeconfig() {
        String envPaths = System.getenv("KUBECONFIG");
        List<String> candidates = new ArrayList<>();

        if (envPaths != null && !envPaths.trim().isEmpty()) {
            String[] paths = envPaths.split(File.pathSeparator);
            for (String path : paths) {
                if (path != null && !path.trim().isEmpty()) {
                    candidates.add(expandUser(path.trim()));
                }
            }
        } else {
            candidates.add(expandUser("~/.kube/config"));
        }

        for (String path : candidates) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                return path;
            }
        }
        return null;
    }

    /**
     * Expand user home directory path
     */
    private static String expandUser(String path) {
        if (path == null) {
            return null;
        }
        if (path.startsWith("~")) {
            String homeDir = System.getProperty("user.home");
            return homeDir + path.substring(1);
        }
        return path;
    }

    /**
     * Read and parse kubeconfig, prefer YAML parsing; attempt JSON if failed.
     * Return (configuration dictionary, actual file path used); return (null, null) if not found.
     */
    private static Map<String, Object> loadKubeconfigContent() {
        String path = firstExistingKubeconfig();
        if (path == null) {
            return null;
        }

        try {
            String rawContent = Files.readString(new File(path).toPath());
            
            // Prefer YAML parsing
            try {
                return YamlUtils.parseYaml(rawContent);
            } catch (Exception e) {
                // Fall back to JSON parsing
                try {
                    return JsonUtils.parseJson(rawContent);
                } catch (Exception ex) {
                    return null;
                }
            }
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Only retain contexts/clusters/users fragments related to current-context.
     * Return original configuration if current-context is not configured.
     */
    private static Map<String, Object> minifyKubeconfig(Map<String, Object> cfg) {
        if (cfg == null) {
            return null;
        }

        Object currentContextObj = cfg.get("current-context");
        if (currentContextObj == null) {
            return cfg;
        }
        String currentContext = currentContextObj.toString();

        List<Map<String, Object>> contexts = getListFromMap(cfg, "contexts");
        List<Map<String, Object>> clusters = getListFromMap(cfg, "clusters");
        List<Map<String, Object>> users = getListFromMap(cfg, "users");

        Map<String, Object> currentContextMap = null;
        for (Map<String, Object> context : contexts) {
            if (currentContext.equals(context.get("name"))) {
                currentContextMap = context;
                break;
            }
        }

        if (currentContextMap == null) {
            return cfg;
        }

        Map<String, Object> contextConfig = (Map<String, Object>) currentContextMap.get("context");
        if (contextConfig == null) {
            return cfg;
        }

        String clusterName = contextConfig.get("cluster") != null ? contextConfig.get("cluster").toString() : null;
        String userName = contextConfig.get("user") != null ? contextConfig.get("user").toString() : null;

        Map<String, Object> minified = new HashMap<>();
        minified.put("apiVersion", cfg.get("apiVersion"));
        minified.put("kind", cfg.getOrDefault("kind", "Config"));
        minified.put("current-context", currentContext);

        List<Map<String, Object>> filteredContexts = new ArrayList<>();
        filteredContexts.add(currentContextMap);
        minified.put("contexts", filteredContexts);

        if (clusterName != null) {
            List<Map<String, Object>> filteredClusters = new ArrayList<>();
            for (Map<String, Object> cluster : clusters) {
                if (clusterName.equals(cluster.get("name"))) {
                    filteredClusters.add(cluster);
                }
            }
            minified.put("clusters", filteredClusters);
        }

        if (userName != null) {
            List<Map<String, Object>> filteredUsers = new ArrayList<>();
            for (Map<String, Object> user : users) {
                if (userName.equals(user.get("name"))) {
                    filteredUsers.add(user);
                }
            }
            minified.put("users", filteredUsers);
        }

        return minified;
    }

    /**
     * Get List from Map, return empty list if it does not exist
     */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> getListFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return new ArrayList<>();
    }

    /**
     * List all available context names and associated server URLs
     *     Output example:
     *     [
     *       {"name":"minikube","cluster":"minikube","server":"https://127.0.0.1:6443","current":true},
     *       {"name":"prod","cluster":"prod-cluster","server":"https://prod.example:6443","current":false}
     *     ]
     */
    @MCPTool(name = "configuration_contexts_list",
            description = "List all available context names and associated server urls from the kubeconfig file")
    public static List<Map<String, Object>> configurationContextsList() {
        Map<String, Object> cfg = loadKubeconfigContent();
        if (cfg == null) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> contexts = getListFromMap(cfg, "contexts");
        List<Map<String, Object>> clusters = getListFromMap(cfg, "clusters");
        Object currentContextObj = cfg.get("current-context");
        String currentContext = currentContextObj != null ? currentContextObj.toString() : null;

        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> context : contexts) {
            Map<String, Object> contextInfo = new HashMap<>();
            String name = context.get("name") != null ? context.get("name").toString() : null;
            contextInfo.put("name", name);

            Map<String, Object> contextConfig = (Map<String, Object>) context.get("context");
            if (contextConfig != null) {
                String clusterName = contextConfig.get("cluster") != null ? contextConfig.get("cluster").toString() : null;
                contextInfo.put("cluster", clusterName);

                if (clusterName != null) {
                    for (Map<String, Object> cluster : clusters) {
                        if (clusterName.equals(cluster.get("name"))) {
                            Map<String, Object> clusterConfig = (Map<String, Object>) cluster.get("cluster");
                            if (clusterConfig != null) {
                                String server = clusterConfig.get("server") != null ? clusterConfig.get("server").toString() : null;
                                contextInfo.put("server", server);
                            }
                            break;
                        }
                    }
                }
            }

            contextInfo.put("current", name != null && name.equals(currentContext));
            result.add(contextInfo);
        }

        return result;
    }

    /**
     * Get current Kubernetes configuration content
     * Return YAML string; return JSON string when PyYAML is not installed or parsing fails.
     */
    @MCPTool(name = "configuration_view",
            description = "Get the current Kubernetes configuration content as a kubeconfig YAML")
    public static String configurationView(
            @ToolParam(description = "Return a minified version (only current-context and related pieces) if True")
            boolean minified) {
        Map<String, Object> cfg = loadKubeconfigContent();
        if (cfg == null) {
            return "# kubeconfig not found. Please set KUBECONFIG or create ~/.kube/config";
        }

        Map<String, Object> out = minified ? minifyKubeconfig(cfg) : cfg;

        // Attempt YAML serialization
        try {
            return YamlUtils.toYaml(out);
        } catch (Exception e) {
            // Fall back to JSON
            try {
                return JsonUtils.toJson(out);
            } catch (Exception ex) {
                return "# Failed to serialize kubeconfig";
            }
        }
    }

    /**
     * YAML utility class
     */
    private static class YamlUtils {
        private static final org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();

        /**
         * Parse YAML string to Map
         */
        public static Map<String, Object> parseYaml(String yamlString) throws Exception {
            return yaml.load(yamlString);
        }

        /**
         * Serialize Map to YAML string
         */
        public static String toYaml(Map<String, Object> map) throws Exception {
            return yaml.dump(map);
        }
    }

    /**
     * JSON utility class
     */
    private static class JsonUtils {
        private static final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        /**
         * Parse JSON string to Map
         */
        public static Map<String, Object> parseJson(String jsonString) throws Exception {
            return mapper.readValue(jsonString, Map.class);
        }

        /**
         * Serialize Map to JSON string
         */
        public static String toJson(Map<String, Object> map) throws Exception {
            return mapper.writeValueAsString(map);
        }
    }
}
