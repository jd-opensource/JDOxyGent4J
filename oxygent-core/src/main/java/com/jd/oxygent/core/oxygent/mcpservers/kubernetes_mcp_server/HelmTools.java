package com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.core_tools.PodsTool;

import java.util.List;
import java.util.Map;

/**
 * Kubernetes MCP Server - helm tools (template-first approach)
 * <p>
 * 提供无需 Helm 二进制的模板化能力：
 * - helm_template_apply：使用 Jinja2 渲染 Helm 风格模板（或通用 YAML 模板），将生成的多文档 YAML 逐条 Create/Patch 到集群
 * - helm_template_uninstall：使用同一模板与 values 渲染出目标对象，逐条 Delete
 */
public class HelmTools {

    // 尝试导入必要的依赖
    private static boolean templateEngineAvailable = false;

    static {
        try {
            // 检查模板引擎是否可用
            Class.forName("org.apache.commons.text.StringSubstitutor");
            templateEngineAvailable = true;
        } catch (ClassNotFoundException e) {
            templateEngineAvailable = false;
        }
    }

    /**
     * 确保 Kubernetes 客户端可用
     */
    private static void ensureK8sAvailable() {
        // 复用 PodsTool 中的检查
        PodsTool.ensureK8sAvailable();
    }

    /**
     * 确保模板引擎可用
     */
    private static void ensureTemplateEngine() {
        if (!templateEngineAvailable) {
            throw new RuntimeException("模板引擎未安装");
        }
    }

    /**
     * 加载 Kubernetes 配置
     */
    private static PodsTool.K8sClientHolder loadKubeConfig(String context) {
        return PodsTool.loadKubeConfig(context);
    }

    /**
     * 使用模板引擎渲染模板，解析为多 YAML 文档对象列表
     */
    private static List<Map<String, Object>> renderToDocuments(String template, Map<String, Object> values) {
        ensureTemplateEngine();
        try {
            // 使用 Apache Commons Text 渲染模板
            org.apache.commons.text.StringSubstitutor substitutor = new org.apache.commons.text.StringSubstitutor(values);
            String renderedTemplate = substitutor.replace(template);
            
            // 解析多文档 YAML
            List<Map<String, Object>> documents = new java.util.ArrayList<>();
            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
            
            // 分割多文档 YAML
            String[] parts = renderedTemplate.split("---");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    Map<String, Object> doc = yaml.load(trimmed);
                    if (doc != null) {
                        documents.add(doc);
                    }
                }
            }
            
            return documents;
        } catch (Exception e) {
            throw new RuntimeException("模板渲染失败: " + e.getMessage(), e);
        }
    }

    /**
     * 对单个对象：若存在则 merge-patch，不存在则 create
     */
    private static Map<String, Object> createOrPatch(PodsTool.K8sClientHolder clientHolder, Map<String, Object> obj, String defaultNamespace) {
        ensureK8sAvailable();
        try {
            // 提取资源信息
            String apiVersion = (String) obj.get("apiVersion");
            String kind = (String) obj.get("kind");
            Map<String, Object> metadata = (Map<String, Object>) obj.get("metadata");
            
            if (apiVersion == null || kind == null || metadata == null) {
                throw new RuntimeException("无效的资源定义：缺少 apiVersion、kind 或 metadata");
            }
            
            String name = (String) metadata.get("name");
            String namespace = (String) metadata.get("namespace");
            if (namespace == null && defaultNamespace != null) {
                namespace = defaultNamespace;
                metadata.put("namespace", namespace);
            }
            
            // 解析 API 版本和资源类型
            String[] groupVersion = parseGroupVersion(apiVersion);
            String group = groupVersion[0];
            String version = groupVersion[1];
            String plural = getPlural(kind);
            
            // 获取自定义对象 API 客户端
            io.kubernetes.client.openapi.apis.CustomObjectsApi customObjectsApi = getCustomObjectsApi(clientHolder);
            
            // 尝试更新资源，如果不存在则创建
            try {
                if (namespace != null) {
                    // 尝试更新命名空间作用域的资源
                    Object updateResult = customObjectsApi.replaceNamespacedCustomObject(group, version, namespace, plural, name, obj).execute();
                    if (updateResult instanceof Map) {
                        return (Map<String, Object>) updateResult;
                    } else {
                        throw new RuntimeException("Update result is not a map");
                    }
                } else {
                    // 尝试更新集群作用域的资源
                    Object updateResult = customObjectsApi.replaceClusterCustomObject(group, version, plural, name, obj).execute();
                    if (updateResult instanceof Map) {
                        return (Map<String, Object>) updateResult;
                    } else {
                        throw new RuntimeException("Update result is not a map");
                    }
                }
            } catch (Exception e) {
                // 更新失败，尝试创建
                try {
                    if (namespace != null) {
                        // 创建命名空间作用域的资源
                        Object createResult = customObjectsApi.createNamespacedCustomObject(group, version, namespace, plural, obj).execute();
                        if (createResult instanceof Map) {
                            return (Map<String, Object>) createResult;
                        } else {
                            throw new RuntimeException("Create result is not a map");
                        }
                    } else {
                        // 创建集群作用域的资源
                        Object createResult = customObjectsApi.createClusterCustomObject(group, version, plural, obj).execute();
                        if (createResult instanceof Map) {
                            return (Map<String, Object>) createResult;
                        } else {
                            throw new RuntimeException("Create result is not a map");
                        }
                    }
                } catch (Exception createException) {
                    throw new RuntimeException("创建资源失败: " + createException.getMessage(), createException);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("创建或更新资源失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除单个对象：按 apiVersion/kind/name/namespace
     */
    private static Map<String, Object> delete(PodsTool.K8sClientHolder clientHolder, Map<String, Object> obj, String defaultNamespace) {
        ensureK8sAvailable();
        try {
            // 提取资源信息
            String apiVersion = (String) obj.get("apiVersion");
            String kind = (String) obj.get("kind");
            Map<String, Object> metadata = (Map<String, Object>) obj.get("metadata");
            
            if (apiVersion == null || kind == null || metadata == null) {
                throw new RuntimeException("无效的资源定义：缺少 apiVersion、kind 或 metadata");
            }
            
            String name = (String) metadata.get("name");
            String namespace = (String) metadata.get("namespace");
            if (namespace == null && defaultNamespace != null) {
                namespace = defaultNamespace;
            }
            
            // 解析 API 版本和资源类型
            String[] groupVersion = parseGroupVersion(apiVersion);
            String group = groupVersion[0];
            String version = groupVersion[1];
            String plural = getPlural(kind);
            
            // 获取自定义对象 API 客户端
            io.kubernetes.client.openapi.apis.CustomObjectsApi customObjectsApi = getCustomObjectsApi(clientHolder);
            
            // 删除资源
            if (namespace != null) {
                // 删除命名空间作用域的资源
                Object deleteResult = customObjectsApi.deleteNamespacedCustomObject(group, version, namespace, plural, name).execute();
                if (deleteResult instanceof Map) {
                    return (Map<String, Object>) deleteResult;
                } else {
                    throw new RuntimeException("Delete result is not a map");
                }
            } else {
                // 删除集群作用域的资源
                Object deleteResult = customObjectsApi.deleteClusterCustomObject(group, version, plural, name).execute();
                if (deleteResult instanceof Map) {
                    return (Map<String, Object>) deleteResult;
                } else {
                    throw new RuntimeException("Delete result is not a map");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("删除资源失败: " + e.getMessage(), e);
        }
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
     * 获取自定义对象 API 客户端实例
     */
    private static io.kubernetes.client.openapi.apis.CustomObjectsApi getCustomObjectsApi(PodsTool.K8sClientHolder clientHolder) {
        return new io.kubernetes.client.openapi.apis.CustomObjectsApi(clientHolder.getApiClient());
    }

    /**
     * 获取资源类型的复数形式
     */
    private static String getPlural(String kind) {
        return kind.toLowerCase() + "s";
    }

    /**
     * 对 Secret 对象进行敏感字段掩码处理
     */
    private static Map<String, Object> maskSecret(Map<String, Object> obj) {
        if (obj == null || !obj.containsKey("kind")) {
            return obj;
        }
        if (!"Secret".equals(obj.get("kind"))) {
            return obj;
        }
        if (obj.containsKey("data") && obj.get("data") instanceof Map) {
            Map<?, ?> data = (Map<?, ?>) obj.get("data");
            for (Object key : data.keySet()) {
                obj.put(key.toString(), "***");
            }
        }
        if (obj.containsKey("stringData") && obj.get("stringData") instanceof Map) {
            Map<?, ?> stringData = (Map<?, ?>) obj.get("stringData");
            for (Object key : stringData.keySet()) {
                obj.put(key.toString(), "***");
            }
        }
        return obj;
    }

    /**
     * 渲染模板并应用资源到集群（创建或更新）
     */
    @MCPTool(name = "helm_template_apply",
            description = "Render template with values and apply resources to the cluster (create or patch)")
    public static List<Map<String, Object>> helmTemplateApply(
            @ToolParam(description = "Jinja2 模板字符串（支持多文档 YAML，通过 '---' 分隔）")
            String template,
            @ToolParam(description = "模板渲染的变量字典")
            Map<String, Object> values,
            @ToolParam(description = "默认命名空间（当文档未指定 metadata.namespace 时使用）")
            String namespace,
            @ToolParam(description = "kubeconfig 上下文；默认当前上下文")
            String context) {
        // 安全保护：只读或禁破坏时拒绝写操作
        if (KubernetesMcpServer.isReadOnly() || KubernetesMcpServer.isDisableDestructive()) {
            throw new RuntimeException("写操作被禁止：当前处于只读或禁破坏模式");
        }

        List<Map<String, Object>> docs = renderToDocuments(template, values);
        PodsTool.K8sClientHolder clientHolder = loadKubeConfig(context);

        List<Map<String, Object>> results = new java.util.ArrayList<>();
        for (Map<String, Object> doc : docs) {
            try {
                results.add(createOrPatch(clientHolder, doc, namespace));
            } catch (Exception e) {
                throw new RuntimeException(String.format("应用资源失败（%s %s）: %s",
                        doc.get("kind"),
                        (((Map<?, ?>) doc.getOrDefault("metadata", Map.of())).get("name") != null ? ((Map<?, ?>) doc.getOrDefault("metadata", Map.of())).get("name").toString() : ""),
                        e.getMessage()), e);
            }
        }
        return results;
    }

    /**
     * 渲染模板并从集群中卸载资源
     */
    @MCPTool(name = "helm_template_uninstall",
            description = "Render template with values and uninstall rendered resources from the cluster")
    public static List<Map<String, Object>> helmTemplateUninstall(
            @ToolParam(description = "Jinja2 模板字符串（支持多文档 YAML，通过 '---' 分隔）")
            String template,
            @ToolParam(description = "模板渲染的变量字典")
            Map<String, Object> values,
            @ToolParam(description = "默认命名空间（当文档未指定 metadata.namespace 时使用）")
            String namespace,
            @ToolParam(description = "kubeconfig 上下文；默认当前上下文")
            String context) {
        // 安全保护：只读或禁破坏时拒绝删除操作
        if (KubernetesMcpServer.isReadOnly() || KubernetesMcpServer.isDisableDestructive()) {
            throw new RuntimeException("删除操作被禁止：当前处于只读或禁破坏模式");
        }

        List<Map<String, Object>> docs = renderToDocuments(template, values);
        PodsTool.K8sClientHolder clientHolder = loadKubeConfig(context);

        List<Map<String, Object>> results = new java.util.ArrayList<>();
        for (Map<String, Object> doc : docs) {
            try {
                results.add(delete(clientHolder, doc, namespace));
            } catch (Exception e) {
                throw new RuntimeException(String.format("卸载资源失败（%s %s）: %s",
                        doc.get("kind"),
                        (((Map<?, ?>) doc.getOrDefault("metadata", Map.of())).get("name") != null ? ((Map<?, ?>) doc.getOrDefault("metadata", Map.of())).get("name").toString() : ""),
                        e.getMessage()), e);
            }
        }
        return results;
    }
}
