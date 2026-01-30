package com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.core_tools.PodsTool;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kubernetes 动态客户端 - 模拟 Python 的 DynamicClient
 * 提供动态的 Kubernetes 资源操作，无需为每种资源类型创建特定客户端
 */
public class KubernetesDynamicClient {

    private final CustomObjectsApi customObjectsApi;
    private final ObjectMapper objectMapper;

    // 缓存 API 组和资源信息
    private final Map<String, String> apiGroupCache = new ConcurrentHashMap<>();
    private final Map<String, String> resourcePluralCache = new ConcurrentHashMap<>();

    /**
     * 获取 KubernetesDynamicClient
     */
    public static KubernetesDynamicClient getDynamicClient(String context) {
        return new KubernetesDynamicClient(context);
    }

    /**
     * 构造函数 - 使用现有 ApiClient
     */
    public KubernetesDynamicClient(String context) {
        this.customObjectsApi = PodsTool.loadKubeConfig(context).getCustomObjectsApi();
        this.objectMapper = new ObjectMapper();
        initResourcePluralCache();
    }
    /**
     * 初始化资源复数形式缓存
     */
    private void initResourcePluralCache() {
        // 常见资源的复数形式映射
        resourcePluralCache.put("Pod", "pods");
        resourcePluralCache.put("Service", "services");
        resourcePluralCache.put("Deployment", "deployments");
        resourcePluralCache.put("StatefulSet", "statefulsets");
        resourcePluralCache.put("DaemonSet", "daemonsets");
        resourcePluralCache.put("ReplicaSet", "replicasets");
        resourcePluralCache.put("ConfigMap", "configmaps");
        resourcePluralCache.put("Secret", "secrets");
        resourcePluralCache.put("PersistentVolume", "persistentvolumes");
        resourcePluralCache.put("PersistentVolumeClaim", "persistentvolumeclaims");
        resourcePluralCache.put("Namespace", "namespaces");
        resourcePluralCache.put("Node", "nodes");
        resourcePluralCache.put("Job", "jobs");
        resourcePluralCache.put("CronJob", "cronjobs");
        resourcePluralCache.put("Ingress", "ingresses");
        resourcePluralCache.put("ServiceAccount", "serviceaccounts");
        resourcePluralCache.put("Role", "roles");
        resourcePluralCache.put("RoleBinding", "rolebindings");
        resourcePluralCache.put("ClusterRole", "clusterroles");
        resourcePluralCache.put("ClusterRoleBinding", "clusterrolebindings");
        resourcePluralCache.put("NetworkPolicy", "networkpolicies");
        resourcePluralCache.put("HorizontalPodAutoscaler", "horizontalpodautoscalers");
        resourcePluralCache.put("StorageClass", "storageclasses");
    }

    /**
     * 解析 API 版本，返回组和版本
     */
    public Map<String, String> parseApiVersion(String apiVersion) {
        String[] parts = apiVersion.split("/", 2);
        Map<String, String> result = new HashMap<>();
        if (parts.length == 2) {
            result.put("group", parts[0]);
            result.put("version", parts[1]);
        } else {
            result.put("group", ""); // core API groups (e.g., v1)
            result.put("version", apiVersion);
        }
        return result;
    }

    /**
     * 获取资源复数形式
     */
    public String getResourcePlural(String kind) {
        // 先从缓存查找
        String plural = resourcePluralCache.get(kind);
        if (plural != null) {
            return plural;
        }

        // 缓存中没有，应用规则生成
        String lowerKind = kind.toLowerCase();

        // 特殊复数规则
        if (kind.endsWith("y")) {
            plural = lowerKind.substring(0, lowerKind.length() - 1) + "ies";
        } else if (kind.endsWith("s") || kind.endsWith("x") || kind.endsWith("z") ||
                kind.endsWith("ch") || kind.endsWith("sh")) {
            plural = lowerKind + "es";
        } else {
            plural = lowerKind + "s";
        }

        // 存入缓存
        resourcePluralCache.put(kind, plural);
        return plural;
    }

    /**
     * 获取动态资源对象（模拟 Python 的 dyn.resources.get）
     */
    public DynamicResource resource(String apiVersion, String kind) {
        Map<String, String> apiInfo = parseApiVersion(apiVersion);
        return new DynamicResource(
                apiInfo.get("group"),
                apiInfo.get("version"),
                getResourcePlural(kind),
                this.customObjectsApi
        );
    }

    /**
     * 动态资源操作类
     */
    public static class DynamicResource {
        private final String group;
        private final String version;
        private final String plural;
        private final CustomObjectsApi api;

        public DynamicResource(String group, String version, String plural, CustomObjectsApi api) {
            this.group = group;
            this.version = version;
            this.plural = plural;
            this.api = api;
        }

        /**
         * 列出资源（支持命名空间和标签选择器）
         */
        @SuppressWarnings("unchecked")
        public List<Map<String, Object>> list(String namespace, String labelSelector,
                                              String fieldSelector, Integer limit,
                                              String resourceVersion, Boolean watch) throws Exception {
            if (namespace != null && !namespace.trim().isEmpty()) {
                // 命名空间级别资源
                Object result = api.listNamespacedCustomObject(group, version, namespace, plural)
                                    .labelSelector(labelSelector)
                                    .limit(limit)
                                    .fieldSelector(fieldSelector)
                                    .resourceVersion(resourceVersion)
                                    .watch(watch)
                                    .execute();
                return extractItems(result);
            } else {
                // 集群级别资源
                Object result = api.listClusterCustomObject(group, version, plural)
                                    .labelSelector(labelSelector)
                                    .limit(limit)
                                    .fieldSelector(fieldSelector)
                                    .resourceVersion(resourceVersion)
                                    .watch(watch)
                                    .execute();
                return extractItems(result);
            }
        }

        /**
         * 列出所有命名空间的资源
         */
        public List<Map<String, Object>> listAllNamespaces(String labelSelector) throws Exception {
            return list(null, labelSelector, null, null, null, null);
        }

        /**
         * 获取单个资源
         */
        @SuppressWarnings("unchecked")
        public Map<String, Object> get(String name, String namespace) throws Exception {
            if (namespace != null && !namespace.trim().isEmpty()) {
                return (Map<String, Object>) api.getNamespacedCustomObject(group, version, namespace, plural, name).execute();
            } else {
                return (Map<String, Object>) api.getClusterCustomObject(group, version, plural, name).execute();
            }
        }

        /**
         * 创建资源
         */
        @SuppressWarnings("unchecked")
        public Map<String, Object> create(Map<String, Object> body, String namespace) throws Exception {
            if (namespace != null && !namespace.trim().isEmpty()) {
                return (Map<String, Object>) api.createNamespacedCustomObject(group, version, namespace, plural, body).execute();
            } else {
                return (Map<String, Object>) api.createClusterCustomObject(group, version, plural, body).execute();
            }
        }

        /**
         * 替换资源（完整更新）
         */
        @SuppressWarnings("unchecked")
        public Map<String, Object> replace(String name, Map<String, Object> body, String namespace) throws Exception {
            if (namespace != null && !namespace.trim().isEmpty()) {
                return (Map<String, Object>) api.replaceNamespacedCustomObject(group, version, namespace, plural, name, body).execute();
            } else {
                return (Map<String, Object>) api.replaceClusterCustomObject(group, version, plural, name, body).execute();
            }
        }

        /**
         * Patch 资源（部分更新）
         */
        @SuppressWarnings("unchecked")
        public Map<String, Object> patch(String name, Map<String, Object> body, String namespace,String patchType) throws Exception {
            if (namespace != null && !namespace.trim().isEmpty()) {
                if(patchType!=null&&!patchType.isEmpty()){
                    api.getApiClient().addDefaultHeader("Content-Type",getContentTypeForPatch(patchType));
                }
                return (Map<String, Object>) api.patchNamespacedCustomObject(group, version, namespace, plural, name, body).execute();
            } else {
                return (Map<String, Object>) api.patchClusterCustomObject(group, version, plural, name, body).execute();
            }
        }

        /**
         * 根据 patchType 获取正确的 Content-Type
         */
        private String getContentTypeForPatch(String patchType) {
            if (patchType == null || patchType.isEmpty()) {
                return "application/merge-patch+json"; // 默认值
            }

            // 支持简写形式
            return switch (patchType.toLowerCase()) {
                case "merge" -> "application/merge-patch+json";
                case "json" -> "application/json-patch+json";
                case "strategic" -> "application/strategic-merge-patch+json";
                default ->
                    // 如果用户已经传入完整的 Content-Type，直接使用
                        patchType;
            };
        }

        /**
         * 删除资源
         */
        @SuppressWarnings("unchecked")
        public Map<String, Object> delete(String name, String namespace) throws Exception {
            if (namespace != null && !namespace.trim().isEmpty()) {
                return (Map<String, Object>) api.deleteNamespacedCustomObject(group, version, namespace, plural, name).execute();
            } else {
                return (Map<String, Object>) api.deleteClusterCustomObject(group, version, plural, name).execute();
            }
        }

        /**
         * 从 API 响应中提取 items
         */
        @SuppressWarnings("unchecked")
        private List<Map<String, Object>> extractItems(Object result) {
            if (result instanceof Map) {
                Map<String, Object> resultMap = (Map<String, Object>) result;
                Object items = resultMap.get("items");
                if (items instanceof List) {
                    return (List<Map<String, Object>>) items;
                }
            }
            return Collections.emptyList();
        }
    }

    /**
     * 掩码 Secret 敏感信息
     */
    public static Map<String, Object> maskSecret(Map<String, Object> obj) {
        if (obj == null || !"Secret".equals(obj.get("kind"))) {
            return obj;
        }

        Map<String, Object> masked = new HashMap<>(obj);

        // 掩码 data 字段
        if (masked.get("data") instanceof Map) {
            Map<String, Object> data = (Map<String, Object>) masked.get("data");
            Map<String, Object> maskedData = new HashMap<>();
            for (String key : data.keySet()) {
                maskedData.put(key, "***");
            }
            masked.put("data", maskedData);
        }

        // 掩码 stringData 字段
        if (masked.get("stringData") instanceof Map) {
            Map<String, Object> stringData = (Map<String, Object>) masked.get("stringData");
            Map<String, Object> maskedStringData = new HashMap<>();
            for (String key : stringData.keySet()) {
                maskedStringData.put(key, "***");
            }
            masked.put("stringData", maskedStringData);
        }

        return masked;
    }

    /**
     * 转换为 JSON 字符串
     */
    public String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    /**
     * 从 JSON 字符串解析
     */
    public Map<String, Object> fromJson(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }
}