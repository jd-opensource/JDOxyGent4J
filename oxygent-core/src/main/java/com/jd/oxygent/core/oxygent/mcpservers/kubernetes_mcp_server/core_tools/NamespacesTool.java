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
 * Provides capabilities related to Kubernetes namespaces:
 * - namespaces_list: List all namespaces
 * - namespaces_get: Get the complete object of a specified namespace
 * - namespaces_create: Create a namespace
 * - namespaces_delete: Delete a namespace
 */
public class NamespacesTool {

    /**
     * Generate namespace summary information
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
     * List all namespaces
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
     * Get the complete object of a specified namespace
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
            
            // Use Jackson to convert Namespace object to Map
            ObjectMapper mapper = new ObjectMapper();
            return mapper.convertValue(namespace, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get namespace", e);
        }
    }

    /**
     * Create a namespace
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
        // Security protection: Reject write operations when in read-only or disable-destructive mode
        if (KubernetesMcpServer.isReadOnly() || KubernetesMcpServer.isDisableDestructive()) {
            throw new RuntimeException("Write operations are prohibited: currently in read-only or disable-destructive mode");
        }
        try {
            CoreV1Api coreV1Api = PodsTool.loadKubeConfig(context).getCoreV1Api();
            
            // Create namespace object
            V1ObjectMeta metadata = new V1ObjectMeta();
                         metadata.setName(name);
                         metadata.setLabels(labels);

            V1Namespace namespace = new V1Namespace();
                        namespace.setMetadata(metadata);
            
            // Create namespace
            V1Namespace createdNamespace = coreV1Api.createNamespace(namespace).execute();
            
            // Use Jackson to convert the created namespace object to Map
            ObjectMapper mapper = new ObjectMapper();
            return mapper.convertValue(createdNamespace, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create namespace", e);
        }
    }

    /**
     * Delete a namespace
     */
    @MCPTool(name = "namespaces_delete",
            description = "Delete a Kubernetes namespace")
    public static Map<String, Object> namespacesDelete(
            @ToolParam(description = "Namespace name")
            String name,
            @ToolParam(description = "Kubeconfig context name; defaults to current context")
            String context) {
        // Security protection: Reject delete operations when in read-only or disable-destructive mode
        if (KubernetesMcpServer.isReadOnly() || KubernetesMcpServer.isDisableDestructive()) {
            throw new RuntimeException("Delete operations are prohibited: currently in read-only or disable-destructive mode");
        }

        try {
            CoreV1Api coreV1Api = PodsTool.loadKubeConfig(context).getCoreV1Api();
            // Delete namespace
            V1Status status = coreV1Api.deleteNamespace(name).execute();
            
            // Use Jackson to convert the delete status object to Map
            ObjectMapper mapper = new ObjectMapper();
            return mapper.convertValue(status, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete namespace", e);
        }
    }
}
