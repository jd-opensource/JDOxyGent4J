package com.jd.oxygent.core.oxygent.mcpservers.mcp_testing_utilities;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;

import java.time.Duration;
import java.util.Map;


/**
 * MCP (Model Context Protocol) Client Test Class
 * Uses Server-Sent Events (SSE) for communication
 * Used to test MCP server connectivity and functionality
 */
public class McpClientSse {
    public static void main(String[] args) {

        // Using verified URL
        String url = "http://127.0.0.1:8000";
        System.out.println("Connecting to: " + url);

        McpSyncClient client = null;

        try {
            // 1. Create SSE transport layer
            McpClientTransport transport = HttpClientSseClientTransport
                    .builder(url)
                    .sseEndpoint("mcp/sse")
                    .build();

            // 2. Create simplified client (without advanced features for now)
            client = McpClient.sync(transport)
                    .requestTimeout(Duration.ofSeconds(30))
                    // Temporarily disable unnecessary capabilities for compatibility
                    .capabilities(McpSchema.ClientCapabilities.builder()
                            .roots(false)       // Set to false if server doesn't support roots
                            .sampling()         // Keep enabled
                            .elicitation()      // Keep enabled
                            .build())
                    // Add logging consumer for debugging
                    .loggingConsumer((notification) -> {
                        System.out.println("[MCP Log] " + notification.data());
                    })
                    // Add progress consumer
                    .progressConsumer((progress) -> {
                        System.out.println("[Progress] " + progress.progress());
                    })
                    .build();

            // 3. 初始化连接
            System.out.println("Initializing connection...");
            client.initialize();

            // 4. 列出可用工具
            System.out.println("\nFetching available tools...");
            McpSchema.ListToolsResult listToolsResult = client.listTools();

            System.out.println("✓ Found " + listToolsResult.tools().size() + " tool(s):");
            listToolsResult.tools().forEach(tool ->
                    System.out.println("  • " + tool.name() +
                            (tool.description() != null ? " - " + tool.description() : ""))
            );

            // 5. 查找calc_pi工具
            boolean hasCalcPi = listToolsResult.tools().stream()
                    .anyMatch(tool -> "calc_pi".equals(tool.name()));

            if (hasCalcPi) {
                // 调用calc_pi工具
                McpSchema.CallToolResult result = client.callTool(
                        new McpSchema.CallToolRequest("calc_pi", Map.of("prec", 20))
                );

                System.out.println("\n=== Tool Call Result ===");
                if (result.content() != null && !result.content().isEmpty()) {
                    System.out.println("Result type: " + result.content().get(0).getClass().getSimpleName());

                    result.content().forEach(content -> {
                        if (content instanceof McpSchema.TextContent textContent) {
                            System.out.println("Text content: " + textContent.text());
                        } else if (content instanceof McpSchema.ImageContent imageContent) {
                            System.out.println("Image content (mime type: " + imageContent.mimeType() + ")");
                            System.out.println("Data length: " + (imageContent.data() != null ? imageContent.data().length() : 0) + " bytes");
                        } else {
                            System.out.println("Content: " + content);
                            System.out.println("Content class: " + content.getClass().getName());
                        }
                    });
                } else {
                    System.out.println("No content returned from tool.");
                }
            } else {
                System.out.println("\n✗ 'calc_pi' tool not found. Available tools:");
                listToolsResult.tools().forEach(tool ->
                        System.out.println("  • " + tool.name())
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 8. Gracefully close client
            if (client != null) {
                try {
                    client.closeGracefully();
                } catch (Exception e) {
                    System.err.println("Error closing client: " + e.getMessage());
                }
            }
        }
    }
}