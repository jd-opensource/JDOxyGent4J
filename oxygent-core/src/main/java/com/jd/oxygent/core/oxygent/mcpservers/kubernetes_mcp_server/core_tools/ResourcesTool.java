package com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.core_tools;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.KubernetesMcpServer;

import java.util.List;
import java.util.Map;

/**
 * Kubernetes MCP Server - core tools: resources
 * <p>
 * 提供与 Kubernetes 资源相关的操作能力：
 * - resources_list：列出指定类型的资源
 * - resources_get：获取指定资源的完整对象
 * - resources_create_or_update：创建或更新资源
 * - resources_delete：删除指定资源
 */
public class ResourcesTool {

    /**
     * 确保 Kubernetes 客户端可用
     */
    private static void ensureK8sAvailable() {
        PodsTool.ensureK8sAvailable();
    }

    /**
     * 加载 Kubernetes 配置
     */
    private static PodsTool.K8sClientHolder loadKubeConfig(String context) {
        return PodsTool.loadKubeConfig(context);
    }

    /**
     * 获取自定义对象 API 客户端实例
     */
    private static io.kubernetes.client.openapi.apis.CustomObjectsApi getCustomObjectsApi(PodsTool.K8sClientHolder clientHolder) throws Exception {
        return new io.kubernetes.client.openapi.apis.CustomObjectsApi(clientHolder.getApiClient());
    }

    /**
     * 解析 API 版本和资源类型
     */
    private static String[] parseGroupVersion(String apiVersion) {
        String[] parts = apiVersion.split("/");
        if (parts.length == 2) {
            return parts; // [group, version]
        } else {
            return new String[]{"", apiVersion}; // [empty group, version]
        }
    }

    /**
     * 列出指定类型的资源
     */
    @MCPTool(name = "resources_list",
            description = "List Kubernetes resources of the specified type")
    public static List<Map<String, Object>> resourcesList(
            @ToolParam(description = "API version of the resource")
            String apiVersion,
            @ToolParam(description = "Kind of the resource")
            String kind,
            @ToolParam(description = "Namespace of the resource (optional for cluster-scoped resources)")
            String namespace,
            @ToolParam(description = "Label selector to filter resources")
            String labelSelector,
            @ToolParam(description = "Kubeconfig context name; defaults to current context")
            String context) {
        ensureK8sAvailable();
        PodsTool.K8sClientHolder clientHolder = loadKubeConfig(context);

        try {
            io.kubernetes.client.openapi.apis.CustomObjectsApi customObjectsApi = getCustomObjectsApi(clientHolder);
            String[] groupVersion = parseGroupVersion(apiVersion);
            String group = groupVersion[0];
            String version = groupVersion[1];
            
            // 资源类型的复数形式（简单转换，实际可能需要更复杂的规则）
            String plural = kind.toLowerCase() + "s";
            
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            
            if (namespace != null && !namespace.isEmpty()) {
                // 列出命名空间作用域的资源
                Object list = customObjectsApi.listNamespacedCustomObject(group, version, namespace, plural).execute();
                if (list instanceof Map) {
                    Map<String, Object> listMap = (Map<String, Object>) list;
                    if (listMap.containsKey("items")) {
                        List<?> items = (List<?>) listMap.get("items");
                        for (Object item : items) {
                            if (item instanceof Map) {
                                result.add((Map<String, Object>) item);
                            }
                        }
                    }
                }
            } else {
                // 列出集群作用域的资源
                Object list = customObjectsApi.listClusterCustomObject(group, version, plural).execute();
                if (list instanceof Map) {
                    Map<String, Object> listMap = (Map<String, Object>) list;
                    if (listMap.containsKey("items")) {
                        List<?> items = (List<?>) listMap.get("items");
                        for (Object item : items) {
                            if (item instanceof Map) {
                                result.add((Map<String, Object>) item);
                            }
                        }
                    }
                }
            }
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to list resources", e);
        }
    }

    /**
     * 获取指定资源的完整对象
     */
    @MCPTool(name = "resources_get",
            description = "Get a Kubernetes resource by name")
    public static Map<String, Object> resourcesGet(
            @ToolParam(description = "API version of the resource")
            String apiVersion,
            @ToolParam(description = "Kind of the resource")
            String kind,
            @ToolParam(description = "Name of the resource")
            String name,
            @ToolParam(description = "Namespace of the resource (optional for cluster-scoped resources)")
            String namespace,
            @ToolParam(description = "Kubeconfig context name; defaults to current context")
            String context) {
        ensureK8sAvailable();
        PodsTool.K8sClientHolder clientHolder = loadKubeConfig(context);

        try {
            io.kubernetes.client.openapi.apis.CustomObjectsApi customObjectsApi = getCustomObjectsApi(clientHolder);
            String[] groupVersion = parseGroupVersion(apiVersion);
            String group = groupVersion[0];
            String version = groupVersion[1];
            
            // 资源类型的复数形式（简单转换，实际可能需要更复杂的规则）
            String plural = kind.toLowerCase() + "s";
            
            if (namespace != null && !namespace.isEmpty()) {
                // 获取命名空间作用域的资源
                Object resource = customObjectsApi.getNamespacedCustomObject(group, version, namespace, plural, name).execute();
                if (resource instanceof Map) {
                    return (Map<String, Object>) resource;
                } else {
                    throw new RuntimeException("Resource is not a map");
                }
            } else {
                // 获取集群作用域的资源
                Object resource = customObjectsApi.getClusterCustomObject(group, version, plural, name).execute();
                if (resource instanceof Map) {
                    return (Map<String, Object>) resource;
                } else {
                    throw new RuntimeException("Resource is not a map");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get resource", e);
        }
    }

    /**
     * 创建或更新资源
     */
    @MCPTool(name = "resources_create_or_update",
            description = "Create or update a Kubernetes resource from YAML/JSON")
    public static Map<String, Object> resourcesCreateOrUpdate(
            @ToolParam(description = "YAML/JSON resource string")
            String resource,
            @ToolParam(description = "Default namespace")
            String namespace,
            @ToolParam(description = "Kubeconfig context")
            String context) {
        // 安全保护：只读或禁破坏时拒绝写操作
        if (KubernetesMcpServer.isReadOnly() || KubernetesMcpServer.isDisableDestructive()) {
            throw new RuntimeException("写操作被禁止：当前处于只读或禁破坏模式");
        }

        ensureK8sAvailable();
        PodsTool.K8sClientHolder clientHolder = loadKubeConfig(context);

        try {
            io.kubernetes.client.openapi.apis.CustomObjectsApi customObjectsApi = getCustomObjectsApi(clientHolder);
            
            // 解析 YAML/JSON 资源定义
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.dataformat.yaml.YAMLFactory yamlFactory = new com.fasterxml.jackson.dataformat.yaml.YAMLFactory();
            com.fasterxml.jackson.databind.ObjectMapper yamlMapper = new com.fasterxml.jackson.databind.ObjectMapper(yamlFactory);
            
            Map<String, Object> resourceMap;
            try {
                // 尝试解析为 JSON
                resourceMap = mapper.readValue(resource, Map.class);
            } catch (Exception e) {
                // 尝试解析为 YAML
                resourceMap = yamlMapper.readValue(resource, Map.class);
            }
            
            // 提取 API 版本、资源类型和元数据
            String apiVersion = (String) resourceMap.get("apiVersion");
            String kind = (String) resourceMap.get("kind");
            Map<String, Object> metadata = (Map<String, Object>) resourceMap.get("metadata");
            
            if (apiVersion == null || kind == null) {
                throw new RuntimeException("Invalid resource definition: missing apiVersion or kind");
            }
            
            // 确定命名空间
            String resourceNamespace = null;
            if (metadata != null && metadata.containsKey("namespace")) {
                resourceNamespace = (String) metadata.get("namespace");
            } else if (namespace != null && !namespace.isEmpty()) {
                resourceNamespace = namespace;
            }
            
            // 解析 API 版本和资源类型
            String[] groupVersion = parseGroupVersion(apiVersion);
            String group = groupVersion[0];
            String version = groupVersion[1];
            
            // 资源类型的复数形式（简单转换，实际可能需要更复杂的规则）
            String plural = kind.toLowerCase() + "s";
            
            // 尝试更新资源，如果不存在则创建
            String resourceName = (String) metadata.get("name");
            Map<String, Object> result;
            
            try {
                if (resourceNamespace != null && !resourceNamespace.isEmpty()) {
                    // 尝试更新命名空间作用域的资源
                    Object updateResult = customObjectsApi.replaceNamespacedCustomObject(group, version, resourceNamespace, plural, resourceName, resourceMap).execute();
                    if (updateResult instanceof Map) {
                        result = (Map<String, Object>) updateResult;
                    } else {
                        throw new RuntimeException("Update result is not a map");
                    }
                } else {
                    // 尝试更新集群作用域的资源
                    Object updateResult = customObjectsApi.replaceClusterCustomObject(group, version, plural, resourceName, resourceMap).execute();
                    if (updateResult instanceof Map) {
                        result = (Map<String, Object>) updateResult;
                    } else {
                        throw new RuntimeException("Update result is not a map");
                    }
                }
            } catch (Exception e) {
                // 更新失败，尝试创建
                try {
                    if (resourceNamespace != null && !resourceNamespace.isEmpty()) {
                        // 创建命名空间作用域的资源
                        Object createResult = customObjectsApi.createNamespacedCustomObject(group, version, resourceNamespace, plural, resourceMap).execute();
                        if (createResult instanceof Map) {
                            result = (Map<String, Object>) createResult;
                        } else {
                            throw new RuntimeException("Create result is not a map");
                        }
                    } else {
                        // 创建集群作用域的资源
                        Object createResult = customObjectsApi.createClusterCustomObject(group, version, plural, resourceMap).execute();
                        if (createResult instanceof Map) {
                            result = (Map<String, Object>) createResult;
                        } else {
                            throw new RuntimeException("Create result is not a map");
                        }
                    }
                } catch (Exception createException) {
                    throw new RuntimeException("Failed to create resource after update failed", createException);
                }
            }
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create or update resource", e);
        }
    }

    /**
     * 删除指定资源
     */
    @MCPTool(name = "resources_delete",
            description = "Delete a Kubernetes resource")
    public static Map<String, Object> resourcesDelete(
            @ToolParam(description = "API version of the resource")
            String apiVersion,
            @ToolParam(description = "Kind of the resource")
            String kind,
            @ToolParam(description = "Name of the resource")
            String name,
            @ToolParam(description = "Namespace of the resource (optional for cluster-scoped resources)")
            String namespace,
            @ToolParam(description = "Kubeconfig context name; defaults to current context")
            String context) {
        // 安全保护：只读或禁破坏时拒绝删除操作
        if (KubernetesMcpServer.isReadOnly() || KubernetesMcpServer.isDisableDestructive()) {
            throw new RuntimeException("删除操作被禁止：当前处于只读或禁破坏模式");
        }

        ensureK8sAvailable();
        PodsTool.K8sClientHolder clientHolder = loadKubeConfig(context);

        try {
            io.kubernetes.client.openapi.apis.CustomObjectsApi customObjectsApi = getCustomObjectsApi(clientHolder);
            String[] groupVersion = parseGroupVersion(apiVersion);
            String group = groupVersion[0];
            String version = groupVersion[1];
            
            // 资源类型的复数形式（简单转换，实际可能需要更复杂的规则）
            String plural = kind.toLowerCase() + "s";
            
            if (namespace != null && !namespace.isEmpty()) {
                // 删除命名空间作用域的资源
                Object status = customObjectsApi.deleteNamespacedCustomObject(group, version, namespace, plural, name).execute();
                if (status instanceof Map) {
                    return (Map<String, Object>) status;
                } else {
                    throw new RuntimeException("Delete status is not a map");
                }
            } else {
                // 删除集群作用域的资源
                Object status = customObjectsApi.deleteClusterCustomObject(group, version, plural, name).execute();
                if (status instanceof Map) {
                    return (Map<String, Object>) status;
                } else {
                    throw new RuntimeException("Delete status is not a map");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete resource", e);
        }
    }
}
