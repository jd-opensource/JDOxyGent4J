package com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.core_tools;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.CoreV1Event;
import io.kubernetes.client.openapi.models.CoreV1EventList;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

public class EventsTool {

    /**
     * 事件摘要生成，与 Python 版保持一致
     */
    private static Map<String, Object> eventSummary(CoreV1Event event) {
        Map<String, Object> summary = new HashMap<>();
        // 元数据
        if (event.getMetadata() != null) {
            summary.put("name", event.getMetadata().getName());
            summary.put("namespace", event.getMetadata().getNamespace());
        }
        // 事件类型和原因
        summary.put("type", event.getType());
        summary.put("reason", event.getReason());
        summary.put("message", event.getMessage());
        summary.put("count", event.getCount());
        // 时间戳
        if (event.getFirstTimestamp() != null) {
            summary.put("firstTimestamp", event.getFirstTimestamp().toString());
        }
        if (event.getLastTimestamp() != null) {
            summary.put("lastTimestamp", event.getLastTimestamp().toString());
        }
        // 相关对象
        if (event.getInvolvedObject() != null) {
            Map<String, Object> involvedObject = new HashMap<>();
            involvedObject.put("kind", event.getInvolvedObject().getKind());
            involvedObject.put("name", event.getInvolvedObject().getName());
            involvedObject.put("namespace", event.getInvolvedObject().getNamespace());
            summary.put("involvedObject", involvedObject);
        }
        return summary;
    }

    /**
     * 列出事件 - 修正版，与 Python 版功能一致
     */
    @MCPTool(name = "events_list",
            description = "List Kubernetes events in all namespaces or a specific namespace")
    public static List<Map<String, Object>> eventsList(
            @ToolParam(description = "Optional namespace to list events from", required = false)
            String namespace,
            @ToolParam(description = "Kubeconfig context name; defaults to current context", required = false)
            String context) {
        PodsTool.K8sClientHolder clientHolder = PodsTool.loadKubeConfig(context);
        CoreV1Api coreApi = clientHolder.getCoreV1Api();
        try {
            List<Map<String, Object>> summaries = new ArrayList<>();

            if (namespace != null && !namespace.trim().isEmpty()) {
                // 获取指定命名空间的事件
                CoreV1EventList eventList = coreApi.listNamespacedEvent(namespace).execute();
                for (CoreV1Event event : eventList.getItems()) {
                    summaries.add(eventSummary(event));
                }
            } else {
                // 获取所有命名空间的事件
                CoreV1EventList eventList = coreApi.listEventForAllNamespaces().execute();
                for (CoreV1Event event : eventList.getItems()) {
                    summaries.add(eventSummary(event));
                }
            }
            return summaries;
        } catch (Exception e) {
            throw new RuntimeException("Failed to list events: " + e.getMessage(), e);
        }
    }
}