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
 * 提供只读的 kubeconfig 检视能力：
 * - configuration_contexts_list：列出所有上下文与对应 server，并标记当前上下文
 * - configuration_view：返回 kubeconfig 内容（默认最小化为当前上下文相关片段）
 */
public class ConfigTools {

    static {
        // 注册工具到 MCP 实例
        // 注意：这里需要根据实际 McpServer 实现调整
    }

    /**
     * 返回第一个存在的 kubeconfig 路径：
     * - 优先环境变量 KUBECONFIG（可包含多个路径，使用系统路径分隔符分隔）
     * - 否则默认 ~/.kube/config
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
     * 展开用户主目录路径
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
     * 读取并解析 kubeconfig，优先使用 YAML 解析；失败则尝试 JSON。
     * 返回 (配置字典, 实际使用的文件路径)；找不到则 (null, null)。
     */
    private static Map<String, Object> loadKubeconfigContent() {
        String path = firstExistingKubeconfig();
        if (path == null) {
            return null;
        }

        try {
            String rawContent = Files.readString(new File(path).toPath());
            
            // 优先 YAML 解析
            try {
                return YamlUtils.parseYaml(rawContent);
            } catch (Exception e) {
                // 退化 JSON 解析
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
     * 仅保留与 current-context 相关的 contexts/clusters/users 片段。
     * 未配置 current-context 时返回原始配置。
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
     * 从 Map 中获取 List，如果不存在则返回空列表
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
     * 列出所有可用上下文名称和关联的服务器 URL
     *     输出示例：
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
     * 获取当前 Kubernetes 配置内容
     * 返回 YAML 字符串；当未安装 PyYAML 或解析失败时，返回 JSON 字符串。
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

        // 尝试 YAML 序列化
        try {
            return YamlUtils.toYaml(out);
        } catch (Exception e) {
            // 退化为 JSON
            try {
                return JsonUtils.toJson(out);
            } catch (Exception ex) {
                return "# Failed to serialize kubeconfig";
            }
        }
    }

    /**
     * YAML 工具类
     */
    private static class YamlUtils {
        private static final org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();

        /**
         * 解析 YAML 字符串为 Map
         */
        public static Map<String, Object> parseYaml(String yamlString) throws Exception {
            return yaml.load(yamlString);
        }

        /**
         * 将 Map 序列化为 YAML 字符串
         */
        public static String toYaml(Map<String, Object> map) throws Exception {
            return yaml.dump(map);
        }
    }

    /**
     * JSON 工具类
     */
    private static class JsonUtils {
        private static final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        /**
         * 解析 JSON 字符串为 Map
         */
        public static Map<String, Object> parseJson(String jsonString) throws Exception {
            return mapper.readValue(jsonString, Map.class);
        }

        /**
         * 将 Map 序列化为 JSON 字符串
         */
        public static String toJson(Map<String, Object> map) throws Exception {
            return mapper.writeValueAsString(map);
        }
    }
}
