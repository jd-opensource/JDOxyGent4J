package com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.EnableMcpServer;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.McpServerStatics;
import com.jd.oxygent.core.oxygent.mcpservers.engine.McpServer;

import java.util.HashSet;
import java.util.Set;

/**
 * Kubernetes MCP Server - Entry Point
 * <p>
 * 该入口对齐 OxyGent 现有 MCP 服务器风格，提供：
 * - 传输模式：stdio / sse / streamable
 * - 端口配置（SSE/Streamable HTTP）
 * - 工具集按需加载（config/core/helm），为非破坏模式等安全开关预留过滤点
 */

public class Server {

    @EnableMcpServer(mode = "stdio")
    public static void main(String[] args) {
        // 解析命令行参数
        CommandLineArgs parsedArgs = parseArgs(args);
        Set<String> selectedToolsets = normalizeToolsets(parsedArgs.toolsets);
        boolean readonly = parsedArgs.readOnly || KubernetesMcpServer.isReadOnly();
        boolean disableDestructive = parsedArgs.disableDestructive || KubernetesMcpServer.isDisableDestructive();

        // 根据传输模式配置
        if (parsedArgs.transport.equals("sse") || parsedArgs.transport.equals("streamable")) {
            McpServerStatics.transport = parsedArgs.transport;
            McpServerStatics.mode = "web";
        }

        if (parsedArgs.transport.equals("stdio")) {
            McpServerStatics.mode = parsedArgs.transport;
        }

        // 加载工具集
        loadToolsets(selectedToolsets, readonly, disableDestructive);
        //传递端口
        if(parsedArgs.port>0){
            McpServerStatics.port = parsedArgs.port+"";
        }
        // 打印启动信息
        System.out.println("[kubernetes_mcp_server] transport=" + parsedArgs.transport);
        System.out.println("[kubernetes_mcp_server] port=" + parsedArgs.port);
        System.out.println("[kubernetes_mcp_server] toolsets=" + String.join(",", selectedToolsets));
        System.out.println("[kubernetes_mcp_server] readonly=" + readonly + " disable_destructive=" + disableDestructive);

        // 运行服务器
        McpServer.start();
    }

    /**
     * 解析命令行参数
     */
    private static CommandLineArgs parseArgs(String[] args) {
        CommandLineArgs result = new CommandLineArgs();

        // 默认值
        result.transport = System.getenv().getOrDefault("K8S_MCP_TRANSPORT", "stdio");
        result.port = Integer.parseInt(System.getenv().getOrDefault("K8S_MCP_PORT", "8000"));
        result.toolsets = System.getenv().getOrDefault("K8S_MCP_TOOLSETS", "config,core,helm");
        result.readOnly = false;
        result.disableDestructive = false;

        // 解析命令行参数
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--transport":
                    if (i + 1 < args.length) {
                        result.transport = args[++i];
                    }
                    break;
                case "--port":
                    if (i + 1 < args.length) {
                        try {
                            result.port = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid port number: " + args[i]);
                        }
                    }
                    break;
                case "--toolsets":
                    if (i + 1 < args.length) {
                        result.toolsets = args[++i];
                    }
                    break;
                case "--read-only":
                    result.readOnly = true;
                    break;
                case "--disable-destructive":
                    result.disableDestructive = true;
                    break;
            }
        }

        return result;
    }

    /**
     * 标准化工具集列表
     */
    private static Set<String> normalizeToolsets(String toolsetsStr) {
        Set<String> result = new HashSet<>();
        if (toolsetsStr != null && !toolsetsStr.isEmpty()) {
            String[] toolsets = toolsetsStr.split(",");
            for (String toolset : toolsets) {
                String trimmed = toolset.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
        }
        return result;
    }

    /**
     * 加载工具集
     */
    private static void loadToolsets(Set<String> selected, boolean readonly, boolean disableDestructive) {
        // config 组
        if (selected.contains("config")) {
                McpServerStatics.scanClasss.add("com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.ConfigTools");
        }

        // core 组
        if (selected.contains("core")) {
            // 只读/通用能力优先
            McpServerStatics.scanClasss.add("com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.core_tools.ReadOnlyCoreTools");
            McpServerStatics.scanClasss.add("com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.core_tools.PodsTool");
            McpServerStatics.scanClasss.add("com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.core_tools.ResourcesTool");
            McpServerStatics.scanClasss.add("com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.core_tools.EventsTool");
            McpServerStatics.scanClasss.add("com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.core_tools.NamespacesTool");
            McpServerStatics.scanClasss.add("com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.core_tools.NodesTool");

            // 未来：如需写操作（create/update/delete），在 readonly/disableDestructive 条件下决定是否导入
            if (!readonly && !disableDestructive) {
                // 例如：加载破坏性操作工具
            }
        }

        // helm 组
        if (selected.contains("helm")) {
            McpServerStatics.scanClasss.add("com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.HelmTools");
        }
    }

    /**
     * 命令行参数封装
     */
    private static class CommandLineArgs {
        String transport;
        int port;
        String toolsets;
        boolean readOnly;
        boolean disableDestructive;
    }
}
