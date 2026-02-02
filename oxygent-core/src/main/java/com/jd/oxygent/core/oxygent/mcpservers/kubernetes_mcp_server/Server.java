package com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.EnableMcpServer;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.McpServerStatics;
import com.jd.oxygent.core.oxygent.mcpservers.engine.McpServer;

import java.util.HashSet;
import java.util.Set;

/**
 * Kubernetes MCP Server - Entry Point
 * <p>
 * This entry aligns with OxyGent's existing MCP server style, providing:
 * - Transport modes: stdio / sse / streamable
 * - Port configuration (SSE/Streamable HTTP)
 * - On-demand toolset loading (config/core/helm), with filtering points reserved for security switches like non-destructive mode
 */

public class Server {

    @EnableMcpServer(mode = "stdio")
    public static void main(String[] args) {
        // Parse command-line arguments
        CommandLineArgs parsedArgs = parseArgs(args);
        Set<String> selectedToolsets = normalizeToolsets(parsedArgs.toolsets);
        boolean readonly = parsedArgs.readOnly || KubernetesMcpServer.isReadOnly();
        boolean disableDestructive = parsedArgs.disableDestructive || KubernetesMcpServer.isDisableDestructive();

        // Configure according to transport mode
        if (parsedArgs.transport.equals("sse") || parsedArgs.transport.equals("streamable")) {
            McpServerStatics.transport = parsedArgs.transport;
            McpServerStatics.mode = "web";
        }

        if (parsedArgs.transport.equals("stdio")) {
            McpServerStatics.mode = parsedArgs.transport;
        }

        // Load toolsets
        loadToolsets(selectedToolsets, readonly, disableDestructive);
        // Pass port
        if(parsedArgs.port>0){
            McpServerStatics.port = parsedArgs.port+"";
        }
        // Print startup information
        System.out.println("[kubernetes_mcp_server] transport=" + parsedArgs.transport);
        System.out.println("[kubernetes_mcp_server] port=" + parsedArgs.port);
        System.out.println("[kubernetes_mcp_server] toolsets=" + String.join(",", selectedToolsets));
        System.out.println("[kubernetes_mcp_server] readonly=" + readonly + " disable_destructive=" + disableDestructive);

        // Run server
        McpServer.start();
    }

    /**
     * Parse command-line arguments
     */
    private static CommandLineArgs parseArgs(String[] args) {
        CommandLineArgs result = new CommandLineArgs();

        // Default values
        result.transport = System.getenv().getOrDefault("K8S_MCP_TRANSPORT", "stdio");
        result.port = Integer.parseInt(System.getenv().getOrDefault("K8S_MCP_PORT", "8000"));
        result.toolsets = System.getenv().getOrDefault("K8S_MCP_TOOLSETS", "config,core,helm");
        result.readOnly = false;
        result.disableDestructive = false;

        // Parse command-line arguments
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
     * Normalize toolset list
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
     * Load toolsets
     */
    private static void loadToolsets(Set<String> selected, boolean readonly, boolean disableDestructive) {
        // config group
        if (selected.contains("config")) {
                McpServerStatics.scanClasss.add("com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.ConfigTools");
        }

        // core group
        if (selected.contains("core")) {
            // Read-only/universal capabilities first
            McpServerStatics.scanClasss.add("com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.core_tools.PodsTool");
            McpServerStatics.scanClasss.add("com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.core_tools.ResourcesTool");
            McpServerStatics.scanClasss.add("com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.core_tools.EventsTool");
            McpServerStatics.scanClasss.add("com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.core_tools.NamespacesTool");
            McpServerStatics.scanClasss.add("com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.core_tools.NodesTool");

            // Future: For write operations (create/update/delete), decide whether to import under readonly/disableDestructive conditions
            if (!readonly && !disableDestructive) {
                // Example: Load destructive operation tools
            }
        }

        // helm group
        if (selected.contains("helm")) {
            McpServerStatics.scanClasss.add("com.jd.oxygent.core.oxygent.mcpservers.kubernetes_mcp_server.HelmTools");
        }
    }

    /**
     * Command-line argument wrapper
     */
    private static class CommandLineArgs {
        String transport;
        int port;
        String toolsets;
        boolean readOnly;
        boolean disableDestructive;
    }
}
