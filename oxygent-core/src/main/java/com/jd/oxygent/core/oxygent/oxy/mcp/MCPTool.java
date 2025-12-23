/*
 * Copyright 2025 JD.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this project except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.oxygent.core.oxygent.oxy.mcp;

import com.jd.oxygent.core.oxygent.oxy.BaseTool;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyState;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Individual proxy class for MCP server tools
 * <p>
 * This class represents a specific tool instance discovered from an MCP server. It acts as a lightweight proxy,
 * delegating actual execution to the parent MCP client while providing the standard BaseTool interface.
 *
 * <h3>Design Patterns</h3>
 * <ul>
 *     <li>Proxy Pattern: Proxies remote tools on MCP servers</li>
 *     <li>Adapter Pattern: Adapts MCP protocol to OxyGent tool interface</li>
 *     <li>Lightweight Design: Minimizes local state, delegates main functionality to MCP client</li>
 * </ul>
 *
 * <h3>Core Features</h3>
 * <ul>
 *     <li>Tool metadata management: Maintains tool name, description, and input schema</li>
 *     <li>Parameter schema conversion: Converts MCP input schema to OxyGent format</li>
 *     <li>Execution proxy: Proxies OxyGent requests to MCP server</li>
 *     <li>Multimedia content support: Handles various content types including text, image, audio, etc.</li>
 * </ul>
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *     <li>Created by BaseMCPClient during tool discovery process</li>
 *     <li>Registered in MAS system's tool space</li>
 *     <li>Receives OxyGent standard requests and converts them to MCP calls</li>
 *     <li>Processes MCP responses and returns OxyGent format results</li>
 * </ol>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MCPTool extends BaseTool {
    private BaseMCPClient mcpClient;
    private String serverName;
    private static final Logger logger = LoggerFactory.getLogger(MCPTool.class);

    /**
     * Construct MCP tool proxy instance
     * <p>
     * Creates a tool proxy based on tool definition obtained from MCP server and client connection.
     * Automatically extracts tool metadata and converts input parameter schema to OxyGent format.
     *
     * @param tool      MCP tool definition containing name, description, and input schema
     * @param mcpClient Synchronous client connection to MCP server
     */
    public MCPTool(McpSchema.Tool tool, BaseMCPClient mcpClient) {
        setName(tool.name());
        setDesc(tool.description());
        this.mcpClient = mcpClient;
        this.serverName = tool.name();


        // Assemble inputSchema
        Map<String, Object> inputSchema = new LinkedHashMap<>();
        inputSchema.put("type", tool.inputSchema().type());
        inputSchema.put("properties", extractProperties(tool.inputSchema().properties()));
        inputSchema.put("required", tool.inputSchema().required());
        this.setInputSchema(inputSchema);
        super.setDescForLlm();
        logger.info("tool.inputSchema={}", tool.inputSchema().properties());
    }

    /**
     * Extract and convert MCP tool property definitions
     * <p>
     * Converts MCP protocol property definition format to format recognized by OxyGent system.
     * Extracts type and description information for each parameter, building standardized property mapping.
     *
     * @param properties Original property definition mapping of MCP tool
     * @return Converted property definition mapping conforming to OxyGent format requirements
     */
    private Map<String, Object> extractProperties(Map<String, Object> properties) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {


            String paramName = entry.getKey();
            if (entry.getValue() instanceof Map) {
                Map<String, String> param = (Map<String, String>) entry.getValue();
                String typeName = param.get("type");
                boolean required = true; // Can be further determined by annotations
                String description = param.get("description"); // Can use custom annotations to describe parameters


                Map<String, Object> prop = new HashMap<>();
                prop.put("description", description);
                prop.put("type", typeName);
                result.put(paramName, prop);
            }

        }

        return result;
    }

    /**
     * Execute MCP tool call
     * <p>
     * Delegates OxyGent requests to MCP server for execution and handles various types of response content.
     * Supports multimedia content types including text, image, audio, embedded resources, and resource links.
     *
     * <h3>Execution Flow</h3>
     * <ul>
     *     <li>Verify MCP client connection status</li>
     *     <li>Construct MCP protocol tool call request</li>
     *     <li>Send request to MCP server and receive response</li>
     *     <li>Parse different types of response content</li>
     *     <li>Return unified format OxyResponse object</li>
     * </ul>
     *
     * <h3>Supported Content Types</h3>
     * <ul>
     *     <li>TextContent: Direct text content extraction</li>
     *     <li>ImageContent: Image data extraction</li>
     *     <li>AudioContent: Audio data extraction</li>
     *     <li>EmbeddedResource: Embedded resource converted to string</li>
     *     <li>ResourceLink: Resource link converted to string</li>
     * </ul>
     *
     * @param oxyRequest OxyGent standard request object containing tool parameters
     * @return OxyResponse object containing execution results and completion status
     * @throws RuntimeException if MCP server is not initialized
     */
    @Override
    public OxyResponse _execute(OxyRequest oxyRequest) {
        String toolName = this.getName();
        if (this.mcpClient instanceof StreamableMCPClient || this.mcpClient instanceof SSEMCPClient) {
            // Get shardData to pass _header or header parameters
            this.mcpClient.initClientSession(oxyRequest);
        }
        if (this.mcpClient == null) {
            throw new RuntimeException("Server " + this.getName() + " not initialized");
        }
        if ("url".equals(oxyRequest.getArguments().get("type"))) {
            if (oxyRequest.getArguments().get("timeout") == null) {
                oxyRequest.getArguments().put("timeout", 20_000);
            }
            oxyRequest.getArguments().put("ignoreCache", true);
        }
        McpSchema.CallToolRequest callToolRequest = new McpSchema.CallToolRequest(toolName, oxyRequest.getArguments());
        McpSchema.CallToolResult mcpResponse = this.mcpClient.clientSession.callTool(callToolRequest);
        List<String> results = new ArrayList<>();
        for (McpSchema.Content content : mcpResponse.content()) {
            if (content != null) {
                if (content instanceof McpSchema.TextContent) {
                    results.add(((McpSchema.TextContent) content).text());
                } else if (content instanceof McpSchema.ImageContent) {
                    results.add(((McpSchema.ImageContent) content).data());
                } else if (content instanceof McpSchema.AudioContent) {
                    results.add(((McpSchema.AudioContent) content).data());
                } else if (content instanceof McpSchema.EmbeddedResource) {
                    results.add(((McpSchema.EmbeddedResource) content).toString());
                } else if (content instanceof McpSchema.ResourceLink) {
                    results.add(((McpSchema.ResourceLink) content).toString());
                }
            }
        }

        Object output = results.isEmpty() ? "" :
                (results.size() == 1 ? results.get(0) : results);

        return new OxyResponse(OxyState.COMPLETED, output);
    }
}
