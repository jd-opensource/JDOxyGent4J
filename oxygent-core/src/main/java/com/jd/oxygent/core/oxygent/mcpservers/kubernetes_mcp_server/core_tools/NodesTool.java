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
 * 提供节点相关只读与监测能力：
 * - nodes_top：通过 metrics.k8s.io/v1beta1 获取节点 CPU/内存用量（需部署 Metrics Server）
 * - nodes_stats_summary：通过 apiserver→kubelet Summary API 获取节点详细资源统计（含 CPU/内存/文件系统/网络等）
 * - nodes_log：通过 apiserver→kubelet logs 代理获取节点日志（如 kubelet、kube-proxy 或指定日志文件路径）
 *
 * 注意：
 * - 所有能力均为只读；如需进一步扩展请在只读与禁破坏开关下审慎添加。
 */
public class NodesTool {

    /**
     * 生成节点摘要信息
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
     * 列出所有节点
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
     * 获取指定节点的完整对象
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
            // 使用 Jackson 将 Node 对象转换为 Map
            ObjectMapper mapper = new ObjectMapper();
            return mapper.convertValue(node, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get node", e);
        }
    }

    /**
     * 从 metrics.k8s.io 读取节点资源使用情况
     * 优先尝试 metrics.k8s.io v1（若可用），否则回退到 v1beta1。
     *     返回示例（简化）：
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
            @ToolParam(description = "Label selector to filter nodes （例如 'node-role.kubernetes.io/worker='）")
            String labelSelector,
            @ToolParam(description = "Kubeconfig context name; defaults to current context")
            String context) {

        try {
            CustomObjectsApi customApi =  PodsTool.loadKubeConfig(context).getCustomObjectsApi();

            String group = "metrics.k8s.io";
            String[] versions = {"v1", "v1beta1"}; // 优先 v1，失败回退 v1beta1
            String plural = "nodes";

            List<Map<String, Object>> result = new ArrayList<>();
            Exception lastError = null;

            // 尝试不同版本的 API
            for (String version : versions) {
                try {
                    // 获取节点指标
                    Object metricsData = customApi.listClusterCustomObject(group, version, plural).labelSelector(labelSelector).execute();
                    // 处理返回的数据
                    if (metricsData instanceof Map) {
                        Map<String, Object> dataMap = (Map<String, Object>) metricsData;
                        for (Map item : (List<Map>) dataMap.get("items")) {
                                Map<String, Object> metadata = (Map<String, Object>) item.get("metadata");
                                // 过滤节点名称
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
                    // 继续尝试下一个版本
                }
            }

            // 如果所有版本都失败，抛出异常
            if (result.isEmpty() && lastError != null) {
                throw new RuntimeException("无法获取节点监控指标（metrics.k8s.io v1/v1beta1 均不可用，可能未部署 Metrics Server）：" + lastError.getMessage(), lastError);
            }
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get node metrics", e);
        }
    }

    /**
     * 通过 kubelet Summary API 获取节点详细资源统计
     *       GET /api/v1/nodes/{name}/proxy/stats/summary
     *
     *     返回字典结构，包含 node/pod/container 层面的 CPU/Memory/FS/Network 等度量。
     */
    @MCPTool(name = "nodes_stats_summary",
            description = "Get detailed resource stats from a Kubernetes node via kubelet Summary API")
    public static Map<String, Object> nodesStatsSummary(
            @ToolParam(description = "节点名称")
            String name,
            @ToolParam(description = "Kubeconfig context name; defaults to current context")
            String context) {

        try {
            CoreV1Api coreV1Api = PodsTool.loadKubeConfig(context).getCoreV1Api();
            String path = "stats/summary";

            // 调用 kubelet Summary API
            String response = coreV1Api.connectGetNodeProxyWithPath(name, path).execute();

            // 使用 Jackson 将 JSON 响应转换为 Map
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.readValue(response, Map.class);
            } catch (Exception e) {
                // 返回原始字符串（不可解析时）
                Map<String, Object> result = new java.util.HashMap<>();
                result.put("raw", response);
                return result;
            }
        } catch (Exception e) {
            throw new RuntimeException("获取节点 Summary 失败（name=" + name + "）：" + e.getMessage(), e);
        }
    }

    /**
     * 通过 apiserver 代理到 kubelet 获取节点日志
     *    使用 CoreV1Api 的 node 代理：
     *      GET /api/v1/nodes/{name}/proxy/{path}
     *
     *    注意：不同集群的 kubelet 代理可用性与日志路径可能存在差异；本实现提供通用路径映射，并在包含查询参数失败时回退到不带查询参数。
     *
     */
    @MCPTool(name = "nodes_log",
            description = "Get logs from a Kubernetes node via apiserver proxy to kubelet")
    public static String nodesLog(
            @ToolParam(description = "节点名称")
            String name,
            @ToolParam(description = "日志来源或文件路径：'kubelet'、'kube-proxy' 或 '/var/log/xxx.log' 等")
            String query,
            @ToolParam(description = "尾部行数（若 kubelet 支持，则作为附加参数；默认 0 表示全部")
            int tailLines,
            @ToolParam(description = "Kubeconfig context name; defaults to current context")
            String context) {

        try {
            CoreV1Api coreV1Api = PodsTool.loadKubeConfig(context).getCoreV1Api();
            String path = resolveLogPath(query);

            // 优先尝试带 tailLines 参数（若提供）
            String finalPath = tailLines <= 0 ? path : path + "?tailLines=" + tailLines;
            try {
                return coreV1Api.connectGetNodeProxyWithPath(name, finalPath).execute();
            } catch (Exception e1) {
                // 回退到不带查询参数
                try {
                    return coreV1Api.connectGetNodeProxyWithPath(name, path).execute();
                } catch (Exception e2) {
                    throw new RuntimeException("获取节点日志失败（name=" + name + ", path=" + path + "），尝试带/不带查询参数均失败：" + e1.getMessage() + " | " + e2.getMessage(), e2);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("获取节点日志失败：" + e.getMessage(), e);
        }
    }

    /**
     * 解析日志路径
     *     将用户友好的 query 转换为 kubelet 代理路径：
     *     - 'kubelet' => 'logs/kubelet.log'
     *     - 'kube-proxy' => 'logs/kube-proxy.log'
     *     - 以 '/' 开头的绝对文件路径 => 'logs{absolute_path}'（例如 '/var/log/kubelet.log' => 'logs/var/log/kubelet.log'）
     *     - 其他 => 'logs/{query}'（相对路径或文件名）
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