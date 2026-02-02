package com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.core_tools;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.util.Config;

/**
 * Kubernetes Client Holder
 * Provides unified Kubernetes client initialization and access methods
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
            // Try to load from kubeconfig
            // Note: In Kubernetes Java Client 25.0.0 version, use the default client directly
            // It will automatically load the default kubeconfig configuration
            apiClient = Config.defaultClient();
        } catch (Exception e) {
            // Fall back to in-cluster configuration
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
