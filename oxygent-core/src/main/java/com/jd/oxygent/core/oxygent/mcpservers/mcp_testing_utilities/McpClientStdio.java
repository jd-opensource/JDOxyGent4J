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
            // 1. 创建Stdio传输层
            // 配置服务器参数，启动node.js MCP服务器
            ServerParameters params = ServerParameters.builder("D:\\Program Files\\node-v22.20.0-win-x64\\npx.cmd")
                    .args("-y", "@modelcontextprotocol/server-everything", "stdio")
                    .build();

            // 创建Stdio传输
            McpClientTransport transport = new StdioClientTransport(params,new JacksonMcpJsonMapper(new ObjectMapper()));

            System.out.println("Stdio transport created, connecting to server...");

            // 2. 创建客户端
            client = McpClient.sync(transport)
                    .requestTimeout(Duration.ofSeconds(30))
                    // 根据服务器能力调整
                    .capabilities(McpSchema.ClientCapabilities.builder()
                            .roots(false)       // 如果服务器不支持roots则设为false
                            .sampling()         // 保持启用
                            .elicitation()      // 保持启用
                            .build())
                    // 添加日志消费者用于调试
                    .loggingConsumer((notification) -> {
                        System.out.println("[MCP Log] " + notification.data());
                    })
                    // 添加进度消费者
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

            // 5. 查找特定工具
            String targetTool = "calc_pi"; // 你可以改为任何你需要的工具名

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

                // 如果找不到calc_pi，可以尝试调用其他工具
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
            // 6. 优雅关闭客户端
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