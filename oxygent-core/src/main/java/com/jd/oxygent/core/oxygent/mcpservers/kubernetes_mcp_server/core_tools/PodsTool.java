package com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.core_tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.models.V1ExecAction;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Streams;
import io.kubernetes.client.util.WebSocketStreamHandler;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Kubernetes MCP Server - core tools: pods
 * <p>
 * Provides common read-only capabilities related to Pods:
 * - pods_list: List Pods in all namespaces
 * - pods_list_in_namespace: List Pods in a specified namespace
 * - pods_get: Get the complete object of a specified Pod
 * - pods_log: Get Pod logs (supports container selection, previous, tail)
 * - pods_exec: Execute command in Pod container and return output
 * - pods_top: Read Pod resource usage from metrics.k8s.io (requires Metrics Server deployment)
 */
public class PodsTool {

    // Attempt to import Kubernetes Java client
    private static boolean k8sAvailable = false;

    static {
        try {
            // Check if Kubernetes Java client is available
            Class.forName("io.kubernetes.client.openapi.ApiClient");
            k8sAvailable = true;
        } catch (ClassNotFoundException e) {
            k8sAvailable = false;
        }
    }

    /**
     * Ensure Kubernetes client is available
     */
    public static void ensureK8sAvailable() {
        if (!k8sAvailable) {
            throw new RuntimeException("Kubernetes Java client not installed");
        }
    }

    /**
     * Load Kubernetes configuration
     */
    public static K8sClientHolder loadKubeConfig(String context) {
        ensureK8sAvailable();
        try {
            return K8sClientHolder.create(context);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load kubeconfig", e);
        }
    }

    /**
     * Generate Pod summary information
     */
    private static Map<String, Object> podSummary(V1Pod pod) {
        Map<String, Object> summary = new java.util.HashMap<>();
        
        if (pod.getMetadata() != null) {
            summary.put("name", pod.getMetadata().getName());
            summary.put("namespace", pod.getMetadata().getNamespace());
            summary.put("labels", pod.getMetadata().getLabels());
        }
        
        if (pod.getSpec() != null) {
            summary.put("nodeName", pod.getSpec().getNodeName());
        }
        
        if (pod.getStatus() != null) {
            summary.put("phase", pod.getStatus().getPhase());
            summary.put("hostIP", pod.getStatus().getHostIP());
            summary.put("podIP", pod.getStatus().getPodIP());
            if (pod.getStatus().getStartTime() != null) {
                summary.put("startTime", pod.getStatus().getStartTime().toInstant().toString());
            }
        }
        
        return summary;
    }

    /**
     * List Pods in all namespaces
     */
    @MCPTool(name = "pods_list",
            description = "List all the Kubernetes pods in the current cluster from all namespaces")
    public static List<Map<String, Object>> podsList(
            @ToolParam(description = "Kubernetes label selector, e.g. 'app=myapp,env=prod'")
            String labelSelector,
            @ToolParam(description = "Kubeconfig context name; defaults to current context")
            String context) {
        try {
            CoreV1Api coreV1Api = loadKubeConfig(context).getCoreV1Api();
            V1PodList podList = coreV1Api.listPodForAllNamespaces()
                    .labelSelector(labelSelector)
                    .execute();
            
            List<Map<String, Object>> summaries = new ArrayList<>();
            for (V1Pod pod : podList.getItems()) {
                summaries.add(podSummary(pod));
            }
            return summaries;
        } catch (Exception e) {
            throw new RuntimeException("Failed to list pods", e);
        }
    }

    /**
     * List Pods in a specified namespace
     */
    @MCPTool(name = "pods_list_in_namespace",
            description = "List all the Kubernetes pods in the specified namespace")
    public static List<Map<String, Object>> podsListInNamespace(
            @ToolParam(description = "Namespace to list pods from")
            String namespace,
            @ToolParam(description = "Kubernetes label selector, e.g. 'app=myapp'")
            String labelSelector,
            @ToolParam(description = "Kubeconfig context name; defaults to current context")
            String context) {
        try {
            CoreV1Api coreV1Api = loadKubeConfig(context).getCoreV1Api();
            V1PodList podList = coreV1Api.listNamespacedPod(namespace)
                    .labelSelector(labelSelector)
                    .execute();
            
            List<Map<String, Object>> summaries = new ArrayList<>();
            for (V1Pod pod : podList.getItems()) {
                summaries.add(podSummary(pod));
            }

            return summaries;
        } catch (Exception e) {
            throw new RuntimeException("Failed to list pods in namespace", e);
        }
    }

    /**
     * Get the complete object of a specified Pod
     */
    @MCPTool(name = "pods_get",
            description = "Get a Kubernetes Pod by name in the provided namespace")
    public static Map<String, Object> podsGet(
            @ToolParam(description = "Pod name")
            String name,
            @ToolParam(description = "Namespace of the Pod")
            String namespace,
            @ToolParam(description = "Kubeconfig context name; defaults to current context")
            String context) {
        try {
            CoreV1Api coreV1Api = loadKubeConfig(context).getCoreV1Api();
            V1Pod pod = coreV1Api.readNamespacedPod(name, namespace).execute();

            // Use Jackson to convert Pod object to Map
            ObjectMapper mapper = new ObjectMapper();
            return mapper.convertValue(pod, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get pod", e);
        }
    }

    /**
     * Get Pod logs
     */
    @MCPTool(name = "pods_log",
            description = "Get logs of a Kubernetes Pod")
    public static String podsLog(
            @ToolParam(description = "Pod name")
            String name,
            @ToolParam(description = "Namespace of the Pod")
            String namespace,
            @ToolParam(description = "Container name in the Pod")
            String container,
            @ToolParam(description = "Return previous terminated container logs")
            boolean previous,
            @ToolParam(description = "Number of lines to retrieve from end; 0 to get all")
            int tail,
            @ToolParam(description = "Kubeconfig context name; defaults to current context")
            String context) {
        try {
            CoreV1Api coreV1Api = loadKubeConfig(context).getCoreV1Api();
            // Call API to get logs
            CoreV1Api.APIreadNamespacedPodLogRequest request = coreV1Api.readNamespacedPodLog(name, namespace);
            if (container != null && !container.isEmpty()) {
                request.container(container);
            }
            if (tail > 0) {
                request.tailLines(tail);
            }
            if (previous) {
                request.previous(true);
            }
            return request.execute();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get pod logs", e);
        }
    }

    /**
     * Execute command in Pod container and return output
     */
    @MCPTool(name = "pods_exec",
            description = "Execute a command in a Kubernetes Pod container and return the output (combined stdout/stderr)")
    public static String podsExec(
            @ToolParam(description = "Command array, e.g. ['ls','-l','/']")
            List<String> command,
            @ToolParam(description = "Pod name")
            String name,
            @ToolParam(description = "Namespace of the Pod")
            String namespace,
            @ToolParam(description = "Container name; default first container")
            String container,
            @ToolParam(description = "Kubeconfig context name; defaults to current context")
            String context) {
        try {
            CoreV1Api coreV1Api = loadKubeConfig(context).getCoreV1Api();
            
            // Create exec request
            V1ExecAction execAction = new V1ExecAction();
                         execAction.setCommand(command);

            // Define command to execute
            String commands = String.join("",command);

            return coreV1Api.connectGetNamespacedPodExec(name, namespace).command(commands).container(container).execute();

        } catch (Exception e) {
            throw new RuntimeException("Failed to execute command in pod", e);
        }
    }

    /**
     * Read Pod resource usage from metrics.k8s.io
     */
    @MCPTool(name = "pods_top",
            description = "List the resource consumption (CPU/memory) for Pods via metrics API (v1 fallback to v1beta1)")
    public static List<Map<String, Object>> podsTop(
            @ToolParam(description = "Namespace to get metrics from; all namespaces if omitted")
            String namespace,
            @ToolParam(description = "Specific Pod name to filter")
            String name,
            @ToolParam(description = "Label selector to filter pods")
            String labelSelector,
            @ToolParam(description = "Kubeconfig context name; defaults to current context")
            String context) {

        try {
            CustomObjectsApi customApi = loadKubeConfig(context).getCustomObjectsApi();
            
            String group = "metrics.k8s.io";
            String[] versions = {"v1", "v1beta1"}; // Prefer v1, fall back to v1beta1 on failure
            String plural = "pods";
            
            List<Map<String, Object>> result = new ArrayList<>();
            Exception lastError = null;
            
            // Try different versions of the API
            for (String version : versions) {
                try {
                    Object metricsData;
                    if (namespace != null && !namespace.isEmpty()) {
                        // Get Pod metrics for specified namespace
                        metricsData = customApi.listNamespacedCustomObject(group, version, namespace, plural).labelSelector(labelSelector).execute();
                    } else {
                        // Get Pod metrics for all namespaces
                        metricsData = customApi.listClusterCustomObject(group, version, plural).labelSelector(labelSelector).execute();
                    }
                    
                    // Process returned data
                    if (metricsData instanceof Map) {
                        Map<String, Object> dataMap = (Map<String, Object>) metricsData;
                        List<?> items = (List<?>) dataMap.get("items");
                        if (items != null) {
                            for (Object item : items) {
                                if (item instanceof Map) {
                                    Map<String, Object> podMetric = (Map<String, Object>) item;
                                    Map<String, Object> metadata = (Map<String, Object>) podMetric.get("metadata");
                                    
                                    // Filter Pod names
                                    if (name != null && !name.isEmpty()) {
                                        String podName = (String) metadata.get("name");
                                        if (!name.equals(podName)) {
                                            continue;
                                        }
                                    }

                                    List usages = new ArrayList<>();
                                    for (Map container : (List<Map>) podMetric.get("containers")) {
                                        usages.add(Map.of("name",container.get("name"),"usage",container.get("usage")));
                                    }

                                    result.add(Map.of("name",metadata.get("name"),
                                                      "namespace",metadata.get("namespace"),
                                                        "timestamp",podMetric.get("timestamp"),
                                                        "window",podMetric.get("window"),
                                                        "containers",usages));
                                }
                            }
                        }
                    }
                    
                    // If successfully get data, break the loop
                    if (!result.isEmpty()) {
                        break;
                    }
                } catch (Exception e) {
                    lastError = e;
                    // Continue trying the next version
                }
            }
            
            // If all versions fail, throw an exception
            if (result.isEmpty() && lastError != null) {
                throw new RuntimeException("Unable to get Pod metrics (both metrics.k8s.io v1/v1beta1 unavailable, Metrics Server may not be deployed): " + lastError.getMessage(), lastError);
            }
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get pod metrics", e);
        }
    }

    /**
     * Kubernetes Client Holder
     */
    public static class K8sClientHolder {
        private final ApiClient apiClient;
        private final CoreV1Api coreV1Api;
        private final CustomObjectsApi customObjectsApi;

        private K8sClientHolder(ApiClient apiClient) {
            this.apiClient = apiClient;
            this.coreV1Api = new CoreV1Api(apiClient);
            this.customObjectsApi = new CustomObjectsApi(apiClient);
        }

        public static K8sClientHolder create(String context) throws Exception {
            ApiClient apiClient;
            try {
                // Try to load from kubeconfig
                // Note: In Kubernetes Java Client 25.0.0 version, use the default client directly
                // It will automatically load the default kubeconfig configuration
                apiClient = Config.defaultClient();
            } catch (Exception e) {
                // Fall back to in-cluster configuration
                apiClient = Config.defaultClient();
            }
            return new K8sClientHolder(apiClient);
        }

        public ApiClient getApiClient() {
            return apiClient;
        }

        public CoreV1Api getCoreV1Api() {
            return coreV1Api;
        }

        public CustomObjectsApi getCustomObjectsApi() {
            return customObjectsApi;
        }
    }
}
