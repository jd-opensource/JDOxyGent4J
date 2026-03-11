package com.jd.oxygent.core.oxygent.mcpservers.mcp_testing_utilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;

import java.time.Duration;
import java.util.Map;

/**
 * MCP (Model Context Protocol) Client Test Class
 * Uses Stdio transport for communication
 * Used to test MCP server connectivity and functionality
 */
public class McpClientStdio {
    public static void main(String[] args) {

        System.out.println("Starting MCP Stdio client...");

        McpSyncClient client = null;

        try {
            // 1. Create Stdio transport layer
            // Configure server parameters, start node.js MCP server
            ServerParameters params = ServerParameters.builder("D:\\Program Files\\node-v22.20.0-win-x64\\npx.cmd")
                    .args("-y", "@modelcontextprotocol/server-everything", "stdio")
                    .build();

            // Create Stdio transport
            McpClientTransport transport = new StdioClientTransport(params,new JacksonMcpJsonMapper(new ObjectMapper()));

            System.out.println("Stdio transport created, connecting to server...");

            // 2. Create client
            client = McpClient.sync(transport)
                    .requestTimeout(Duration.ofSeconds(30))
                    // Adjust according to server capabilities
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

            // 3. Initialize connection
            System.out.println("Initializing connection...");
            client.initialize();

            // 4. List available tools
            System.out.println("\nFetching available tools...");
            McpSchema.ListToolsResult listToolsResult = client.listTools();

            System.out.println("✓ Found " + listToolsResult.tools().size() + " tool(s):");
            listToolsResult.tools().forEach(tool ->
                    System.out.println("  • " + tool.name() +
                            (tool.description() != null ? " - " + tool.description() : ""))
            );

            // 5. Find specific tool
            String targetTool = "calc_pi"; // You can change this to any tool name you need

            boolean hasTargetTool = listToolsResult.tools().stream()
                    .anyMatch(tool -> targetTool.equals(tool.name()));

            if (hasTargetTool) {
                // 调用工具
                McpSchema.CallToolResult result = client.callTool(
                        new McpSchema.CallToolRequest(targetTool, Map.of("prec", 20))
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
                System.out.println("\n✗ '" + targetTool + "' tool not found. Available tools:");
                listToolsResult.tools().forEach(tool ->
                        System.out.println("  • " + tool.name() +
                                (tool.description() != null ? " - " + tool.description() : ""))
                );

                // If calc_pi is not found, try calling other tools
                if (!listToolsResult.tools().isEmpty()) {
                    McpSchema.Tool tool = listToolsResult.tools().get(0);
                    System.out.println("\nTrying to call first available tool: " + tool.name());
                    try {
                        McpSchema.CallToolResult result = client.callTool(
                                new McpSchema.CallToolRequest(tool.name(), Map.of())
                        );
                        System.out.println("Tool call result: " + result);
                    } catch (Exception e) {
                        System.out.println("Failed to call tool: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 6. Gracefully close client
            if (client != null) {
                try {
                    System.out.println("\nClosing client...");
                    client.closeGracefully();
                    System.out.println("Client closed successfully.");
                } catch (Exception e) {
                    System.err.println("Error closing client: " + e.getMessage());
                }
            }
        }
    }
}