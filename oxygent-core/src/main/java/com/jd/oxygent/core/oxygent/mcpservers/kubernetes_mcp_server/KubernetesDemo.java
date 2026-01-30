package com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server;

/**
 * Kubernetes MCP 服务器测试示例
 *
 * 本示例展示如何在 OxyGent 中集成和使用 Kubernetes MCP 服务器。
 * 包含完整的配置、启动和测试流程。
 *
 * 使用前请确保：
 * 1. 已安装所有依赖：pip install -r mcp_servers/kubernetes_mcp_server/requirements.txt
 * 2. 配置了可访问的 Kubernetes 集群
 * 3. 设置了正确的环境变量
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Semaphore;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

/**
 * Kubernetes MCP 服务器测试示例
 *
 * 本示例展示如何在 OxyGent 中集成和使用 Kubernetes MCP 服务器。
 * 包含完整的配置、启动和测试流程。
 *
 * 使用前请确保：
 * 1. 已安装所有依赖：kubernetes-client, snakeyaml, commons-text
 * 2. 配置了可访问的 Kubernetes 集群
 * 3. 设置了正确的环境变量
 */
public class KubernetesDemo {

    /**
     * 获取 Kubernetes MCP OxySpace 配置
     * 包含完整的 Kubernetes 管理功能
     *
     * @return BaseOxy 列表，包含 MCP 工具和智能体
     */
    @OxySpaceBean(value = "kubernetesMCPOxySpace", defaultStart = true,  query = "请帮我查看当前 Kubernetes 集群的基本信息")
    public static List<BaseOxy> getKubernetesOxySpace() {
        // 配置环境变量
        final Map<String, String> env = new HashMap<>();
        env.put("K8S_MCP_TRANSPORT", "stdio");
        env.put("K8S_MCP_TOOLSETS", "config,core,helm");
        env.put("K8S_MCP_READ_ONLY", "false");
        env.put("K8S_MCP_DISABLE_DESTRUCTIVE", "false");

        // 设置类路径，确保能加载到相关类
        String classpath = System.getProperty("java.class.path");
        env.put("CLASSPATH", classpath);

        // 创建 Kubernetes MCP 客户端 - 完整功能模式
        var k8sMcpTools = new StdioMCPClient(
                "kubernetes_mcp_server_tools",
                "java",
                Arrays.asList(
                        "-cp",
                        classpath,
                        "com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.KubernetesMcpServer",
                        "--transport", "stdio",
                        "--toolsets", "config,core,helm",
                        "--read-only", "false",
                        "--disable-destructive", "false"
                )
        );
        k8sMcpTools.setEnvMap(env);

        return Arrays.asList(
                // LLM 配置
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("DEFAULT_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("DEFAULT_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("DEFAULT_LLM_MODEL_NAME"))
                        .llmParams(Map.of("temperature", 0.01f))
                        .semaphore(new Semaphore(4))
                        .timeout(240)
                        .build(),

                // Kubernetes MCP 工具
                k8sMcpTools,

                // Kubernetes 管理智能体
                ReActAgent.builder()
                        .name("k8s_admin_agent")
                        .isMaster( true)
                        .desc("Kubernetes 集群管理专家，能够查看和管理 K8s 资源，包括 Pods、Nodes、Namespaces 等")
                        .tools(Arrays.asList("kubernetes_mcp_server_tools"))
                        .trustMode(false)
                        .timeout(120)
                        .build()
        );
    }

    /**
     * 应用程序主入口点
     * 初始化 MCP 工具并启动 Spring Boot 应用
     *
     * @param args 命令行参数
     * @throws Exception 当应用程序启动失败时
     */
    public static void main(String[] args) throws Exception {
        System.out.println("🔧 启动 Kubernetes MCP 服务器测试...");
        System.out.println("📝 请确保已配置好环境变量和 Kubernetes 集群访问权限");
        System.out.println("🌐 Web 界面将在启动后自动打开");
        System.out.println("-".repeat(50));

        System.out.println("检查环境变量:");
        System.out.println("DEFAULT_LLM_API_KEY: " + (EnvUtils.getEnv("DEFAULT_LLM_API_KEY") != null ? "已设置" : "未设置"));
        System.out.println("DEFAULT_LLM_BASE_URL: " + (EnvUtils.getEnv("DEFAULT_LLM_BASE_URL") != null ? "已设置" : "未设置"));
        System.out.println("DEFAULT_LLM_MODEL_NAME: " + (EnvUtils.getEnv("DEFAULT_LLM_MODEL_NAME") != null ? "已设置" : "未设置"));

        // 检查 Kubernetes 配置
        System.out.println("\n检查 Kubernetes 配置:");
        String kubeConfigPath = System.getenv("KUBECONFIG");
        if (kubeConfigPath != null) {
            System.out.println("KUBECONFIG 路径: " + kubeConfigPath);
        } else {
            System.out.println("KUBECONFIG 未设置，将使用默认路径 ~/.kube/config");
        }

        // 检查是否能在类路径中找到必要的类
        try {
            Class.forName("io.kubernetes.client.openapi.ApiClient");
            System.out.println("✅ Kubernetes Java Client 已加载");
        } catch (ClassNotFoundException e) {
            System.out.println("❌ Kubernetes Java Client 未找到，请添加依赖");
        }

        try {
            Class.forName("com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.KubernetesMcpServer");
            System.out.println("✅ Kubernetes MCP 服务器类已加载");
        } catch (ClassNotFoundException e) {
            System.out.println("❌ Kubernetes MCP 服务器类未找到");
        }

        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);
    }
}