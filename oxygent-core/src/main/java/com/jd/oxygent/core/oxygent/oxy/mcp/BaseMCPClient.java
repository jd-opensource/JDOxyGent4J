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
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Base implementation class for MCP (Model Context Protocol) client
 * <p>
 * This class provides basic functionality for communicating with MCP servers, including tool discovery,
 * tool invocation, and resource management. MCP is a standardized protocol for integrating large
 * language models with external tools and data sources.
 *
 * <h3>Core Features</h3>
 * <ul>
 *     <li>MCP server connection management: Establish and maintain connections with MCP servers</li>
 *     <li>Tool discovery and registration: Automatically discover tools provided by MCP servers and register them in the system</li>
 *     <li>Tool invocation proxy: Convert OxyGent requests to MCP protocol calls</li>
 *     <li>Resource lifecycle management: Ensure proper cleanup of connections and resources</li>
 * </ul>
 *
 * <h3>Architecture Design</h3>
 * <ul>
 *     <li>Extends BaseTool: Reuses basic capabilities of the OxyGent tool system</li>
 *     <li>Thread safety: Uses ReentrantLock to ensure concurrent safety</li>
 *     <li>Resource stack management: Ensures proper resource release through AutoCloseableStack</li>
 *     <li>Asynchronous cleanup: Supports asynchronous resource cleanup to avoid blocking</li>
 * </ul>
 *
 * <h3>Usage Pattern</h3>
 * <pre>{@code
 * BaseMCPClient client = new SSEMCPClient("server_url");
 * client.init();           // Initialize connection
 * client.listTools();      // Discover and register tools
 * // Tools will be automatically registered in the MAS system
 * client.cleanup();        // Clean up resources
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class BaseMCPClient extends BaseTool {

    private static final Logger logger = LoggerFactory.getLogger(BaseMCPClient.class);

    protected List<String> includedToolNameList = new ArrayList<>();
    protected McpSyncClient clientSession = null;
    protected final ReentrantLock cleanupLock = new ReentrantLock();
    protected final AutoCloseableStack exitStack = new AutoCloseableStack();
    protected Object stdioContext = null;

    protected Map<String, String> headers = new HashMap<>();
    protected boolean isDynamicHeaders = false;
    protected boolean isInheritHeaders = false;
    protected boolean isKeepAlive = false;

    private static final Pattern DIGIT_PATTERN = Pattern.compile("text=\\{(.*?)}]", Pattern.DOTALL);

    /**
     * Default constructor
     * <p>
     * Initializes basic properties of the MCP client, including tool list and resource management components.
     */
    public BaseMCPClient() {
        super();
    }

    public BaseMCPClient(Map<String, Object> args) {
        if (MapUtils.isEmpty(args)) {
            logger.warn("header args is empty !!!!");
        } else {
            this.isKeepAlive = (Boolean) args.getOrDefault("is_keep_alive", false);
            this.isDynamicHeaders = (Boolean) args.getOrDefault("is_dynamic_headers", false);
            this.isInheritHeaders = (Boolean) args.getOrDefault("is_inherit_headers", false);
            this.headers = (Map<String, String>) args.getOrDefault("headers", new HashMap<>());
        }
    }

    abstract boolean initClientSession(OxyRequest oxyRequest);

    protected HttpRequest.Builder getHttpRequestBuilder(OxyRequest oxyRequest) {
        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder();
        if (!this.isDynamicHeaders && this.isKeepAlive && MapUtils.isNotEmpty(this.getHeaders())) {
            //this.getHeaders().keySet().stream().forEach(k-> httpRequestBuilder.header(k,this.getHeaders().get(k)));
            String[] params = headers.entrySet().stream()
                    .flatMap(entry -> Stream.of(entry.getKey(), entry.getValue()))
                    .toArray(String[]::new);
            httpRequestBuilder.headers(params);
        } else {
            Map<String, String> mergedHeaders;
            if (isDynamicHeaders) {
                // Get headers from shared data, use empty map if not exists
                Map<String, String> sharedHeaders = oxyRequest != null ? (Map<String, String>)
                        oxyRequest.getSharedData().getOrDefault("_headers", new HashMap<>()) : new HashMap<>();

                // If need to inherit headers
                if (isInheritHeaders) {
                    // Copy shared headers
                    Map<String, String> _headers = new HashMap<>(sharedHeaders);

                    // Remove host field (if exists)
                    _headers.remove("host");

                    // Merge: self.headers | _headers | shared_data.get("headers", {})
                    mergedHeaders = new HashMap<>(this.headers);
                    mergedHeaders.putAll(_headers);

                    // Merge headers from shared_data
                    Map<String, String> dataHeaders = oxyRequest != null ? (Map<String, String>)
                            oxyRequest.getSharedData().getOrDefault("headers", new HashMap<>()) : new HashMap<>();
                    mergedHeaders.putAll(dataHeaders);
                } else {
                    // Don't inherit headers, only use headers from shared data
                    Map<String, String> _headers = new HashMap<>(sharedHeaders);
                    _headers.remove("host");

                    // Merge: _headers | shared_data.get("headers", {})
                    mergedHeaders = new HashMap<>(_headers);

                    Map<String, String> dataHeaders = oxyRequest != null ? (Map<String, String>)
                            oxyRequest.getSharedData().getOrDefault("headers", new HashMap<>()) : new HashMap<>();
                    mergedHeaders.putAll(dataHeaders);
                }
            } else {
                // Don't use dynamic headers, directly use self.headers
                mergedHeaders = new HashMap<>(this.headers);
            }

            String[] params = mergedHeaders.entrySet().stream()
                    .flatMap(entry -> Stream.of(entry.getKey(), entry.getValue()))
                    .toArray(String[]::new);
            if (params.length > 0) {
                httpRequestBuilder.headers(params);
            }
        }
        return httpRequestBuilder;
    }

    /**
     * Discover and register tools from MCP server
     * <p>
     * This method connects to the MCP server, retrieves the list of available tools, and wraps each tool
     * as an MCPTool instance to register in the current MAS system. This enables the functionality
     * provided by the MCP server to be used by agents.
     *
     * <h3>Workflow</h3>
     * <ul>
     *     <li>Verify that the MCP client connection has been established</li>
     *     <li>Call the MCP protocol's listTools method to get the tool list</li>
     *     <li>Create MCPTool wrapper instances for each tool</li>
     *     <li>Register tools in the current MAS system's tool space</li>
     * </ul>
     *
     * @throws RuntimeException if MCP server is not initialized
     */
    public void listTools() {
        if (this.clientSession == null) {
            throw new RuntimeException("Server " + this.getName() + " not initialized");
        }

        logger.info("Listing tools...");
        McpSchema.ListToolsResult toolsResponse = this.clientSession.listTools();

        Map<String, Object> params = modelDumpExclude(
                "sse_url", "server_url", "headers", "middlewares",
                "included_tool_name_list", "name", "desc", "mcp_client",
                "server_name", "input_schema"
        );

        for (io.modelcontextprotocol.spec.McpSchema.Tool tool : toolsResponse.tools()) {
            this.includedToolNameList.add(tool.name());
            MCPTool mcpTool = new MCPTool(tool, this);

            var mas = this.getMas();
            if (mas != null) {
                mcpTool.setMas(mas);
                this.mas.addOxy(mcpTool);
            }
        }
    }

    /**
     * Execute tool calls through MCP server
     * <p>
     * This method converts OxyGent's standard requests to MCP protocol format, sends them to the MCP server
     * for execution, and parses the returned results into OxyGent response format. Supports regular expression
     * parsing of text content.
     *
     * <h3>Execution Flow</h3>
     * <ul>
     *     <li>Verify MCP client connection status</li>
     *     <li>Construct MCP protocol CallToolRequest</li>
     *     <li>Send request to MCP server and get response</li>
     *     <li>Parse response content and extract text results</li>
     *     <li>Return wrapped OxyResponse object</li>
     * </ul>
     *
     * <h3>Content Parsing</h3>
     * Uses regular expression {@code text=\\{(.*?)\\}]} to parse text content returned by the MCP server,
     * extracting JSON-formatted result data.
     *
     * @param oxyRequest OxyGent standard request object containing tool name and parameters
     * @return OxyResponse object containing execution results and status
     * @throws RuntimeException if MCP server is not initialized
     */
    @Override
    public OxyResponse _execute(OxyRequest oxyRequest) {
        String toolName = this.getName();
        if (this.clientSession == null) {
            throw new RuntimeException("Server " + this.getName() + " not initialized");
        }

        McpSchema.CallToolRequest callToolRequest = new McpSchema.CallToolRequest(toolName, oxyRequest.getArguments());
        McpSchema.CallToolResult mcpResponse = this.clientSession.callTool(callToolRequest);
        List<String> results = new ArrayList<>();
        for (var content : mcpResponse.content()) {
            if (content != null) {
                Matcher matcher = DIGIT_PATTERN.matcher(content.toString());

                if (matcher.find()) {
                    String textValue = "{" + matcher.group(1) + "}";
                    results.add(textValue);
                    logger.info("textValue:" + textValue);
                } else {
                    logger.info("textValue Not found!");
                }

            }
        }

        Object output = results.isEmpty() ? "" : (results.size() == 1 ? results.get(0) : results);
        return new OxyResponse(OxyState.COMPLETED, output);
    }

    /**
     * Clean up MCP server resources and connections
     * <p>
     * This method is responsible for cleaning up all resources related to the MCP server, including
     * closing connections, releasing memory, clearing client references, etc. Uses a thread-safe
     * approach to ensure proper resource release.
     *
     * <h3>Cleanup Flow</h3>
     * <ul>
     *     <li>Acquire cleanup lock to ensure concurrent safety</li>
     *     <li>Asynchronously execute resource stack close operations</li>
     *     <li>Handle exceptions during cleanup process</li>
     *     <li>Reset client and context references to null</li>
     * </ul>
     *
     * <h3>Exception Handling</h3>
     * <ul>
     *     <li>InterruptedException: Log warning and interrupt current thread</li>
     *     <li>Other exceptions: Suppressed during cleanup process to avoid affecting resource release</li>
     * </ul>
     *
     * @return CompletableFuture<Void> asynchronous result of cleanup operation
     */
    public CompletableFuture<Void> cleanup() {
        cleanupLock.lock();
        try {
            return CompletableFuture.runAsync(() -> {
                try {
                    exitStack.close();
                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        logger.warn("cleanup(): Operation was cancelled");
                        Thread.currentThread().interrupt();
                    }
                    // Suppress exceptions during cleanup
                } finally {
                    this.clientSession = null;
                    this.stdioContext = null;
                }
            });
        } finally {
            cleanupLock.unlock();
        }
    }

    /**
     * Model property export method, excluding specified fields
     * <p>
     * Gets all field values of the current object through reflection and excludes specified
     * sensitive or irrelevant fields. Used for generating tool parameters or configuration
     * information export.
     *
     * @param excludeFields array of field names to be excluded
     * @return map containing field names and values with specified fields excluded
     */
    private Map<String, Object> modelDumpExclude(String... excludeFields) {
        Set<String> excludeSet = Set.of(excludeFields);
        Map<String, Object> result = new HashMap<>();

        for (var field : this.getClass().getDeclaredFields()) {
            String name = field.getName();
            if (!excludeSet.contains(name)) {
                try {
                    field.setAccessible(true);
                    result.put(name, field.get(this));
                } catch (IllegalAccessException e) {
                    logger.warn(e.getMessage());
                }
            }
        }
        return result;
    }

    /**
     * Auto-closeable resource stack
     * <p>
     * Stack structure for managing multiple AutoCloseable resources, ensuring that all resources
     * are closed in LIFO order during cleanup. Even if some resources fail to close, it will
     * continue to close other resources.
     *
     * <h3>Design Features</h3>
     * <ul>
     *     <li>LIFO order: Resources added later are closed first</li>
     *     <li>Exception aggregation: Collects all close exceptions and throws them uniformly</li>
     *     <li>Error isolation: Single resource close failure doesn't affect other resources</li>
     * </ul>
     */
    private static class AutoCloseableStack implements AutoCloseable {
        private final Deque<AutoCloseable> stack = new ArrayDeque<>();

        public void push(AutoCloseable resource) {
            stack.push(resource);
        }

        @Override
        public void close() throws Exception {
            Exception first = null;
            while (!stack.isEmpty()) {
                try {
                    stack.pop().close();
                } catch (Exception e) {
                    if (first == null) {
                        first = e;
                    } else {
                        first.addSuppressed(e);
                    }
                }
            }
            if (first != null) {
                throw first;
            }
        }
    }
}