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
 * 提供与 Kubernetes 资源相关的操作能力：
 * - resources_list：列出指定类型的资源
 * - resources_get：获取指定资源的完整对象
 * - resources_create_or_update：创建或更新资源
 * - resources_delete：删除指定资源
 */
public class ResourcesTool {


    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    /**
     * 解析 YAML/JSON 字符串为 Map
     */
    private static Map<String, Object> parseResource(String resource) throws Exception {
        try {
            // 尝试解析为 JSON
            return JSON_MAPPER.readValue(resource, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            // 尝试解析为 YAML
            return YAML_MAPPER.readValue(resource, new TypeReference<Map<String, Object>>() {});
        }
    }

    /**
     * 列出指定类型的资源
     */
    @MCPTool(name = "resources_list",
            description = "List Kubernetes resources by apiVersion/kind (optional: namespace, labelSelector)")
    public static List<Map<String, Object>> resourcesList(
            @ToolParam(description = "API version of the resource 例如 'v1','apps/v1','networking.k8s.io/v1'")
            String apiVersion,
            @ToolParam(description = "Kind of the resource 例如 'Pod','Service','Deployment','Ingress'")
            String kind,
            @ToolParam(description = "Namespace of the resource (optional for cluster-scoped resources)", required = false)
            String namespace,
            @ToolParam(description = "Label selector to filter resources （例如 'app=myapp,env=prod'）" , required = false)
            String labelSelector,
            @ToolParam(description = "Kubeconfig context name; defaults to current context" , required = false)
            String context) {
        try {
            KubernetesDynamicClient dyn = KubernetesDynamicClient.getDynamicClient(context);
            KubernetesDynamicClient.DynamicResource resource = dyn.resource(apiVersion, kind);

            // 列出资源
            List<Map<String, Object>> items;
            if (namespace != null && !namespace.trim().isEmpty()) {
                items = resource.list(namespace, labelSelector, null, null, null, null);
            } else {
                items = resource.listAllNamespaces(labelSelector);
            }

            // 应用 Secret 掩码
            List<Map<String, Object>> maskedItems = new ArrayList<>();
            for (Map<String, Object> item : items) {
                maskedItems.add(KubernetesDynamicClient.maskSecret(item));
            }

            return maskedItems;

        } catch (Exception e) {
            throw new RuntimeException("列出资源失败：" + e.getMessage(), e);
        }
    }

    /**
     * 获取指定资源的完整对象
     */
    @MCPTool(name = "resources_get",
            description = "Get a Kubernetes resource by apiVersion/kind/name (optional: namespace)")
    public static Map<String, Object> resourcesGet(
            @ToolParam(description = "API version of the resource 例如 'apps/v1")
            String apiVersion,
            @ToolParam(description = "Kind of the resource 例如 'Deployment")
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

            // 获取资源
            Map<String, Object> result = resource.get(name, namespace);

            // 应用 Secret 掩码
            return KubernetesDynamicClient.maskSecret(result);

        } catch (Exception e) {
            throw new RuntimeException("获取资源失败：" + e.getMessage(), e);
        }
    }

    /**
     * 创建或更新资源
     */
    @MCPTool(name = "resources_create_or_update",
            description = "Create or update a Kubernetes resource from YAML/JSON (server-side patch on exists)")
    public static Map<String, Object> resourcesCreateOrUpdate(
            @ToolParam(description = "YAML/JSON resource string 需包含 apiVersion/kind/metadata 等顶级字段")
            String resource,
            @ToolParam(description = "Default namespace （若 resource.metadata.namespace 未提供时可作为默认值）", required = false)
            String namespace,
            @ToolParam(description = "Kubeconfig context", required = false)
            String context) {
        // 安全保护：只读或禁破坏时拒绝写操作
        if (KubernetesMcpServer.isReadOnly() || KubernetesMcpServer.isDisableDestructive()) {
            throw new RuntimeException("写操作被禁止：当前处于只读或禁破坏模式");
        }

        try {
            // 解析资源
            Map<String, Object> obj = parseResource(resource);

            // 验证必要字段
            String apiVersion = (String) obj.get("apiVersion");
            String kind = (String) obj.get("kind");
            Map<String, Object> metadata = (Map<String, Object>) obj.get("metadata");

            if (apiVersion == null || kind == null || metadata == null) {
                throw new RuntimeException("资源缺少必要字段：apiVersion/kind/metadata");
            }

            String name = (String) metadata.get("name");
            if (name == null || name.trim().isEmpty()) {
                throw new RuntimeException("资源缺少必要字段：metadata.name");
            }

            // 确定命名空间
            String resourceNamespace = (String) metadata.get("namespace");
            if (resourceNamespace == null || resourceNamespace.trim().isEmpty()) {
                resourceNamespace = namespace;
            }

            KubernetesDynamicClient dyn = KubernetesDynamicClient.getDynamicClient(context);
            KubernetesDynamicClient.DynamicResource resourceClient = dyn.resource(apiVersion, kind);

            // 先尝试获取以判断存在性
            boolean exists = false;
            try {
                resourceClient.get(name, resourceNamespace);
                exists = true;
            } catch (Exception e) {
                exists = false;
            }

            Map<String, Object> result;
            if (exists) {
                // 使用 patch 更新（服务器端合并）
                result = resourceClient.patch(name, obj, resourceNamespace, "merge");
            } else {
                // 创建新资源
                result = resourceClient.create(obj, resourceNamespace);
            }

            // 应用 Secret 掩码
            return KubernetesDynamicClient.maskSecret(result);

        } catch (Exception e) {
            throw new RuntimeException("创建/更新资源失败：" + e.getMessage(), e);
        }
    }

    /**
     * 删除指定资源
     */
    @MCPTool(name = "resources_delete",
            description = "Delete a Kubernetes resource by apiVersion/kind/name (optional: namespace)")
    public static Map<String, Object> resourcesDelete(
            @ToolParam(description = "API version of the resource 例如 'v1','apps/v1'")
            String apiVersion,
            @ToolParam(description = "Kind of the resource 例如 'Pod','Deployment'")
            String kind,
            @ToolParam(description = "Name of the resource")
            String name,
            @ToolParam(description = "Namespace of the resource (optional for cluster-scoped resources)" , required = false)
            String namespace,
            @ToolParam(description = "Kubeconfig context name; defaults to current context" , required = false)
            String context) {
        // 安全保护：只读或禁破坏时拒绝删除
        if (KubernetesMcpServer.isReadOnly() || KubernetesMcpServer.isDisableDestructive()) {
            throw new RuntimeException("删除操作被禁止：当前处于只读或禁破坏模式");
        }

        try {
            KubernetesDynamicClient dyn = KubernetesDynamicClient.getDynamicClient(context);
            KubernetesDynamicClient.DynamicResource resource = dyn.resource(apiVersion, kind);

            // 删除资源
            Map<String, Object> result = resource.delete(name, namespace);

            // 应用 Secret 掩码
            return KubernetesDynamicClient.maskSecret(result);

        } catch (Exception e) {
            throw new RuntimeException("删除资源失败：" + e.getMessage(), e);
        }
    }
}
