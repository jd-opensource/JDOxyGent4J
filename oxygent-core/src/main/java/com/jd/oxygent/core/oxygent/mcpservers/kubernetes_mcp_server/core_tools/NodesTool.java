package com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.core_tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Kubernetes MCP Server - core tools: nodes
 *
 * Provides node-related read-only and monitoring capabilities:
 * - nodes_top: Get node CPU/memory usage via metrics.k8s.io/v1beta1 (requires Metrics Server deployment)
 * - nodes_stats_summary: Get detailed node resource statistics via apiserver→kubelet Summary API (including CPU/memory/filesystem/network, etc.)
 * - nodes_log: Get node logs via apiserver→kubelet logs proxy (such as kubelet, kube-proxy, or specified log file paths)
 *
 * Note:
 * - All capabilities are read-only; if further expansion is needed, add them cautiously under read-only and disable-destructive switches.
 */
public class NodesTool {

    /**
     * Generate node summary information
     */
    private static Map<String, Object> nodeSummary(V1Node node) {
        Map<String, Object> summary = new java.util.HashMap<>();
        
        if (node.getMetadata() != null) {
            summary.put("name", node.getMetadata().getName());
            summary.put("labels", node.getMetadata().getLabels());
            summary.put("annotations", node.getMetadata().getAnnotations());
        }
        
        if (node.getStatus() != null) {
            summary.put("phase", node.getStatus().getPhase());
            summary.put("conditions", node.getStatus().getConditions());
            summary.put("addresses", node.getStatus().getAddresses());
            if (node.getStatus().getCapacity() != null) {
                summary.put("capacity", node.getStatus().getCapacity());
            }
            if (node.getStatus().getAllocatable() != null) {
                summary.put("allocatable", node.getStatus().getAllocatable());
            }
        }
        
        return summary;
    }

    /**
     * List all nodes
     */
    @MCPTool(name = "nodes_list",
            description = "List the resource consumption (CPU/memory) for Nodes via metrics API (v1 fallback to v1beta1)")
    public static List<Map<String, Object>> nodesList(
            @ToolParam(description = "Label selector to filter nodes")
            String labelSelector,
            @ToolParam(description = "Kubeconfig context name; defaults to current context")
            String context) {
        try {
            CoreV1Api coreV1Api = PodsTool.loadKubeConfig(context).getCoreV1Api();
            V1NodeList nodeList = coreV1Api.listNode().execute();
            
            List<Map<String, Object>> summaries = new java.util.ArrayList<>();
            for (V1Node node : nodeList.getItems()) {
                summaries.add(nodeSummary(node));
            }
            return summaries;
        } catch (Exception e) {
            throw new RuntimeException("Failed to list nodes", e);
        }
    }

    /**
     * Get the complete object of a specified node
     */
    @MCPTool(name = "nodes_get",
            description = "Get a Kubernetes node by name")
    public static Map<String, Object> nodesGet(
            @ToolParam(description = "Node name")
            String name,
            @ToolParam(description = "Kubeconfig context name; defaults to current context")
            String context) {

        try {
            CoreV1Api coreV1Api = PodsTool.loadKubeConfig(context).getCoreV1Api();
            V1Node node = coreV1Api.readNode(name).execute();
            // Use Jackson to convert Node object to Map
            ObjectMapper mapper = new ObjectMapper();
            return mapper.convertValue(node, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get node", e);
        }
    }

    /**
     * Read node resource usage from metrics.k8s.io
     * Prefer metrics.k8s.io v1 (if available), otherwise fall back to v1beta1.
     *     Return example (simplified):
     *     [
     *       {"name":"worker-1","usage":{"cpu":"50m","memory":"1024Mi"}, "timestamp":"...", "window":"..."},
     *       ...
     *     ]
     */
    @MCPTool(name = "nodes_top",
            description = "List the resource consumption (CPU/memory) for Nodes via metrics API (v1 fallback to v1beta1)")
    public static List<Map<String, Object>> nodesTop(
            @ToolParam(description = "Specific node name to filter")
            String name,
            @ToolParam(description = "Label selector to filter nodes (e.g. 'node-role.kubernetes.io/worker=')")
            String labelSelector,
            @ToolParam(description = "Kubeconfig context name; defaults to current context")
            String context) {

        try {
            CustomObjectsApi customApi =  PodsTool.loadKubeConfig(context).getCustomObjectsApi();

            String group = "metrics.k8s.io";
            String[] versions = {"v1", "v1beta1"}; // Prefer v1, fall back to v1beta1 on failure
            String plural = "nodes";

            List<Map<String, Object>> result = new ArrayList<>();
            Exception lastError = null;

            // Try different versions of the API
            for (String version : versions) {
                try {
                    // Get node metrics
                    Object metricsData = customApi.listClusterCustomObject(group, version, plural).labelSelector(labelSelector).execute();
                    // Process returned data
                    if (metricsData instanceof Map) {
                        Map<String, Object> dataMap = (Map<String, Object>) metricsData;
                        for (Map item : (List<Map>) dataMap.get("items")) {
                                Map<String, Object> metadata = (Map<String, Object>) item.get("metadata");
                                // Filter node names
                                if (name != null && !name.isEmpty()) {
                                    if (!name.equals(metadata.get("name"))) {
                                        continue;
                                    }
                                }
                                result.add(Map.of("name",metadata.get("name"),
                                        "usage",item.get("usage"),
                                        "timestamp",item.get("timestamp"),
                                        "window",item.get("window")));
                        }
                    }
                   break;
                } catch (Exception e) {
                    lastError = e;
                    // Continue trying the next version
                }
            }

            // If all versions fail, throw an exception
            if (result.isEmpty() && lastError != null) {
                throw new RuntimeException("Unable to get node metrics (both metrics.k8s.io v1/v1beta1 unavailable, Metrics Server may not be deployed): " + lastError.getMessage(), lastError);
            }
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get node metrics", e);
        }
    }

    /**
     * Get detailed node resource statistics via kubelet Summary API
     *       GET /api/v1/nodes/{name}/proxy/stats/summary
     *
     *     Returns dictionary structure containing CPU/Memory/FS/Network metrics at node/pod/container level.
     */
    @MCPTool(name = "nodes_stats_summary",
            description = "Get detailed resource stats from a Kubernetes node via kubelet Summary API")
    public static Map<String, Object> nodesStatsSummary(
            @ToolParam(description = "Node name")
            String name,
            @ToolParam(description = "Kubeconfig context name; defaults to current context")
            String context) {

        try {
            CoreV1Api coreV1Api = PodsTool.loadKubeConfig(context).getCoreV1Api();
            String path = "stats/summary";

            // Call kubelet Summary API
            String response = coreV1Api.connectGetNodeProxyWithPath(name, path).execute();

            // Use Jackson to convert JSON response to Map
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.readValue(response, Map.class);
            } catch (Exception e) {
                // Return raw string (when not parseable)
                Map<String, Object> result = new java.util.HashMap<>();
                result.put("raw", response);
                return result;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get node Summary (name=" + name + "): " + e.getMessage(), e);
        }
    }

    /**
     * Get node logs via apiserver proxy to kubelet
     *    Using CoreV1Api's node proxy:
     *      GET /api/v1/nodes/{name}/proxy/{path}
     *
     *    Note: Different clusters may have differences in kubelet proxy availability and log paths; this implementation provides universal path mapping and falls back to without query parameters when query parameters fail.
     *
     */
    @MCPTool(name = "nodes_log",
            description = "Get logs from a Kubernetes node via apiserver proxy to kubelet")
    public static String nodesLog(
            @ToolParam(description = "Node name")
            String name,
            @ToolParam(description = "Log source or file path: 'kubelet', 'kube-proxy', or '/var/log/xxx.log', etc.")
            String query,
            @ToolParam(description = "Tail lines (if supported by kubelet, used as additional parameter; default 0 means all")
            int tailLines,
            @ToolParam(description = "Kubeconfig context name; defaults to current context")
            String context) {

        try {
            CoreV1Api coreV1Api = PodsTool.loadKubeConfig(context).getCoreV1Api();
            String path = resolveLogPath(query);

            // Prefer attempting with tailLines parameter (if provided)
            String finalPath = tailLines <= 0 ? path : path + "?tailLines=" + tailLines;
            try {
                return coreV1Api.connectGetNodeProxyWithPath(name, finalPath).execute();
            } catch (Exception e1) {
                // Fall back to without query parameters
                try {
                    return coreV1Api.connectGetNodeProxyWithPath(name, path).execute();
                } catch (Exception e2) {
                    throw new RuntimeException("Failed to get node logs (name=" + name + ", path=" + path + "), attempts with and without query parameters failed: " + e1.getMessage() + " | " + e2.getMessage(), e2);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get node logs: " + e.getMessage(), e);
        }
    }

    /**
     * Parse log path
     *     Convert user-friendly query to kubelet proxy path:
     *     - 'kubelet' => 'logs/kubelet.log'
     *     - 'kube-proxy' => 'logs/kube-proxy.log'
     *     - Absolute file path starting with '/' => 'logs{absolute_path}' (e.g. '/var/log/kubelet.log' => 'logs/var/log/kubelet.log')
     *     - Others => 'logs/{query}' (relative path or filename)
     */
    private static String resolveLogPath(String query) {
        String q = (query != null) ? query.strip() : "";
        if ("kubelet".equals(q)) {
            return "logs/kubelet.log";
        }
        if ("kube-proxy".equals(q)) {
            return "logs/kube-proxy.log";
        }
        if (q.startsWith("/")) {
            return "logs" + q;
        }
        return "logs/" + q;
    }
}