package com.jd.oxygent.core.oxygent.mcpservers.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * CalculatorTool provides a simple calculator functionality as a tool for the MCP server.
 */
public class CalculatorTool implements BiFunction<McpSyncServerExchange,Map<String, Object>, CallToolResult> {
 
    private final Tool toolDefinition;
    
    public CalculatorTool() {
        this.toolDefinition =  McpSchema.Tool.builder()
                .name("calculator")
                .title("Basic calculator")
                .description("""
                        {
                            "type": "object",
                            "properties": {
                                "operation": {
                                    "type": "string"
                                },
                                "a": {
                                    "type": "number"
                                },
                                "b": {
                                    "type": "number"
                                }
                            },
                            "required": ["operation", "a", "b"]
                        }
        """).inputSchema(new JacksonMcpJsonMapper(new ObjectMapper()),"""
                                                                                {
                                                                                    "type": "object",
                                                                                    "properties": {
                                                                                        "operation": {
                                                                                            "type": "string"
                                                                                        },
                                                                                        "a": {
                                                                                            "type": "number"
                                                                                        },
                                                                                        "b": {
                                                                                            "type": "number"
                                                                                        }
                                                                                    },
                                                                                    "required": ["operation", "a", "b"]
                                                                                }
                                                    """)
                .build();
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
     * Apply the calculator operation on the given arguments.
     * 
     * @param arguments Map containing operation and operands
     * @return The result of the calculation
     */
    @Override
    public CallToolResult apply(McpSyncServerExchange mcpSyncServerExchange,Map<String, Object> arguments) {
        // Extract arguments
        String operation = (String) arguments.get("operation");
        Double a = Double.parseDouble(arguments.get("a").toString());
        Double b = Double.parseDouble(arguments.get("b").toString());
        
        // Perform calculation
        Double result;
        String resultMessage;
        
        switch (operation.toLowerCase()) {
            case "add":
            case "+":
                result = a + b;
                resultMessage = String.format("%.2f + %.2f = %.2f", a, b, result);
                break;
            case "subtract":
            case "-":
                result = a - b;
                resultMessage = String.format("%.2f - %.2f = %.2f", a, b, result);
                break;
            case "multiply":
            case "*":
                result = a * b;
                resultMessage = String.format("%.2f * %.2f = %.2f", a, b, result);
                break;
            case "divide":
            case "/":
                if (b == 0) {
                    return new CallToolResult(
                        List.of(new TextContent("Error: Division by zero")),
                        true
                    );
                }
                result = a / b;
                resultMessage = String.format("%.2f / %.2f = %.2f", a, b, result);
                break;
            default:
                return new CallToolResult(
                    List.of(new TextContent("Error: Unsupported operation. Supported operations are: add, subtract, multiply, divide or +, -, *, /")),
                    true
                );
        }
        
        return new CallToolResult(
            List.of(new TextContent(resultMessage)),
            false
        );
    }
}
