package com.jd.oxygent.core.oxygent.mcpservers.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.core.oxygent.mcpservers.engine.metadata.ToolMetadata;
import com.jd.oxygent.core.oxygent.samples.server.tomcat.McpTomcat;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import io.modelcontextprotocol.spec.McpServerTransportProviderBase;
import jakarta.servlet.Servlet;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;
import java.time.Duration;
import java.util.Arrays;
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
    public void start(String mode, String className, String packageName, List<ToolMetadata> tools,String localhost,String port,String transport) {
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
                startWebServer(tools, className, localhost, port, transport);
                break;
            default:
                throw new IllegalArgumentException("Unsupported startup mode: " + mode);
        }
    }

    private void startStdioServer(List<ToolMetadata> tools, String serverName) {
        //Create StdioServerTransportProvider
        StdioServerTransportProvider transportProvider = new StdioServerTransportProvider(new JacksonMcpJsonMapper(new ObjectMapper()));
        startServer(tools, serverName, transportProvider);
    }

    private void startServer(List<ToolMetadata> tools, String serverName, McpServerTransportProviderBase  transport) {
        log.info("[MCPServerLauncher] Starting stdio server");

        try {
            // 1. Create tool registry
            MCPToolRegistry toolRegistry = new MCPToolRegistry();
            toolRegistry.registerTools(tools);
            McpSyncServer syncServer = null;
            // 2. Create MCP server
            if(transport instanceof HttpServletSseServerTransportProvider){
                syncServer = McpServer.sync((HttpServletSseServerTransportProvider) transport)
                        .serverInfo(serverName, "1.0.0")
                        .capabilities(McpSchema.ServerCapabilities.builder()
                                .tools(true)
                                .prompts(true)
                                .resources(false, true)
                                .logging()
                                .build())
                        .build();
            }else if(transport instanceof HttpServletStreamableServerTransportProvider){
                syncServer = McpServer.sync((HttpServletStreamableServerTransportProvider) transport)
                        .serverInfo(serverName, "1.0.0")
                        .capabilities(McpSchema.ServerCapabilities.builder()
                                .tools(true)
                                .prompts(true)
                                .resources(false, true)
                                .logging()
                                .build())
                        .build();
            }else if(transport instanceof StdioServerTransportProvider){
                syncServer = McpServer.sync((StdioServerTransportProvider) transport)
                        .serverInfo(serverName, "1.0.0")
                        .capabilities(McpSchema.ServerCapabilities.builder()
                                .tools(true)
                                .prompts(true)
                                .resources(false, true)
                                .logging()
                                .build())
                        .build();
            }
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

    private void startWebServer(List<ToolMetadata> tools, String serverName,String localhost,String port,String transport) {
        log.info("[MCPServerLauncher] Starting web server");

        // Web server implementation needed based on actual requirements
        McpServerTransportProviderBase mcpServerTransportProvider = null;

        // Choose the appropriate transport provider based on the specified transport type
        switch (transport) {
            case "sse":
                // Configure SSE (Server-Sent Events) transport provider
                mcpServerTransportProvider = HttpServletSseServerTransportProvider.builder()
                        .jsonMapper(new JacksonMcpJsonMapper(new ObjectMapper())) // Set JSON mapper for serialization
                        .messageEndpoint("/mcp/message") // Define message endpoint
                        .sseEndpoint("/mcp/sse") // Define SSE endpoint
                        .baseUrl("http://"+localhost+":"+port) // Set base URL for the server
                        .keepAliveInterval(Duration.ofSeconds(1)) // Set keep-alive interval to 1 second
                        .build();
                break;
            case "streamable":
                // Configure streamable HTTP transport provider
                mcpServerTransportProvider = HttpServletStreamableServerTransportProvider.builder()
                        .jsonMapper(new JacksonMcpJsonMapper(new ObjectMapper())) // Set JSON mapper for serialization
                        .disallowDelete(true) // Disable DELETE operations
                        .mcpEndpoint("/mcp/sse") // Define MCP endpoint
                        .keepAliveInterval(Duration.ofSeconds(1)) // Set keep-alive interval to 1 second
                        .build();
                break;
            default:
                // Throw exception for unsupported transport modes
                throw new IllegalArgumentException("Mcp Server Unsupported transport mode: " + transport);
        }
        
        // Create a Tomcat server instance with the configured transport provider
        McpTomcat mcpTomcat = McpTomcat.builder()
                .servlet((Servlet) mcpServerTransportProvider) // Set the servlet using the transport provider
                .port(Integer.parseInt(port)) // Set the port number from the parameter
                .addresses(localhost) // Set the host address from the parameter
                .contextPath("/") // Set root context path
                .build();

        // Start the MCP server with the configured transport
        startServer(tools, serverName, mcpServerTransportProvider);
        log.info("[McpTomcat] Starting ......");

        // Launch the Tomcat server to start accepting connections


        mcpTomcat.launch(new String[]{transport});
    }

    /**
     * Keep server running
     */
    private void keepServerRunning(McpSyncServer syncServer) {
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("[MCPServerLauncher] Shutting down MCP server...");
            try {
                syncServer.close();
                log.info("[MCPServerLauncher] MCP server closed");
            } catch (Exception e) {
                log.error("[MCPServerLauncher] Error closing server: {}", e.getMessage());
            }
       }));
    }
}