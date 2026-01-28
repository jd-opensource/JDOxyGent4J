package com.jd.oxygent.core.oxygent.mcpservers.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.web.reactive.function.client.WebClient;

public class CalculatorAndWeatherToolStarter {

    public static void main(String[] args) {

//      WebFluxSseServerTransport webFluxSseServerTransport = new WebFluxSseServerTransport(new ObjectMapper(), "/mcp/message");

//      RouterFunction<?> routerFunction = webFluxSseServerTransport.getRouterFunction();

        StdioServerTransportProvider stdioServerTransport = new StdioServerTransportProvider(new JacksonMcpJsonMapper(new ObjectMapper()));

        CalculatorTool calculatorTool = new CalculatorTool();
        WeatherTool weatherTool = new WeatherTool(WebClient.builder(), "013a6d7921984db68bf82618262701");

        // Create a server with custom configuration
        McpSyncServer syncServer = McpServer.sync(stdioServerTransport)
                .serverInfo(new McpSchema.Implementation("my-server", "1.0.0"))
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)         // Enable tool support
                        .prompts(false)      // Change to false if not implementing prompts
                        .resources(false,false)
                        .logging()           // Enable logging support
                        .build())
                .build();

        // Register the calculator tool using builder pattern
        var calculatorToolRegistration = McpServerFeatures.SyncToolSpecification.builder()
                .tool(calculatorTool.getToolDefinition())
                .callHandler((exchange, callToolRequest) -> {
                    return calculatorTool.apply(exchange, callToolRequest.arguments());
                })
                .build();

        // Register the weather tool using builder pattern
        var weatherToolRegistration = McpServerFeatures.SyncToolSpecification.builder()
                .tool(weatherTool.getToolDefinition())
                .callHandler((exchange, callToolRequest) -> {
                    return weatherTool.apply(exchange, callToolRequest.arguments());
                })
                .build();

        syncServer.addTool(calculatorToolRegistration);
        syncServer.addTool(weatherToolRegistration);

        System.err.println("MCP Server initialized with capabilities: tools=" + syncServer.getServerCapabilities().tools() + ", prompts=" + syncServer.getServerCapabilities().prompts() + ", resources=" + syncServer.getServerCapabilities().resources());
        System.err.println("Server is ready to accept requests. Tools registered: " + syncServer.listTools().size());
    }
}
