package com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.core_tools;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.util.Config;

/**
 * Kubernetes 客户端持有者
 * 提供统一的 Kubernetes 客户端初始化和访问方法
 */
public class K8sClientHolder {
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
            apiClient = Config.fromCluster();
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
