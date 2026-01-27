package com.jd.oxygent.core.oxygent.mcpservers.example;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.EnableMcpServer;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.jd.oxygent.core.oxygent.mcpservers.engine.McpServer;

/**
 * Calculator service providing arithmetic operations and weather information tools.
 * Exposes MCP-compatible tools for use with the Model Context Protocol server.
 */
public class CalculatorService {
    /**
     * Performs arithmetic operations based on the specified operation.
     * 
     * @param operation The operation to perform: add, subtract, multiply, divide
     * @param a First number
     * @param b Second number
     * @return Formatted result of the arithmetic operation
     */
    @MCPTool(name = "calculator",
            description = "A simple calculator that performs arithmetic operations")
    public String calculate(
            @ToolParam(description = "The operation to perform: add, subtract, multiply, divide")
            String operation,

            @ToolParam(description = "First number")
            double a,

            @ToolParam(description = "Second number")
            double b) {

        double result;
        String operationSymbol;

        switch (operation.toLowerCase()) {
            case "add":
                result = a + b;
                operationSymbol = "+";
                break;
            case "subtract":
                result = a - b;
                operationSymbol = "-";
                break;
            case "multiply":
                result = a * b;
                operationSymbol = "*";
                break;
            case "divide":
                if (b == 0) {
                    return "Error: Division by zero";
                }
                result = a / b;
                operationSymbol = "/";
                break;
            default:
                return "Error: Unsupported operation. Use add, subtract, multiply, or divide";
        }

        return String.format("%.2f %s %.2f = %.2f", a, operationSymbol, b, result);
    }

    /**
     * Gets current weather information for a specified location.
     * 
     * @param location City name (e.g., Beijing, Shanghai)
     * @param unit Temperature unit (celsius or fahrenheit), defaults to celsius
     * @return Formatted weather information
     */
    @MCPTool(name = "get_current_weather",
            description = "Get current weather for a location")
    public String getWeather(
            @ToolParam(description = "City name, e.g., Beijing, Shanghai")
            String location,

            @ToolParam(description = "Temperature unit: celsius or fahrenheit",
                    required = false,
                    defaultValue = "celsius",
                    enumValues = "[\"celsius\", \"fahrenheit\"]")
            String unit) {

        // Real weather API can be called here
        return String.format("Current weather in %s: Sunny, 22°%s", location, unit.equals("fahrenheit") ? "F" : "C");
    }

    /**
     * Main method to start the MCP server with stdio mode.
     * 
     * @param args Command line arguments
     */
    @EnableMcpServer(mode = "stdio")
    public static void main(String[] args) {
        McpServer.start();
    }
}