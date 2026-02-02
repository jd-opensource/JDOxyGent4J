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
 * Provides templating capabilities without Helm binary:
 * - helm_template_apply: Render Helm-style templates (or general YAML templates) using Jinja2, Create/Patch the generated multi-document YAML to the cluster one by one
 * - helm_template_uninstall: Use the same template and values to render target objects, Delete one by one
 * - Explanation: This solution replaces direct calls to Helm CLI with the "render→K8s unified resource API" process, avoiding binary dependencies and environmental inconsistency issues
 *
 * Notes:
 * - Belongs to write/delete operations, protected by read-only and disable-destructive switches
 * - Multi-cluster support: Optional context parameter; defaults to current kubeconfig context or in-cluster configuration
 * - Template rendering input: `template` (string supporting Jinja2 syntax); `values` (dictionary)
 * - Multi-document YAML: Use `---` separator; each document must contain top-level fields apiVersion/kind/metadata.name
 */
public class HelmTools {

    // Attempt to import necessary dependencies
    private static boolean templateEngineAvailable = false;

    static {
        try {
            // Check if template engine is available
            Class.forName("org.apache.commons.text.StringSubstitutor");
            templateEngineAvailable = true;
        } catch (ClassNotFoundException e) {
            templateEngineAvailable = false;
        }
    }

    /**
     * Ensure template engine is available
     */
    private static void ensureTemplateEngine() {
        if (!templateEngineAvailable) {
            throw new RuntimeException("Template engine not installed");
        }
    }

    /**
     * Use template engine to render template, parse into multiple YAML document object list
     */
    private static List<Map<String, Object>> renderToDocuments(String template, Map<String, Object> values) {
        ensureTemplateEngine();
        try {
            // Use Apache Commons Text to render template
            StringSubstitutor substitutor = new StringSubstitutor(values);
            String renderedTemplate = substitutor.replace(template);

            // Use Yaml's loadAll method to directly parse multi-documents
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

                    // Basic field validation
                    if (doc.get("apiVersion") == null || doc.get("kind") == null) {
                        throw new RuntimeException("Rendered document missing apiVersion/kind");
                    }

                    Map<String, Object> metadata = (Map<String, Object>) doc.get("metadata");
                    if (metadata == null || metadata.get("name") == null) {
                        throw new RuntimeException("Rendered document missing metadata.name");
                    }

                    documents.add(doc);
                } else {
                    throw new RuntimeException("YAML document is not a valid object structure");
                }
            }

            return documents;
        } catch (Exception e) {
            throw new RuntimeException("Template rendering failed: " + e.getMessage(), e);
        }
    }

    /**
     * For single object: merge-patch if exists, create if not exists
     */
    private static Map<String, Object> createOrPatch(String context, Map<String, Object> obj, String defaultNamespace) throws Exception {
        PodsTool.ensureK8sAvailable();

        String apiVersion = (String) obj.get("apiVersion");
        String kind = (String) obj.get("kind");
        Map<String, Object> metadata = (Map<String, Object>) obj.get("metadata");

        if (apiVersion == null || kind == null || metadata == null) {
            throw new RuntimeException("Invalid resource definition: missing apiVersion, kind, or metadata");
        }

        String name = (String) metadata.get("name");
        String namespace = (String) metadata.get("namespace");
        if (namespace == null) {
            namespace = defaultNamespace;
        }

        // Use KubernetesDynamicClient
        KubernetesDynamicClient dyn = KubernetesDynamicClient.getDynamicClient(context);
        KubernetesDynamicClient.DynamicResource resource = dyn.resource(apiVersion, kind);

        try {
            // Try to get resource (check if exists)
            Map<String, Object> existing = resource.get(name, namespace);
            if (existing != null) {
                // Use merge-patch if exists
                return resource.patch(name, obj, namespace, "merge");
            }
        } catch (Exception e) {
            // Resource doesn't exist, ignore exception and continue to create
        }

        // Create if doesn't exist
        return resource.create(obj, namespace);
    }

    /**
     * Delete single object: by apiVersion/kind/name/namespace
     */
    private static Map<String, Object> delete(String context, Map<String, Object> obj, String defaultNamespace) throws Exception {
        PodsTool.ensureK8sAvailable();

        String apiVersion = (String) obj.get("apiVersion");
        String kind = (String) obj.get("kind");
        Map<String, Object> metadata = (Map<String, Object>) obj.get("metadata");

        if (apiVersion == null || kind == null || metadata == null) {
            throw new RuntimeException("Invalid resource definition: missing apiVersion, kind, or metadata");
        }

        String name = (String) metadata.get("name");
        String namespace = (String) metadata.get("namespace");
        if (namespace == null) {
            namespace = defaultNamespace;
        }

        // Use KubernetesDynamicClient
        KubernetesDynamicClient dyn = KubernetesDynamicClient.getDynamicClient(context);
        KubernetesDynamicClient.DynamicResource resource = dyn.resource(apiVersion, kind);

        return resource.delete(name, namespace);
    }

    /**
     * Render template and apply resources to cluster (create or update)
     */
    @MCPTool(name = "helm_template_apply",
            description = "Render template with values and apply resources to the cluster (create or patch)")
    public static List<Map<String, Object>> helmTemplateApply(
            @ToolParam(description = "Jinja2 template string (supports multi-document YAML, separated by '---')")
            String template,
            @ToolParam(description = "Variable dictionary for template rendering")
            Map<String, Object> values,
            @ToolParam(description = "Default namespace (used when document does not specify metadata.namespace)")
            String namespace,
            @ToolParam(description = "kubeconfig context; defaults to current context")
            String context) {
        // Security protection: Reject write operations when in read-only or disable-destructive mode
        if (KubernetesMcpServer.isReadOnly() || KubernetesMcpServer.isDisableDestructive()) {
            throw new RuntimeException("Write operations are prohibited: currently in read-only or disable-destructive mode");
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
                throw new RuntimeException(String.format("Failed to apply resource (%s %s): %s", kind, name, e.getMessage()), e);
            }
        }
        return results;
    }

    /**
     * Render template and uninstall resources from cluster
     */
    @MCPTool(name = "helm_template_uninstall",
            description = "Render template with values and uninstall rendered resources from the cluster")
    public static List<Map<String, Object>> helmTemplateUninstall(
            @ToolParam(description = "Jinja2 template string (supports multi-document YAML, separated by '---')")
            String template,
            @ToolParam(description = "Variable dictionary for template rendering")
            Map<String, Object> values,
            @ToolParam(description = "Default namespace (used when document does not specify metadata.namespace)")
            String namespace,
            @ToolParam(description = "kubeconfig context; defaults to current context")
            String context) {
        // Security protection: Reject delete operations when in read-only or disable-destructive mode
        if (KubernetesMcpServer.isReadOnly() || KubernetesMcpServer.isDisableDestructive()) {
            throw new RuntimeException("Delete operations are prohibited: currently in read-only or disable-destructive mode");
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
                throw new RuntimeException(String.format("Failed to uninstall resource (%s %s): %s", kind, name, e.getMessage()), e);
            }
        }
        return results;
    }
}
