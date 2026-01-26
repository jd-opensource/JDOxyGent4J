package com.jd.oxygent.core.oxygent.mcpservers.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.web.reactive.function.client.WebClient;

public class MCPTest {

    public static void main(String[] args) {

//        WebFluxSseServerTransport webFluxSseServerTransport = new WebFluxSseServerTransport(new ObjectMapper(), "/mcp/message");

//        RouterFunction<?> routerFunction = webFluxSseServerTransport.getRouterFunction();

        StdioServerTransportProvider stdioServerTransport = new StdioServerTransportProvider(new JacksonMcpJsonMapper(new ObjectMapper()));

        CalculatorTool calculatorTool = new CalculatorTool();
        WeatherTool weatherTool = new WeatherTool(WebClient.builder(), "${WEATHER_API_KEY:demo_key}");

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

        // Register the calculator tool
        var calculatorToolRegistration = new McpServerFeatures.SyncToolSpecification(
                calculatorTool.getToolDefinition(),
                calculatorTool
        );

        // Register the weather tool
        var weatherToolRegistration = new McpServerFeatures.SyncToolSpecification(
                weatherTool.getToolDefinition(),
                weatherTool
        );

        syncServer.addTool(calculatorToolRegistration);
        syncServer.addTool(weatherToolRegistration);

        System.err.println("MCP Server initialized with capabilities: tools=" + syncServer.getServerCapabilities().tools() + ", prompts=" + syncServer.getServerCapabilities().prompts() + ", resources=" + syncServer.getServerCapabilities().resources());
        System.err.println("Server is ready to accept requests. Tools registered: " + syncServer.listTools().size());

        // Start the server - StdioServerTransportProvider automatically starts listening on System.in/System.out
        // We need to keep the main thread alive to process incoming requests
        // Note: Logging notifications should be sent from within tool handlers after client connection
        try {
            // The server will process requests from stdin
            // Keep the main thread alive to handle incoming messages
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Server interrupted");
        } finally {
            // Clean up resources
            syncServer.close();
        }
    }
}
