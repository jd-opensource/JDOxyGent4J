package com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.core_tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.KubernetesDynamicClient;
import com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.KubernetesMcpServer;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Kubernetes MCP Server - core tools: resources
 * <p>
 * Provides capabilities related to Kubernetes resources:
 * - resources_list: List resources of specified type
 * - resources_get: Get the complete object of a specified resource
 * - resources_create_or_update: Create or update resource
 * - resources_delete: Delete specified resource
 */
public class ResourcesTool {


    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    /**
     * Parse YAML/JSON string to Map
     */
    private static Map<String, Object> parseResource(String resource) throws Exception {
        try {
            // Try to parse as JSON
            return JSON_MAPPER.readValue(resource, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            // Try to parse as YAML
            return YAML_MAPPER.readValue(resource, new TypeReference<Map<String, Object>>() {});
        }
    }

    /**
     * List resources of specified type
     */
    @MCPTool(name = "resources_list",
            description = "List Kubernetes resources by apiVersion/kind (optional: namespace, labelSelector)")
    public static List<Map<String, Object>> resourcesList(
            @ToolParam(description = "API version of the resource e.g. 'v1','apps/v1','networking.k8s.io/v1'")
            String apiVersion,
            @ToolParam(description = "Kind of the resource e.g. 'Pod','Service','Deployment','Ingress'")
            String kind,
            @ToolParam(description = "Namespace of the resource (optional for cluster-scoped resources)", required = false)
            String namespace,
            @ToolParam(description = "Label selector to filter resources e.g. 'app=myapp,env=prod'" , required = false)
            String labelSelector,
            @ToolParam(description = "Kubeconfig context name; defaults to current context" , required = false)
            String context) {
        try {
            KubernetesDynamicClient dyn = KubernetesDynamicClient.getDynamicClient(context);
            KubernetesDynamicClient.DynamicResource resource = dyn.resource(apiVersion, kind);

            // List resources
            List<Map<String, Object>> items;
            if (namespace != null && !namespace.trim().isEmpty()) {
                items = resource.list(namespace, labelSelector, null, null, null, null);
            } else {
                items = resource.listAllNamespaces(labelSelector);
            }

            // Apply Secret masking
            List<Map<String, Object>> maskedItems = new ArrayList<>();
            for (Map<String, Object> item : items) {
                maskedItems.add(KubernetesDynamicClient.maskSecret(item));
            }

            return maskedItems;

        } catch (Exception e) {
            throw new RuntimeException("Failed to list resources: " + e.getMessage(), e);
        }
    }

    /**
     * Get the complete object of a specified resource
     */
    @MCPTool(name = "resources_get",
            description = "Get a Kubernetes resource by apiVersion/kind/name (optional: namespace)")
    public static Map<String, Object> resourcesGet(
            @ToolParam(description = "API version of the resource e.g. 'apps/v1")
            String apiVersion,
            @ToolParam(description = "Kind of the resource e.g. 'Deployment")
            String kind,
            @ToolParam(description = "Name of the resource")
            String name,
            @ToolParam(description = "Namespace of the resource (optional for cluster-scoped resources)", required = false)
            String namespace,
            @ToolParam(description = "Kubeconfig context name; defaults to current context", required = false)
            String context) {

        try {
            KubernetesDynamicClient dyn = KubernetesDynamicClient.getDynamicClient(context);
            KubernetesDynamicClient.DynamicResource resource = dyn.resource(apiVersion, kind);

            // Get resource
            Map<String, Object> result = resource.get(name, namespace);

            // Apply Secret masking
            return KubernetesDynamicClient.maskSecret(result);

        } catch (Exception e) {
            throw new RuntimeException("Failed to get resource: " + e.getMessage(), e);
        }
    }

    /**
     * Create or update resource
     */
    @MCPTool(name = "resources_create_or_update",
            description = "Create or update a Kubernetes resource from YAML/JSON (server-side patch on exists)")
    public static Map<String, Object> resourcesCreateOrUpdate(
            @ToolParam(description = "YAML/JSON resource string must include top-level fields like apiVersion/kind/metadata")
            String resource,
            @ToolParam(description = "Default namespace (used if resource.metadata.namespace is not provided)", required = false)
            String namespace,
            @ToolParam(description = "Kubeconfig context", required = false)
            String context) {
        // Security protection: Reject write operations when in read-only or disable-destructive mode
        if (KubernetesMcpServer.isReadOnly() || KubernetesMcpServer.isDisableDestructive()) {
            throw new RuntimeException("Write operations are prohibited: currently in read-only or disable-destructive mode");
        }

        try {
            // Parse resource
            Map<String, Object> obj = parseResource(resource);

            // Validate required fields
            String apiVersion = (String) obj.get("apiVersion");
            String kind = (String) obj.get("kind");
            Map<String, Object> metadata = (Map<String, Object>) obj.get("metadata");

            if (apiVersion == null || kind == null || metadata == null) {
                throw new RuntimeException("Resource missing required fields: apiVersion/kind/metadata");
            }

            String name = (String) metadata.get("name");
            if (name == null || name.trim().isEmpty()) {
                throw new RuntimeException("Resource missing required field: metadata.name");
            }

            // Determine namespace
            String resourceNamespace = (String) metadata.get("namespace");
            if (resourceNamespace == null || resourceNamespace.trim().isEmpty()) {
                resourceNamespace = namespace;
            }

            KubernetesDynamicClient dyn = KubernetesDynamicClient.getDynamicClient(context);
            KubernetesDynamicClient.DynamicResource resourceClient = dyn.resource(apiVersion, kind);

            // First try to get to check existence
            boolean exists = false;
            try {
                resourceClient.get(name, resourceNamespace);
                exists = true;
            } catch (Exception e) {
                exists = false;
            }

            Map<String, Object> result;
            if (exists) {
                // Use patch to update (server-side merge)
                result = resourceClient.patch(name, obj, resourceNamespace, "merge");
            } else {
                // Create new resource
                result = resourceClient.create(obj, resourceNamespace);
            }

            // Apply Secret masking
            return KubernetesDynamicClient.maskSecret(result);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create/update resource: " + e.getMessage(), e);
        }
    }

    /**
     * Delete specified resource
     */
    @MCPTool(name = "resources_delete",
            description = "Delete a Kubernetes resource by apiVersion/kind/name (optional: namespace)")
    public static Map<String, Object> resourcesDelete(
            @ToolParam(description = "API version of the resource e.g. 'v1','apps/v1'")
            String apiVersion,
            @ToolParam(description = "Kind of the resource e.g. 'Pod','Deployment'")
            String kind,
            @ToolParam(description = "Name of the resource")
            String name,
            @ToolParam(description = "Namespace of the resource (optional for cluster-scoped resources)" , required = false)
            String namespace,
            @ToolParam(description = "Kubeconfig context name; defaults to current context" , required = false)
            String context) {
        // Security protection: Reject deletion when in read-only or disable-destructive mode
        if (KubernetesMcpServer.isReadOnly() || KubernetesMcpServer.isDisableDestructive()) {
            throw new RuntimeException("Delete operations are prohibited: currently in read-only or disable-destructive mode");
        }

        try {
            KubernetesDynamicClient dyn = KubernetesDynamicClient.getDynamicClient(context);
            KubernetesDynamicClient.DynamicResource resource = dyn.resource(apiVersion, kind);

            // Delete resource
            Map<String, Object> result = resource.delete(name, namespace);

            // Apply Secret masking
            return KubernetesDynamicClient.maskSecret(result);

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete resource: " + e.getMessage(), e);
        }
    }
}
