package com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server;

/**
 * Kubernetes MCP Server Test Example
 *
 * This example demonstrates how to integrate and use the Kubernetes MCP server in OxyGent.
 * It includes the complete configuration, startup, and testing process.
 *
 * Before use, ensure:
 * 1. All dependencies are installed: pip install -r mcp_servers/kubernetes_mcp_server/requirements.txt
 * 2. A Kubernetes cluster is configured and accessible
 * 3. Correct environment variables are set
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
 * Kubernetes MCP Server Test Example
 *
 * This example demonstrates how to integrate and use the Kubernetes MCP server in OxyGent.
 * It includes the complete configuration, startup, and testing process.
 *
 * Before use, ensure:
 * 1. All dependencies are installed: kubernetes-client, snakeyaml, commons-text
 * 2. A Kubernetes cluster is configured and accessible
 * 3. Correct environment variables are set
 */
public class KubernetesDemo {

    /**
     * Get Kubernetes MCP OxySpace configuration
     * Contains complete Kubernetes management capabilities
     *
     * @return BaseOxy list, containing MCP tools and agents
     */
    @OxySpaceBean(value = "kubernetesMCPOxySpace", defaultStart = true,  query = "Please help me check the basic information of the current Kubernetes cluster")
    public static List<BaseOxy> getKubernetesOxySpace() {
        // Configure environment variables
        final Map<String, String> env = new HashMap<>();
        env.put("K8S_MCP_TRANSPORT", "stdio");
        env.put("K8S_MCP_TOOLSETS", "config,core,helm");
        env.put("K8S_MCP_READ_ONLY", "false");
        env.put("K8S_MCP_DISABLE_DESTRUCTIVE", "false");

        // Set classpath, ensure related classes can be loaded
        String classpath = System.getProperty("java.class.path");
        env.put("CLASSPATH", classpath);

        // Create Kubernetes MCP client - full functionality mode
        var k8sMcpTools = new StdioMCPClient(
                "kubernetes_mcp_server_tools",
                "java",
                Arrays.asList(
                        "-cp",
                        classpath,
                        "com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.Server",
                        "--transport", "stdio",
                        "--toolsets", "config,core,helm",
                        "--read-only", "false",
                        "--disable-destructive", "false"
                )
        );
        k8sMcpTools.setEnvMap(env);

        return Arrays.asList(
                // LLM Configuration
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .llmParams(Map.of("temperature", 0.01f))
                        .semaphore(new Semaphore(4))
                        .timeout(240)
                        .build(),

                // Kubernetes MCP Tools
                k8sMcpTools,

                // Kubernetes Management Agent
                ReActAgent.builder()
                        .name("k8s_admin_agent")
                        .isMaster( true)
                        .desc("Kubernetes cluster management expert, able to view and manage K8s resources, including Pods, Nodes, Namespaces, etc.")
                        .tools(Arrays.asList("kubernetes_mcp_server_tools"))
                        .trustMode(false)
                        .timeout(120)
                        .build()
        );
    }

    /**
     * Main entry point for the application
     * Initialize MCP tools and start Spring Boot application
     *
     * @param args Command line arguments
     * @throws Exception When application startup fails
     */
    public static void main(String[] args) throws Exception {
        System.out.println("🔧 Starting Kubernetes MCP server test...");
        System.out.println("📝 Please ensure environment variables and Kubernetes cluster access permissions are configured");
        System.out.println("🌐 Web interface will open automatically after startup");
        System.out.println("-".repeat(50));

        System.out.println("Checking environment variables:");
        System.out.println("OXY_LLM_API_KEY: " + (EnvUtils.getEnv("OXY_LLM_API_KEY") != null ? "Set" : "Not set"));
        System.out.println("OXY_LLM_BASE_URL: " + (EnvUtils.getEnv("OXY_LLM_BASE_URL") != null ? "Set" : "Not set"));
        System.out.println("OXY_LLM_MODEL_NAME: " + (EnvUtils.getEnv("OXY_LLM_MODEL_NAME") != null ? "Set" : "Not set"));

        // Check Kubernetes configuration
        System.out.println("\nChecking Kubernetes configuration:");
        String kubeConfigPath = System.getenv("KUBECONFIG");
        if (kubeConfigPath != null) {
            System.out.println("KUBECONFIG path: " + kubeConfigPath);
        } else {
            System.out.println("KUBECONFIG not set, will use default path ~/.kube/config");
        }

        // Check if necessary classes can be found in classpath
        try {
            Class.forName("io.kubernetes.client.openapi.ApiClient");
            System.out.println("✅ Kubernetes Java Client loaded");
        } catch (ClassNotFoundException e) {
            System.out.println("❌ Kubernetes Java Client not found, please add dependency");
        }

        try {
            Class.forName("com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.KubernetesMcpServer");
            System.out.println("✅ Kubernetes MCP server class loaded");
        } catch (ClassNotFoundException e) {
            System.out.println("❌ Kubernetes MCP server class not found");
        }

        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);
    }
}