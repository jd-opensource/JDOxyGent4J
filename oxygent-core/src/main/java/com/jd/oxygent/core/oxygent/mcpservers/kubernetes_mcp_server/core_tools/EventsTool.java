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

/**
 * Kubernetes MCP Server - core tools: events
 *
 * Provides read-only capabilities for Kubernetes events:
 * - events_list: List events in all namespaces or a specific namespace
 */
public class EventsTool {

    private static Map<String, Object> eventSummary(CoreV1Event event) {
        Map<String, Object> summary = new HashMap<>();
        // Metadata
        if (event.getMetadata() != null) {
            summary.put("name", event.getMetadata().getName());
            summary.put("namespace", event.getMetadata().getNamespace());
        }
        // Event type and reason
        summary.put("type", event.getType());
        summary.put("reason", event.getReason());
        summary.put("message", event.getMessage());
        summary.put("count", event.getCount());
        // Timestamp
        if (event.getFirstTimestamp() != null) {
            summary.put("firstTimestamp", event.getFirstTimestamp().toString());
        }
        if (event.getLastTimestamp() != null) {
            summary.put("lastTimestamp", event.getLastTimestamp().toString());
        }
        // Involved object
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
     * List events - corrected version, consistent with Python version
     */
    @MCPTool(name = "events_list",
            description = "List Kubernetes events in all namespaces or a specific namespace")
    public static List<Map<String, Object>> eventsList(
            @ToolParam(description = "Optional namespace to list events from", required = false)
            String namespace,
            @ToolParam(description = "Kubeconfig context name; defaults to current context", required = false)
            String context) {
        CoreV1Api coreApi = PodsTool.loadKubeConfig(context).getCoreV1Api();
        try {
            List<Map<String, Object>> summaries = new ArrayList<>();

            if (namespace != null && !namespace.trim().isEmpty()) {
                // Get events from specified namespace
                CoreV1EventList eventList = coreApi.listNamespacedEvent(namespace).execute();
                for (CoreV1Event event : eventList.getItems()) {
                    summaries.add(eventSummary(event));
                }
            } else {
                // Get events from all namespaces
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