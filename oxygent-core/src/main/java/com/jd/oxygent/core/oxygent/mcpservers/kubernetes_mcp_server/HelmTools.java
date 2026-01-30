package com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.core_tools.PodsTool;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.util.Yaml;
import org.apache.commons.text.StringSubstitutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Kubernetes MCP Server - helm tools (template-first approach)
 * <p>
 * 提供无需 Helm 二进制的模板化能力：
 * - helm_template_apply：使用 Jinja2 渲染 Helm 风格模板（或通用 YAML 模板），将生成的多文档 YAML 逐条 Create/Patch 到集群
 * - helm_template_uninstall：使用同一模板与 values 渲染出目标对象，逐条 Delete
 * - 说明：此方案以“渲染→K8s 统一资源 API”的流程替代直接调用 Helm CLI，规避二进制依赖与环境不一致问题
 *
 * 注意：
 * - 属于写/删除操作，受只读与禁破坏开关保护
 * - 多集群支持：可选 context 参数；默认使用当前 kubeconfig 上下文或 in-cluster 配置
 * - 模板渲染输入：`template`（字符串，支持 Jinja2 语法）；`values`（字典）
 * - 多文档 YAML：使用 `---` 分隔；每个文档需包含 apiVersion/kind/metadata.name 顶级字段
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
     * 确保模板引擎可用
     */
    private static void ensureTemplateEngine() {
        if (!templateEngineAvailable) {
            throw new RuntimeException("模板引擎未安装");
        }
    }

    /**
     * 使用模板引擎渲染模板，解析为多 YAML 文档对象列表
     */
    private static List<Map<String, Object>> renderToDocuments(String template, Map<String, Object> values) {
        ensureTemplateEngine();
        try {
            // 使用 Apache Commons Text 渲染模板
            StringSubstitutor substitutor = new StringSubstitutor(values);
            String renderedTemplate = substitutor.replace(template);

            // 使用 Yaml 的 loadAll 方法直接解析多文档
            Yaml yaml = new Yaml();
            List<Map<String, Object>> documents = new ArrayList<>();

            Iterable<Object> yamlDocs = yaml.loadAll(renderedTemplate);
            for (Object yamlDoc : yamlDocs) {
                if (yamlDoc == null) {
                    continue;
                }

                if (yamlDoc instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> doc = (Map<String, Object>) yamlDoc;

                    // 基本字段校验
                    if (doc.get("apiVersion") == null || doc.get("kind") == null) {
                        throw new RuntimeException("渲染文档缺少 apiVersion/kind");
                    }

                    Map<String, Object> metadata = (Map<String, Object>) doc.get("metadata");
                    if (metadata == null || metadata.get("name") == null) {
                        throw new RuntimeException("渲染文档缺少 metadata.name");
                    }

                    documents.add(doc);
                } else {
                    throw new RuntimeException("YAML 文档不是有效的对象结构");
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
    private static Map<String, Object> createOrPatch(String context, Map<String, Object> obj, String defaultNamespace) throws Exception {
        PodsTool.ensureK8sAvailable();

        String apiVersion = (String) obj.get("apiVersion");
        String kind = (String) obj.get("kind");
        Map<String, Object> metadata = (Map<String, Object>) obj.get("metadata");

        if (apiVersion == null || kind == null || metadata == null) {
            throw new RuntimeException("无效的资源定义：缺少 apiVersion、kind 或 metadata");
        }

        String name = (String) metadata.get("name");
        String namespace = (String) metadata.get("namespace");
        if (namespace == null) {
            namespace = defaultNamespace;
        }

        // 使用 KubernetesDynamicClient
        KubernetesDynamicClient dyn = KubernetesDynamicClient.getDynamicClient(context);
        KubernetesDynamicClient.DynamicResource resource = dyn.resource(apiVersion, kind);

        try {
            // 尝试获取资源（检查是否存在）
            Map<String, Object> existing = resource.get(name, namespace);
            if (existing != null) {
                // 存在则使用 merge-patch
                return resource.patch(name, obj, namespace, "merge");
            }
        } catch (Exception e) {
            // 资源不存在，忽略异常继续创建
        }

        // 不存在则创建
        return resource.create(obj, namespace);
    }

    /**
     * 删除单个对象：按 apiVersion/kind/name/namespace
     */
    private static Map<String, Object> delete(String context, Map<String, Object> obj, String defaultNamespace) throws Exception {
        PodsTool.ensureK8sAvailable();

        String apiVersion = (String) obj.get("apiVersion");
        String kind = (String) obj.get("kind");
        Map<String, Object> metadata = (Map<String, Object>) obj.get("metadata");

        if (apiVersion == null || kind == null || metadata == null) {
            throw new RuntimeException("无效的资源定义：缺少 apiVersion、kind 或 metadata");
        }

        String name = (String) metadata.get("name");
        String namespace = (String) metadata.get("namespace");
        if (namespace == null) {
            namespace = defaultNamespace;
        }

        // 使用 KubernetesDynamicClient
        KubernetesDynamicClient dyn = KubernetesDynamicClient.getDynamicClient(context);
        KubernetesDynamicClient.DynamicResource resource = dyn.resource(apiVersion, kind);

        return resource.delete(name, namespace);
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
        List<Map<String, Object>> results = new ArrayList<>();

        for (Map<String, Object> doc : docs) {
            try {
                Map<String, Object> result = createOrPatch(context, doc, namespace);
                results.add(KubernetesDynamicClient.maskSecret(result));
            } catch (Exception e) {
                String kind = (String) doc.get("kind");
                String name = "unknown";
                Map<String, Object> metadata = (Map<String, Object>) doc.get("metadata");
                if (metadata != null && metadata.get("name") != null) {
                    name = (String) metadata.get("name");
                }
                throw new RuntimeException(String.format("应用资源失败（%s %s）: %s", kind, name, e.getMessage()), e);
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
        List<Map<String, Object>> results = new ArrayList<>();

        for (Map<String, Object> doc : docs) {
            try {
                Map<String, Object> result = delete(context, doc, namespace);
                results.add(KubernetesDynamicClient.maskSecret(result));
            } catch (Exception e) {
                String kind = (String) doc.get("kind");
                String name = "unknown";
                Map<String, Object> metadata = (Map<String, Object>) doc.get("metadata");
                if (metadata != null && metadata.get("name") != null) {
                    name = (String) metadata.get("name");
                }
                throw new RuntimeException(String.format("卸载资源失败（%s %s）: %s", kind, name, e.getMessage()), e);
            }
        }
        return results;
    }
}
