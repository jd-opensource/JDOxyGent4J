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
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.util.Map;

/**
 * MCP client implementation based on Server-Sent Events (SSE)
 * <p>
 * This class implements a client that communicates with MCP servers via the SSE protocol. SSE is an HTML5 standard
 * that allows servers to push real-time data streams to clients, particularly suitable for scenarios that require
 * continuous server updates.
 *
 * <h3>Core Features</h3>
 * <ul>
 *     <li>SSE connection management: Establish and maintain SSE connections with MCP servers</li>
 *     <li>Real-time data streaming: Support real-time data push from server to client</li>
 *     <li>Auto-reconnection: Built-in automatic reconnection mechanism after connection drops</li>
 *     <li>Low-latency communication: Lower network latency compared to polling approaches</li>
 * </ul>
 *
 * <h3>Applicable Scenarios</h3>
 * <ul>
 *     <li>MCP services requiring real-time data push</li>
 *     <li>Tool calls in long connection scenarios</li>
 *     <li>Streaming data processing and response</li>
 *     <li>Applications requiring server-initiated notifications</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * SSEMCPClient sseClient = new SSEMCPClient(
 *     "weather-service",
 *     "http://localhost:8080",
 *     "/mcp/sse"
 * );
 * sseClient.init();
 * // Client will automatically discover and register tools provided by MCP server
 * }</pre>
 *
 * <h3>Network Protocol</h3>
 * Uses HTTP SSE protocol for communication, with the following advantages:
 * <ul>
 *     <li>HTTP-based: Reuses existing HTTP infrastructure</li>
 *     <li>Unidirectional push: Server can actively push data to client</li>
 *     <li>Auto-reconnection: Browser native support for automatic reconnection after disconnection</li>
 *     <li>Text protocol: Easy to debug and monitor</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @see BaseMCPClient MCP client base class
 * @see HttpClientSseClientTransport SSE transport layer implementation
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SSEMCPClient extends BaseMCPClient {
    private static final Logger logger = LoggerFactory.getLogger(SSEMCPClient.class);

    /**
     * Base URL address of the MCP server
     * <p>
     * Complete URL including protocol, hostname and port, e.g.: http://localhost:8080
     */
    private String baseUrl = "";

    /**
     * SSE endpoint path
     * <p>
     * Relative path to the SSE endpoint on the server, e.g.: /mcp/sse
     */
    private String endpoint = "";

    /**
     * Construct an SSE MCP client instance
     * <p>
     * Creates an MCP client based on SSE protocol for establishing connection with the specified MCP server.
     *
     * @param name     Client name for identification and logging
     * @param baseUrl  Base URL of the MCP server, including protocol, host and port
     * @param endpoint Relative path of the SSE endpoint
     */
    public SSEMCPClient(String name, String baseUrl, String endpoint) {
        this.setName(name);
        this.baseUrl = baseUrl;
        this.endpoint = endpoint;
    }

    public SSEMCPClient(String name, String baseUrl, String endpoint, Map<String, Object> args) {
        super(args);
        this.setName(name);
        this.baseUrl = baseUrl;
        this.endpoint = endpoint;
    }

    /**
     * Initialize SSE connection to MCP server
     * <p>
     * Establishes SSE connection with the MCP server, completes client initialization, and automatically
     * discovers and registers tools provided by the server. This method performs the following operations:
     *
     * <h3>Initialization Flow</h3>
     * <ol>
     *     <li>Create HttpClientSseClientTransport transport layer</li>
     *     <li>Build synchronous MCP client instance</li>
     *     <li>Execute MCP protocol handshake and initialization</li>
     *     <li>Verify initialization status</li>
     *     <li>Discover and register tools provided by MCP server</li>
     * </ol>
     *
     * <h3>Exception Handling</h3>
     * <ul>
     *     <li>Logs error messages when connection fails</li>
     *     <li>Automatically calls cleanup() method to clean up resources</li>
     *     <li>Throws RuntimeException wrapping original exception</li>
     * </ul>
     *
     * <h3>Post-conditions</h3>
     * <ul>
     *     <li>MCP client connection is established and initialization completed</li>
     *     <li>Server tools are discovered and registered in OxyGent system</li>
     *     <li>Client is in available state and can accept tool call requests</li>
     * </ul>
     *
     * @throws RuntimeException when any error occurs during initialization process,
     *                          wrapping underlying connection exceptions, protocol exceptions, etc.
     * @see #listTools() Tool discovery and registration method
     * @see #cleanup() Resource cleanup method
     */
    @Override
    public void init() {
        try {
            initClientSession(null);
            logger.info("sse--Starting to load {} tool methods into oxy:", this.getName());
            this.listTools();

            logger.info("MCP Server started via sse.");
        } catch (Exception e) {
            logger.error("Error initializing server {}: {}", this.getName(), e.getMessage(), e);
            this.cleanup();
            throw new RuntimeException("Server " + this.getName() + " error", e);
        }
    }

    @Override
    boolean initClientSession(OxyRequest oxyRequest) {
        try {
            HttpRequest.Builder httpRequestBuilder = this.getHttpRequestBuilder(oxyRequest);
            HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(this.baseUrl).sseEndpoint(endpoint).requestBuilder(httpRequestBuilder).build();

            this.clientSession = McpClient.sync(transport).build();
            logger.info("{}--sse--Starting initialization....", this.getName());
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