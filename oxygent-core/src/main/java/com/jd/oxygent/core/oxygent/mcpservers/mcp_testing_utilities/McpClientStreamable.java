package com.jd.oxygent.core.oxygent.mcpservers.mcp_testing_utilities;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;

import java.time.Duration;
import java.util.Map;

/**
 * MCP (Model Context Protocol) Client Streamable Test Class
 * 
 * This class is used to test Streamable HTTP connections with an MCP server.
 * Main functions include:
 * 1. Establishing a connection to the local MCP server (default address: http://127.0.0.1:8080/mcp/sse)
 * 2. Initializing the client and retrieving the list of available tools
 * 3. If a tool named 'calc_pi' is found, calling this tool to calculate pi
 * 4. Processing tool call results and outputting content
 * 5. Gracefully closing the connection when the program ends
 * 
 * Note: Although the endpoint path is named /sse, it actually uses the Streamable HTTP protocol
 */
public class McpClientStreamable {
    public static void main(String[] args) {

        // ⚠️ Using Streamable HTTP transport, port 8080
        String baseUrl = "http://127.0.0.1:8080";
        String endpoint = "/mcp/sse";  // Note: Although the path is called sse, it is actually Streamable HTTP

        McpSyncClient client = null;

        try {
            // 1. Create Streamable HTTP transport layer
            McpClientTransport transport = HttpClientStreamableHttpTransport
                    .builder(baseUrl)
                    .endpoint(endpoint)  // Set endpoint path
                    .build();

            // 2. Create client - simplified configuration
            client = McpClient.sync(transport)
                    .requestTimeout(Duration.ofSeconds(30))
                    // Don't enable any capabilities yet to ensure connection
                    .loggingConsumer((notification) -> {
                        System.out.println("[MCP Log] " + notification.data());
                    })
                    .progressConsumer((progress) -> {
                        System.out.println("[Progress] " + progress.progress());
                    })
                    .toolsChangeConsumer((tools) -> {
                        System.out.println("[Tools Change] Available tools: " + tools.size());
                    })
                    .build();

            // 3. Initialize connection
            System.out.println("\nInitializing connection...");
            client.initialize();
            System.out.println("✓ Connection successful!");

            // 4. List all tools
            System.out.println("\nGetting available tools...");
            McpSchema.ListToolsResult listToolsResult = client.listTools();

            if (listToolsResult.tools().isEmpty()) {
                System.out.println("⚠️ Server has no registered tools");
            } else {
                System.out.println("✓ Found " + listToolsResult.tools().size() + " tool(s):");
                listToolsResult.tools().forEach(tool -> {
                    System.out.println("\nTool name: " + tool.name());
                    if (tool.description() != null && !tool.description().isEmpty()) {
                        System.out.println("  Description: " + tool.description());
                    }
                    if (tool.inputSchema() != null) {
                        System.out.println("  Input parameters: Defined");
                    }
                });

                // 5. Try to find and call math-related tools
                // Priority: find calc_pi
                boolean foundCalcPi = listToolsResult.tools().stream()
                        .anyMatch(tool -> "calc_pi".equalsIgnoreCase(tool.name()));

                if (foundCalcPi) {
                    System.out.println("Found calc_pi tool, calling...");
                    callTool(client, "calc_pi", Map.of("prec", 20));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 7. Close connection
            if (client != null) {
                try {
                    client.closeGracefully();
                } catch (Exception e) {
                    System.err.println("Error closing: " + e.getMessage());
                }
            }
        }
    }

    private static void callTool(McpSyncClient client, String toolName, Map<String, Object> arguments) {
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(toolName, arguments);
        McpSchema.CallToolResult result = client.callTool(request);
        if (result.content() != null && !result.content().isEmpty()) {
            result.content().forEach(content -> {
                if (content instanceof McpSchema.TextContent textContent) {
                    System.out.println("  Text: " + textContent.text());
                } else if (content instanceof McpSchema.ImageContent imageContent) {
                    System.out.println("  Image: " + imageContent.mimeType() +
                            " (" + (imageContent.data() != null ? imageContent.data().length() : 0) + " bytes)");
                } else {
                    System.out.println("  Content: " + content);
                }
            });
        } else {
            System.out.println("  Tool returned no content");
        }
    }
}