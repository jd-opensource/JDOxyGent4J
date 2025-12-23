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

import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MCP client implementation based on HTTP streaming transport
 * <p>
 * This class implements a client that communicates with MCP servers via HTTP streaming protocol.
 * Streaming HTTP transport supports bidirectional data stream transmission, suitable for MCP service
 * scenarios requiring large data transfers or real-time interaction.
 *
 * <h3>Core Features</h3>
 * <ul>
 *     <li>Streaming transport: Support HTTP streaming data transmission, improving large data transfer efficiency</li>
 *     <li>Bidirectional communication: Support bidirectional data streams between client and server</li>
 *     <li>Middleware support: Support HTTP middleware extensions to enhance request processing capabilities</li>
 *     <li>Custom headers: Support custom HTTP request headers to meet authentication and configuration requirements</li>
 * </ul>
 *
 * <h3>Applicable Scenarios</h3>
 * <ul>
 *     <li>Large data volume MCP tool calls</li>
 *     <li>Services requiring streaming responses</li>
 *     <li>Complex HTTP authentication and authorization scenarios</li>
 *     <li>Request chains requiring middleware processing</li>
 * </ul>
 *
 * <h3>Transport Characteristics</h3>
 * <ul>
 *     <li>HTTP/1.1 Chunked transfer: Support chunked transfer encoding</li>
 *     <li>Persistent connections: Reuse HTTP connections to reduce connection overhead</li>
 *     <li>Streaming processing: Process data while receiving, reducing memory usage</li>
 *     <li>Error recovery: Support detection and recovery of transmission errors</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * Map<String, String> headers = new HashMap<>();
 * headers.put("Authorization", "Bearer token123");
 * headers.put("Content-Type", "application/json");
 *
 * StreamableMCPClient client = new StreamableMCPClient(
 *     "data-service",
 *     "http://localhost:8080",
 *     "/mcp/stream"
 * );
 * client.setHeaders(headers);
 * client.init();
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @see BaseMCPClient MCP client base class
 * @see HttpClientStreamableHttpTransport HTTP streaming transport implementation
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class StreamableMCPClient extends BaseMCPClient {

    private static final Logger logger = LoggerFactory.getLogger(StreamableMCPClient.class);

    /**
     * Base URL address of the MCP server
     * <p>
     * Complete URL including protocol, hostname and port, e.g.: http://localhost:8080
     */
    private String baseUrl = "";

    /**
     * Streaming HTTP endpoint path
     * <p>
     * Relative path to the streaming HTTP endpoint on the server, e.g.: /mcp/stream
     */
    private String endpoint = "";

    /**
     * HTTP middleware list
     * <p>
     * Support middleware processing for HTTP requests and responses, can be used for logging,
     * request transformation, response handling and other extension functions.
     */
    private List<Object> middlewares = new ArrayList<>();


    /**
     * Construct a streaming HTTP MCP client instance
     * <p>
     * Creates an MCP client based on HTTP streaming transport protocol for establishing
     * efficient streaming data connections with the specified MCP server.
     *
     * @param name     Client name for identification and logging
     * @param baseUrl  Base URL of the MCP server, including protocol, host and port
     * @param endpoint Relative path of the streaming HTTP endpoint
     */
    public StreamableMCPClient(String name, String baseUrl, String endpoint) {
        this.setName(name);
        this.baseUrl = baseUrl;
        this.endpoint = endpoint;
    }

    public StreamableMCPClient(String name, String baseUrl, String endpoint, Map<String, Object> args) {
        super(args);
        this.setName(name);
        this.baseUrl = baseUrl;
        this.endpoint = endpoint;
    }

    /**
     * Initialize HTTP streaming connection to MCP server
     * <p>
     * Establishes HTTP streaming connection with the MCP server, completes client initialization,
     * and automatically discovers and registers tools provided by the server. This method supports
     * custom HTTP headers and middleware configuration.
     *
     * <h3>Initialization Flow</h3>
     * <ol>
     *     <li>Create HttpClientStreamableHttpTransport transport layer</li>
     *     <li>Configure endpoint path and custom parameters</li>
     *     <li>Build synchronous MCP client instance</li>
     *     <li>Execute MCP protocol handshake and initialization</li>
     *     <li>Verify initialization status</li>
     *     <li>Discover and register tools provided by MCP server</li>
     * </ol>
     *
     * <h3>Transport Configuration</h3>
     * <ul>
     *     <li>Use specified baseUrl as server address</li>
     *     <li>Configure endpoint as streaming transport endpoint</li>
     *     <li>Apply custom HTTP headers (if set)</li>
     *     <li>Integrate HTTP middleware processing chain</li>
     * </ul>
     *
     * <h3>Exception Handling</h3>
     * <ul>
     *     <li>Logs detailed error messages when connection fails</li>
     *     <li>Automatically calls cleanup() method to clean up resources</li>
     *     <li>Throws RuntimeException wrapping original exception</li>
     * </ul>
     *
     * <h3>Post-conditions</h3>
     * <ul>
     *     <li>HTTP streaming connection established and initialization completed</li>
     *     <li>Server tools discovered and registered in OxyGent system</li>
     *     <li>Client in available state, supports streaming data transmission</li>
     * </ul>
     *
     * @throws RuntimeException when any error occurs during initialization process,
     *                          wrapping underlying connection exceptions, protocol exceptions, transport exceptions, etc.
     * @see #listTools() Tool discovery and registration method
     * @see #cleanup() Resource cleanup method
     * @see HttpClientStreamableHttpTransport HTTP streaming transport layer
     */
    @Override
    public void init() {
        if (initClientSession(null)) {
            logger.info("streamable--Starting to load {} tool methods into oxy:", this.getName());
            this.listTools();
            logger.info("MCP Server started via streamable.");
        }
    }

    @Override
    public boolean initClientSession(OxyRequest oxyRequest) {
        try {

            HttpRequest.Builder httpRequestBuilder = this.getHttpRequestBuilder(oxyRequest);
            HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder(this.baseUrl).endpoint(endpoint).requestBuilder(httpRequestBuilder).build();

            this.clientSession = McpClient.sync(transport).build();
            logger.info("{}--streamable--Starting initialization....", this.getName());
            this.clientSession.initialize();
            logger.info("Is initialized: " + this.clientSession.isInitialized());
            return true;
        } catch (Exception e) {
            logger.error("Error initializing server {}: {}", this.getName(), e.getMessage(), e);
            this.cleanup();
            return false;
        }
    }
}