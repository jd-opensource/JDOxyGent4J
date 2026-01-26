package io.github.innobridge.mcpserver.tools;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import lombok.extern.slf4j.Slf4j;

/**
 * WeatherTool provides weather information as a tool for the MCP server.
 */
@Component
@Slf4j
public class WeatherTool implements Function<Map<String, Object>, CallToolResult> {
    
    private final Tool toolDefinition;
    private final WebClient weatherClient;
    private final String apiKey;
    
    public WeatherTool(WebClient.Builder webClientBuilder, String apiKey) {
        this.weatherClient = webClientBuilder
            .baseUrl("https://api.weatherapi.com/v1")
            .build();
        this.apiKey = apiKey;
        
        this.toolDefinition = new Tool(
            "get_current_weather",
            "Get the current weather in a given location",
            """
            {
                "type": "object",
                "properties": {
                    "location": {
                        "type": "string",
                        "description": "The name of the city e.g. San Francisco, CA"
                    },
                    "format": {
                        "type": "string",
                        "enum": ["celsius", "fahrenheit"],
                        "description": "The format to return the weather in"
                    }
                },
                "required": ["location"]
            }
            """
        );
    }
    
    /**
     * Get the tool definition for registration with the MCP server.
     * 
     * @return The Tool definition
     */
    public Tool getToolDefinition() {
        return toolDefinition;
    }
    
    /**
     * Apply the weather tool on the given arguments.
     * 
     * @param arguments Map containing location and format
     * @return The weather information
     */
    @Override
    public CallToolResult apply(Map<String, Object> arguments) {
        try {
            // Extract arguments
            String location = (String) arguments.get("location");
            if (location == null || location.trim().isEmpty()) {
                return new CallToolResult(
                    List.of(new TextContent("Error: Location is required")),
                    true
                );
            }
            
            Format format = Format.CELSIUS;  // Default format
            String formatStr = (String) arguments.get("format");
            if (formatStr != null && !formatStr.trim().isEmpty() && 
                formatStr.toLowerCase().equals("fahrenheit")) {
                format = Format.FAHRENHEIT;
            }
            
            // Call weather API
            String uri = UriComponentsBuilder.fromPath("/current.json")
                    .queryParam("key", apiKey)
                    .queryParam("q", location)
                    .queryParam("aqi", "no")
                    .build()
                    .toUriString();
            
            log.info("Calling weather API with URI: {}", uri);
            
            Response response = weatherClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(Response.class)
                    .block();
            
            // Format response
            String weatherText;
            if (format == Format.CELSIUS) {
                weatherText = String.format(
                    "Current weather in %s, %s: %s, %.1f°C, Precipitation: %.1f mm",
                    response.location().name(),
                    response.location().country(),
                    response.current().condition().text(),
                    response.current().temp_c(),
                    response.current().precipIn() * 25.4 // Convert inches to mm
                );
            } else {
                weatherText = String.format(
                    "Current weather in %s, %s: %s, %.1f°F, Precipitation: %.1f in",
                    response.location().name(),
                    response.location().country(),
                    response.current().condition().text(),
                    response.current().temp_c() * 9/5 + 32, // Convert C to F
                    response.current().precipIn()
                );
            }
            
            return new CallToolResult(
                List.of(new TextContent(weatherText)),
                false
            );
        } catch (Exception e) {
            log.error("Error getting weather", e);
            return new CallToolResult(
                List.of(new TextContent("Error getting weather: " + e.getMessage())),
                true
            );
        }
    }
    
    enum Format {
        CELSIUS("celsius"), 
        FAHRENHEIT("fahrenheit");

        public final String formatName;

        Format(String formatName) {
            this.formatName = formatName;
        }

        @Override
        public String toString() {
            return formatName;
        }
    }
    
    @JsonInclude(Include.NON_NULL)
    public record Request(
        @JsonProperty(required = true) 
        @JsonPropertyDescription("The name of the city e.g. San Francisco, CA") 
        String location,
        @JsonProperty(required = true) 
        @JsonPropertyDescription("The format to return the weather in, e.g. 'celsius' or 'fahrenheit'") 
        Format format) {
    }
    
    public record Response(
        Location location,
        Current current
    ) {
        public record Current(
            double temp_c,
            @JsonProperty("precip_in")
            double precipIn,
            Condition condition
        ) {}
    
        public record Condition(
            String text
        ) {}

        public record Location(
            String name,
            String country
        ) {}
    }
}
