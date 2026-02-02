package com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.core_tools;

import java.util.Map;

/**
 * Kubernetes Utility Class
 * Provides common utility methods related to Kubernetes
 */
public class K8sUtils {

    /**
     * Parse API version and resource type
     */
    public static String[] parseGroupVersion(String apiVersion) {
        String[] parts = apiVersion.split("/");
        if (parts.length == 2) {
            return parts; // [group, version]
        } else {
            return new String[]{"", apiVersion}; // [empty group, version]
        }
    }

    /**
     * Get plural form of resource type
     */
    public static String getPlural(String kind) {
        return kind.toLowerCase() + "s";
    }

    /**
     * Perform sensitive field masking on Secret objects
     */
    public static Map<String, Object> maskSecret(Map<String, Object> obj) {
        try {
            if (obj == null) {
                return obj;
            }
            if (!"Secret".equals(obj.get("kind"))) {
                return obj;
            }
            if (obj.get("data") instanceof Map) {
                Map<?, ?> data = (Map<?, ?>) obj.get("data");
                for (Object key : data.keySet()) {
                    obj.put(key.toString(), "***");
                }
            }
            if (obj.get("stringData") instanceof Map) {
                Map<?, ?> stringData = (Map<?, ?>) obj.get("stringData");
                for (Object key : stringData.keySet()) {
                    obj.put(key.toString(), "***");
                }
            }
        } catch (Exception e) {
            // When an exception occurs during masking, do not interrupt the main process
            return obj;
        }
        return obj;
    }

    /**
     * Ensure Kubernetes client is available
     */
    public static void ensureK8sAvailable() {
        try {
            // Check if Kubernetes Java client is available
            Class.forName("io.kubernetes.client.openapi.ApiClient");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Kubernetes Java client not installed");
        }
    }
}
