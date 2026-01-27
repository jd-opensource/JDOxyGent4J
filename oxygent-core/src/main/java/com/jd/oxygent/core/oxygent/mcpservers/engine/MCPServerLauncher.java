package com.jd.oxygent.core.oxygent.mcpservers.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.core.oxygent.mcpservers.engine.metadata.ToolMetadata;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CountDownLatch;
@Slf4j
public class MCPServerLauncher {

    /**
     * Start MCP server
     * @param mode Startup mode
     * @param className Startup class name
     * @param packageName Package name
     * @param tools List of scanned tools
     */
    public void start(String mode, String className, String packageName, List<ToolMetadata> tools) {
        log.info("[MCPServerLauncher] Startup parameters:");
        log.info("  Mode: {}", mode);
        log.info("  Class name: {}", className);
        log.info("  Package name: {}", packageName);
        log.info("  Tool count: {}", tools.size());

        // Start different servers according to mode
        switch (mode.toLowerCase()) {
            case "stdio":
                startStdioServer(tools, className);
                break;
            case "web":
                startWebServer(tools, className);
                break;
            default:
                throw new IllegalArgumentException("Unsupported startup mode: " + mode);
        }
    }

    private void startStdioServer(List<ToolMetadata> tools, String serverName) {
        log.info("[MCPServerLauncher] Starting stdio server");

        try {
            // 1. Create tool registry
            MCPToolRegistry toolRegistry = new MCPToolRegistry();
            toolRegistry.registerTools(tools);

            // 2. Create MCP server
            StdioServerTransportProvider transportProvider =
                    new StdioServerTransportProvider(new JacksonMcpJsonMapper(new ObjectMapper()));

            McpSyncServer syncServer = McpServer.sync(transportProvider)
                    .serverInfo(serverName, "1.0.0")
                    .capabilities(McpSchema.ServerCapabilities.builder()
                            .tools(true)
                            .prompts(true)
                            .resources(false, true)
                            .logging()
                            .build())
                    .build();

            // 3. Register tools to MCP server
            toolRegistry.registerToMCPServer(syncServer);

            log.info("[MCPServerLauncher] MCP Server '{}' started with {} tools via stdio", serverName, toolRegistry.getToolCount());

            // 4. Keep server running
            keepServerRunning(syncServer);

        } catch (Exception e) {
            log.error("[MCPServerLauncher] Failed to start stdio server: {}", e.getMessage());
            log.debug("Error details:", e);
            throw new RuntimeException("Failed to start stdio server", e);
        }
    }

    private void startWebServer(List<ToolMetadata> tools, String serverName) {
        log.info("[MCPServerLauncher] Starting web server");

        // TODO: Implement WebSocket/HTTP server
        // Web server implementation needed based on actual requirements

        log.warn("[MCPServerLauncher] Web server mode not yet implemented");

        // Temporary implementation: use stdio as fallback
        log.info("[MCPServerLauncher] Temporarily falling back to stdio mode");
        startStdioServer(tools, serverName);
    }

    /**
     * Keep server running
     */
    private void keepServerRunning(McpSyncServer syncServer) {
        CountDownLatch latch = new CountDownLatch(1);

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("[MCPServerLauncher] Shutting down MCP server...");
            try {
                syncServer.close();
                log.info("[MCPServerLauncher] MCP server closed");
            } catch (Exception e) {
                log.error("[MCPServerLauncher] Error closing server: {}", e.getMessage());
            }
            latch.countDown();
        }));

        try {
            log.info("[MCPServerLauncher] MCP server is running...");
            latch.await(); // Block indefinitely until shutdown signal is received
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[MCPServerLauncher] Server interrupted");
        }
    }
}