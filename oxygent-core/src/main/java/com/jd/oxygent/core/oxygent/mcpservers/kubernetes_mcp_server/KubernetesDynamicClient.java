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
 * Kubernetes Dynamic Client - Simulates Python's DynamicClient
 * Provides dynamic Kubernetes resource operations without creating specific clients for each resource type
 */
public class KubernetesDynamicClient {

    private final CustomObjectsApi customObjectsApi;
    private final ObjectMapper objectMapper;

    // Cache API groups and resource information
    private final Map<String, String> apiGroupCache = new ConcurrentHashMap<>();
    private final Map<String, String> resourcePluralCache = new ConcurrentHashMap<>();

    /**
     * Get KubernetesDynamicClient
     */
    public static KubernetesDynamicClient getDynamicClient(String context) {
        return new KubernetesDynamicClient(context);
    }

    /**
     * Constructor - Use existing ApiClient
     */
    public KubernetesDynamicClient(String context) {
        this.customObjectsApi = PodsTool.loadKubeConfig(context).getCustomObjectsApi();
        this.objectMapper = new ObjectMapper();
        initResourcePluralCache();
    }
    /**
     * Initialize resource plural form cache
     */
    private void initResourcePluralCache() {
        // Plural form mapping for common resources
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
     * Parse API version, return group and version
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
     * Get resource plural form
     */
    public String getResourcePlural(String kind) {
        // First look up from cache
        String plural = resourcePluralCache.get(kind);
        if (plural != null) {
            return plural;
        }

        // Not in cache, apply rules to generate
        String lowerKind = kind.toLowerCase();

        // Special plural rules
        if (kind.endsWith("y")) {
            plural = lowerKind.substring(0, lowerKind.length() - 1) + "ies";
        } else if (kind.endsWith("s") || kind.endsWith("x") || kind.endsWith("z") ||
                kind.endsWith("ch") || kind.endsWith("sh")) {
            plural = lowerKind + "es";
        } else {
            plural = lowerKind + "s";
        }

        // Store in cache
        resourcePluralCache.put(kind, plural);
        return plural;
    }

    /**
     * Get dynamic resource object (simulate Python's dyn.resources.get)
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
     * Dynamic resource operation class
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
         * List resources (supports namespace and label selector)
         */
        @SuppressWarnings("unchecked")
        public List<Map<String, Object>> list(String namespace, String labelSelector,
                                              String fieldSelector, Integer limit,
                                              String resourceVersion, Boolean watch) throws Exception {
            if (namespace != null && !namespace.trim().isEmpty()) {
                // Namespace-level resources
                Object result = api.listNamespacedCustomObject(group, version, namespace, plural)
                                    .labelSelector(labelSelector)
                                    .limit(limit)
                                    .fieldSelector(fieldSelector)
                                    .resourceVersion(resourceVersion)
                                    .watch(watch)
                                    .execute();
                return extractItems(result);
            } else {
                // Cluster-level resources
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
         * List resources from all namespaces
         */
        public List<Map<String, Object>> listAllNamespaces(String labelSelector) throws Exception {
            return list(null, labelSelector, null, null, null, null);
        }

        /**
         * Get single resource
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
         * Create resource
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
         * Replace resource (full update)
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
         * Patch resource (partial update)
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
         * Get correct Content-Type based on patchType
         */
        private String getContentTypeForPatch(String patchType) {
            if (patchType == null || patchType.isEmpty()) {
                return "application/merge-patch+json"; // Default value
            }

            // Support abbreviated forms
            return switch (patchType.toLowerCase()) {
                case "merge" -> "application/merge-patch+json";
                case "json" -> "application/json-patch+json";
                case "strategic" -> "application/strategic-merge-patch+json";
                default ->
                    // If user has passed in the complete Content-Type, use directly
                        patchType;
            };
        }

        /**
         * Delete resource
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
         * Extract items from API response
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
     * Mask Secret sensitive information
     */
    public static Map<String, Object> maskSecret(Map<String, Object> obj) {
        if (obj == null || !"Secret".equals(obj.get("kind"))) {
            return obj;
        }

        Map<String, Object> masked = new HashMap<>(obj);

        // Mask data field
        if (masked.get("data") instanceof Map) {
            Map<String, Object> data = (Map<String, Object>) masked.get("data");
            Map<String, Object> maskedData = new HashMap<>();
            for (String key : data.keySet()) {
                maskedData.put(key, "***");
            }
            masked.put("data", maskedData);
        }

        // Mask stringData field
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
     * Convert to JSON string
     */
    public String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    /**
     * Parse from JSON string
     */
    public Map<String, Object> fromJson(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }
}