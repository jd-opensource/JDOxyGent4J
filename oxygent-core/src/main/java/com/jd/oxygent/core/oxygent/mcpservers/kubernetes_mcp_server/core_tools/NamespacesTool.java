package com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.core_tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.KubernetesMcpServer;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Kubernetes MCP Server - core tools: namespaces
 * <p>
 * 提供与 Kubernetes 命名空间相关的操作能力：
 * - namespaces_list：列出所有命名空间
 * - namespaces_get：获取指定命名空间的完整对象
 * - namespaces_create：创建命名空间
 * - namespaces_delete：删除命名空间
 */
public class NamespacesTool {

    /**
     * 生成命名空间摘要信息
     */
    private static Map<String, Object> namespaceSummary(V1Namespace namespace) {
        Map<String, Object> summary = new java.util.HashMap<>();
        
        if (namespace.getMetadata() != null) {
            summary.put("name", namespace.getMetadata().getName());
            summary.put("labels", namespace.getMetadata().getLabels());
            summary.put("annotations", namespace.getMetadata().getAnnotations());
            summary.put("creationTimestamp", namespace.getMetadata().getCreationTimestamp());
        }
        
        if (namespace.getStatus() != null) {
            summary.put("phase", namespace.getStatus().getPhase());
        }
        
        return summary;
    }

    /**
     * 列出所有命名空间
     */
    @MCPTool(name = "namespaces_list",
            description = "List all the Kubernetes namespaces in the current cluster")
    public static List<Map<String, Object>> namespacesList(
            @ToolParam(description = "Kubeconfig context name; defaults to current context")
            String context) {
        try {
            CoreV1Api coreV1Api = PodsTool.loadKubeConfig(context).getCoreV1Api();
            V1NamespaceList namespaceList = coreV1Api.listNamespace().execute();
            
            List<Map<String, Object>> summaries = new ArrayList<>();
            for (V1Namespace namespace : namespaceList.getItems()) {
                summaries.add(namespaceSummary(namespace));
            }

            return summaries;
        } catch (Exception e) {
            throw new RuntimeException("Failed to list namespaces", e);
        }
    }

    /**
     * 获取指定命名空间的完整对象
     */
    @MCPTool(name = "namespaces_get",
            description = "Get a Kubernetes namespace by name")
    public static Map<String, Object> namespacesGet(
            @ToolParam(description = "Namespace name")
            String name,
            @ToolParam(description = "Kubeconfig context name; defaults to current context")
            String context) {
        try {
            CoreV1Api coreV1Api = PodsTool.loadKubeConfig(context).getCoreV1Api();
            V1Namespace namespace = coreV1Api.readNamespace(name).execute();
            
            // 使用 Jackson 将 Namespace 对象转换为 Map
            ObjectMapper mapper = new ObjectMapper();
            return mapper.convertValue(namespace, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get namespace", e);
        }
    }

    /**
     * 创建命名空间
     */
    @MCPTool(name = "namespaces_create",
            description = "Create a new Kubernetes namespace")
    public static Map<String, Object> namespacesCreate(
            @ToolParam(description = "Namespace name")
            String name,
            @ToolParam(description = "Labels for the namespace")
            Map<String, String> labels,
            @ToolParam(description = "Kubeconfig context name; defaults to current context")
            String context) {
        // 安全保护：只读或禁破坏时拒绝写操作
        if (KubernetesMcpServer.isReadOnly() || KubernetesMcpServer.isDisableDestructive()) {
            throw new RuntimeException("写操作被禁止：当前处于只读或禁破坏模式");
        }
        try {
            CoreV1Api coreV1Api = PodsTool.loadKubeConfig(context).getCoreV1Api();
            
            // 创建命名空间对象
            V1ObjectMeta metadata = new V1ObjectMeta();
                         metadata.setName(name);
                         metadata.setLabels(labels);

            V1Namespace namespace = new V1Namespace();
                        namespace.setMetadata(metadata);
            
            // 创建命名空间
            V1Namespace createdNamespace = coreV1Api.createNamespace(namespace).execute();
            
            // 使用 Jackson 将创建的命名空间对象转换为 Map
            ObjectMapper mapper = new ObjectMapper();
            return mapper.convertValue(createdNamespace, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create namespace", e);
        }
    }

    /**
     * 删除命名空间
     */
    @MCPTool(name = "namespaces_delete",
            description = "Delete a Kubernetes namespace")
    public static Map<String, Object> namespacesDelete(
            @ToolParam(description = "Namespace name")
            String name,
            @ToolParam(description = "Kubeconfig context name; defaults to current context")
            String context) {
        // 安全保护：只读或禁破坏时拒绝删除操作
        if (KubernetesMcpServer.isReadOnly() || KubernetesMcpServer.isDisableDestructive()) {
            throw new RuntimeException("删除操作被禁止：当前处于只读或禁破坏模式");
        }

        try {
            CoreV1Api coreV1Api = PodsTool.loadKubeConfig(context).getCoreV1Api();
            // 删除命名空间
            V1Status status = coreV1Api.deleteNamespace(name).execute();
            
            // 使用 Jackson 将删除状态对象转换为 Map
            ObjectMapper mapper = new ObjectMapper();
            return mapper.convertValue(status, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete namespace", e);
        }
    }
}
