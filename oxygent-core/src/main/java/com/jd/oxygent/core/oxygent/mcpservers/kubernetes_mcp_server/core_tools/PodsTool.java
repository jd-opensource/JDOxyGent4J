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
 * 提供与 Pod 相关的常用只读能力：
 * - pods_list：列出所有命名空间的 Pods
 * - pods_list_in_namespace：列出指定命名空间的 Pods
 * - pods_get：获取指定 Pod 的完整对象
 * - pods_log：获取 Pod 日志（支持容器选择、previous、tail）
 * - pods_exec：在 Pod 容器内执行命令并返回输出
 * - pods_top：从 metrics.k8s.io 读取 Pod 资源使用情况（需部署 Metrics Server）
 */
public class PodsTool {

    // 尝试导入 Kubernetes Java 客户端
    private static boolean k8sAvailable = false;

    static {
        try {
            // 检查 Kubernetes Java 客户端是否可用
            Class.forName("io.kubernetes.client.openapi.ApiClient");
            k8sAvailable = true;
        } catch (ClassNotFoundException e) {
            k8sAvailable = false;
        }
    }

    /**
     * 确保 Kubernetes 客户端可用
     */
    public static void ensureK8sAvailable() {
        if (!k8sAvailable) {
            throw new RuntimeException("Kubernetes Java 客户端未安装");
        }
    }

    /**
     * 加载 Kubernetes 配置
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
     * 生成 Pod 摘要信息
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
     * 列出所有命名空间的 Pods
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
     * 列出指定命名空间的 Pods
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
     * 获取指定 Pod 的完整对象
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

            // 使用 Jackson 将 Pod 对象转换为 Map
            ObjectMapper mapper = new ObjectMapper();
            return mapper.convertValue(pod, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get pod", e);
        }
    }

    /**
     * 获取 Pod 日志
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
            // 调用 API 获取日志
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
     * 在 Pod 容器内执行命令并返回输出
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
            
            // 创建 exec 请求
            V1ExecAction execAction = new V1ExecAction();
                         execAction.setCommand(command);

            // 定义要执行的命令
            String commands = String.join("",command);

            return coreV1Api.connectGetNamespacedPodExec(name, namespace).command(commands).container(container).execute();

        } catch (Exception e) {
            throw new RuntimeException("Failed to execute command in pod", e);
        }
    }

    /**
     * 从 metrics.k8s.io 读取 Pod 资源使用情况
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
            String[] versions = {"v1", "v1beta1"}; // 优先 v1，失败回退 v1beta1
            String plural = "pods";
            
            List<Map<String, Object>> result = new ArrayList<>();
            Exception lastError = null;
            
            // 尝试不同版本的 API
            for (String version : versions) {
                try {
                    Object metricsData;
                    if (namespace != null && !namespace.isEmpty()) {
                        // 获取指定命名空间的 Pod 指标
                        metricsData = customApi.listNamespacedCustomObject(group, version, namespace, plural).labelSelector(labelSelector).execute();
                    } else {
                        // 获取所有命名空间的 Pod 指标
                        metricsData = customApi.listClusterCustomObject(group, version, plural).labelSelector(labelSelector).execute();
                    }
                    
                    // 处理返回的数据
                    if (metricsData instanceof Map) {
                        Map<String, Object> dataMap = (Map<String, Object>) metricsData;
                        List<?> items = (List<?>) dataMap.get("items");
                        if (items != null) {
                            for (Object item : items) {
                                if (item instanceof Map) {
                                    Map<String, Object> podMetric = (Map<String, Object>) item;
                                    Map<String, Object> metadata = (Map<String, Object>) podMetric.get("metadata");
                                    
                                    // 过滤 Pod 名称
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
                    
                    // 如果成功获取数据，跳出循环
                    if (!result.isEmpty()) {
                        break;
                    }
                } catch (Exception e) {
                    lastError = e;
                    // 继续尝试下一个版本
                }
            }
            
            // 如果所有版本都失败，抛出异常
            if (result.isEmpty() && lastError != null) {
                throw new RuntimeException("无法获取 Pod 监控指标（metrics.k8s.io v1/v1beta1 均不可用，可能未部署 Metrics Server）：" + lastError.getMessage(), lastError);
            }
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get pod metrics", e);
        }
    }

    /**
     * Kubernetes 客户端持有者
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
                // 尝试从 kubeconfig 加载
                // 注意：在 Kubernetes Java Client 25.0.0 版本中，直接使用默认客户端
                // 它会自动加载默认的 kubeconfig 配置
                apiClient = Config.defaultClient();
            } catch (Exception e) {
                // 回退到集群内配置
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
