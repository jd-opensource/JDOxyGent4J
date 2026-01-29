package com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.core_tools;

import java.util.Map;

/**
 * Kubernetes 工具类
 * 提供 Kubernetes 相关的通用工具方法
 */
public class K8sUtils {

    /**
     * 解析 API 版本和资源类型
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
     * 获取资源类型的复数形式
     */
    public static String getPlural(String kind) {
        return kind.toLowerCase() + "s";
    }

    /**
     * 对 Secret 对象进行敏感字段掩码处理
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
            // 掩码过程中出现异常时，不阻断主流程
            return obj;
        }
        return obj;
    }

    /**
     * 确保 Kubernetes 客户端可用
     */
    public static void ensureK8sAvailable() {
        try {
            // 检查 Kubernetes Java 客户端是否可用
            Class.forName("io.kubernetes.client.openapi.ApiClient");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Kubernetes Java 客户端未安装");
        }
    }
}
