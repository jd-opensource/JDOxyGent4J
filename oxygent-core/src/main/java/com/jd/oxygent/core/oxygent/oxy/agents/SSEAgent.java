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
package com.jd.oxygent.core.oxygent.oxy.agents;

import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyState;
import com.jd.oxygent.core.oxygent.utils.LogUtils;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SSE Agent - Streaming Remote Agent based on Server-Sent Events
 *
 * <p>SSEAgent is a remote agent implementation specifically designed for handling streaming data transmission,
 * providing real-time bidirectional communication capabilities based on Server-Sent Events (SSE) technology.
 * It inherits from RemoteAgent and focuses on handling long connections and real-time data streams.</p>
 *
 * <p>Core Features:</p>
 * <ul>
 *     <li>Streaming Communication: Real-time data stream transmission based on SSE</li>
 *     <li>Reactive Programming: Non-blocking IO implementation using Spring WebFlux</li>
 *     <li>Automatic Retry: Automatic retry mechanism support for connection failures</li>
 *     <li>Call Stack Sharing: Support for distributed call stack information passing</li>
 *     <li>Custom Headers: Support for custom HTTP headers like authentication and application identification</li>
 *     <li>Event Filtering: Intelligent filtering and processing of different types of SSE events</li>
 * </ul>
 *
 * <p>SSE Event Processing:</p>
 * <ul>
 *     <li>answer events: Handle final answers and intermediate results</li>
 *     <li>tool_call events: Handle tool invocation information</li>
 *     <li>observation events: Handle tool execution result observations</li>
 *     <li>heartbeat events: Maintain connection active status</li>
 *     <li>done events: Indicate completion of data stream transmission</li>
 * </ul>
 *
 * <p>Applicable Scenarios:</p>
 * <ul>
 *     <li>Real-time Conversation: Dialogue systems requiring streaming responses</li>
 *     <li>Long-term Reasoning: Real-time feedback for complex reasoning processes</li>
 *     <li>Collaborative Agents: Real-time coordination between multiple agents</li>
 *     <li>Monitoring Dashboards: Real-time status updates and event notifications</li>
 *     <li>Stream Analysis: Real-time processing feedback for big data streams</li>
 * </ul>
 *
 * <p>Technical Advantages:</p>
 * <ul>
 *     <li>Low Latency: Real-time data transmission, reducing user waiting time</li>
 *     <li>High Reliability: Automatic retry and error recovery mechanisms</li>
 *     <li>Scalability: Support for large numbers of concurrent connections</li>
 *     <li>Standardization: Based on W3C standard SSE protocol</li>
 * </ul>
 *
 * <p>Configuration Example:</p>
 * <pre>{@code
 * SSEAgent sseAgent = SSEAgent.builder()
 *     .name("Streaming AI Assistant")
 *     .serverUrl("https://ai-stream.example.com/sse")
 *     .isShareCallStack(true)
 *     .maxRetries(3)
 *     .retryDelayMs(2000)
 *     .customHeaders(Map.of(
 *         "Authorization", "Bearer token123",
 *         "app_id", "myapp"
 *     ))
 *     .webClient(WebClient.builder()
 *         .codecs(c -> c.defaultCodecs().maxInMemorySize(32 * 1024 * 1024))
 *         .build())
 *     .build();
 *
 * // Execute streaming request
 * OxyRequest request = OxyRequest.builder()
 *     .query("Please analyze market trends in detail")
 *     .build();
 *
 * // Will receive real-time analysis process and final results
 * OxyResponse response = sseAgent.execute(request);
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@SuperBuilder
@Slf4j
public class SSEAgent extends RemoteAgent {

    /**
     * Whether to share call stack information
     *
     * <p>Controls whether to pass call stack information to remote services in SSE requests:</p>
     * <ul>
     *     <li>true: Pass complete call chain information, supporting distributed tracing</li>
     *     <li>false: Set caller to "user", simplifying call relationships</li>
     * </ul>
     *
     * <p>Enabling call stack sharing helps with:</p>
     * <ul>
     *     <li>Call chain tracing in distributed systems</li>
     *     <li>Maintaining collaborative context between agents</li>
     *     <li>Problem localization and performance analysis</li>
     * </ul>
     */
    @Builder.Default
    protected boolean isShareCallStack = true;

    /**
     * Custom HTTP headers
     *
     * <p>Used to add additional HTTP header information in SSE requests, supporting various authentication and identification needs:</p>
     * <ul>
     *     <li>Authentication information: Authorization Bearer tokens</li>
     *     <li>Application identification: app_id, client_id, etc.</li>
     *     <li>Version information: API version, client version</li>
     *     <li>Tracing identifiers: trace_id, correlation_id</li>
     *     <li>Other metadata: user_id, tenant_id, etc.</li>
     * </ul>
     *
     * <p>Example configuration:</p>
     * <pre>{@code
     * Map.of(
     *     "Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGc...",
     *     "app_id", "oxygent-client",
     *     "X-Trace-Id", "trace-12345"
     * )
     * }</pre>
     */
    @Builder.Default
    protected Map<String, String> customHeaders = new HashMap<>();

    /**
     * Maximum retry count
     *
     * <p>Maximum number of retries when SSE connection fails. Default is 1 retry,
     * meaning a total of 2 attempts (initial attempt + 1 retry).</p>
     *
     * <p>Retry scenarios include:</p>
     * <ul>
     *     <li>Network connection timeout</li>
     *     <li>Server temporarily unavailable</li>
     *     <li>HTTP status code errors</li>
     *     <li>SSE stream unexpected interruption</li>
     * </ul>
     */
    @Builder.Default
    protected int maxRetries = 1;

    /**
     * Retry delay time (milliseconds)
     *
     * <p>Wait time between two retries, default is 1000 milliseconds (1 second).
     * Appropriate retry delay can avoid putting pressure on the server while giving time for temporary failures to recover.</p>
     */
    @Builder.Default
    protected long retryDelayMs = 1000;

    /**
     * WebClient instance
     *
     * <p>Spring WebFlux client for handling HTTP requests and SSE streams.
     * Default configuration includes:</p>
     * <ul>
     *     <li>16MB memory buffer: Accommodates large response data</li>
     *     <li>Reactive programming: Supports non-blocking IO operations</li>
     *     <li>Automatic encoding/decoding: JSON and SSE format support</li>
     * </ul>
     *
     * <p>WebClient configuration can be customized to meet special needs:</p>
     * <pre>{@code
     * WebClient.builder()
     *     .codecs(c -> c.defaultCodecs().maxInMemorySize(32 * 1024 * 1024))
     *     .defaultHeader("User-Agent", "OxyGent-SSE-Client/1.0")
     *     .build()
     * }</pre>
     */
    @Builder.Default
    protected WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
            .build();

    /**
     * Initialize SSE Agent
     * Retrieve organization structure information from remote server and cache locally
     * Use WebClient instead of traditional HttpURLConnection for HTTP requests
     */
    @Override
    public void init() {
        super.init();

        try {
            // Use WebClient to send GET request to retrieve organization information
            // Set 10 second timeout to avoid long blocking
            var requestSpec = webClient.get()
                    .uri(getServerUrl() + "/get_organization");

            // Add custom HTTP headers to initialization request
            if (customHeaders != null && !customHeaders.isEmpty()) {
                for (Map.Entry<String, String> header : customHeaders.entrySet()) {
                    requestSpec = requestSpec.header(header.getKey(), header.getValue());
                }
                log.debug("Added {} custom headers to organization request", customHeaders.size());
            }

            String responseBody = requestSpec
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (responseBody != null) {
                // Parse JSON response and extract organization structure data
                var response = JsonUtils.parseObject(responseBody, Map.class);
                @SuppressWarnings("unchecked")
                var data = (Map<String, Object>) response.get("data");
                super.setOrg((Map<String, Object>) data.get("organization"));

                log.info("Initialized SSE Agent with organization from: {}", getServerUrl());
            }
        } catch (Exception e) {
            // Initialization failure will not prevent Agent creation, only log warning
            log.warn("Failed to fetch organization from server: {}", e.getMessage());
        }
    }

    /**
     * Core method for executing SSE requests
     * Use WebClient to establish SSE connection and handle real-time data streams
     * Support retry mechanism, default retry 1 time
     *
     * @param request OxyRequest request object containing query information and call context
     * @return OxyResponse response object containing execution results
     */
    @Override
    public OxyResponse _execute(OxyRequest request) {
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    log.info("Retrying SSE connection (attempt {}/{}). {}", attempt + 1, maxRetries + 1, getServerUrl());
                    // Wait for specified time before retry
                    Thread.sleep(retryDelayMs);
                } else {
                    log.info("Initiating SSE connection. {}", getServerUrl());
                }

                return executeSSERequest(request);

            } catch (Exception e) {
                lastException = e;
                log.warn("SSE execution attempt {} failed: {}", attempt + 1, e.getMessage());

                // If this is the last attempt, do not retry
                if (attempt == maxRetries) {
                    log.error("SSE execution failed after {} attempts", maxRetries + 1, e);
                    break;
                }
            }
        }

        // Build failure response
        return OxyResponse.builder()
                .oxyRequest(request)
                .state(OxyState.FAILED)
                .output("SSE execution failed after " + (maxRetries + 1) + " attempts: " +
                        (lastException != null ? lastException.getMessage() : "Unknown error"))
                .build();
    }

    /**
     * Private method for executing a single SSE request
     * Extracted from the original _execute method to facilitate retry logic implementation
     *
     * @param request OxyRequest request object
     * @return OxyResponse response object
     * @throws Exception Any exception during execution
     */
    protected OxyResponse executeSSERequest(OxyRequest request) throws Exception {
        try {
            // === Step 1: Build request payload ===
            // Build payload, corresponding to Python version's payload construction logic
            var payload = new HashMap<String, Object>();

            // Copy basic fields, excluding specific fields (corresponding to Python's exclude)
            payload.put("query", request.getQuery());
            payload.put("current_trace_id", request.getCurrentTraceId());
            payload.put("node_id", request.getNodeId());
            payload.put("session_name", request.getSessionName());

            // Add arguments content to top level
            if (request.getArguments() != null) {
                payload.putAll(request.getArguments());
            }

            // Set caller category to user
            payload.put("caller_category", "user");

            // === Step 2: Handle call stack logic ===
            // Decide whether to share call stack information based on isShareCallStack configuration
            if (isShareCallStack && request.getCallStack() != null && !request.getCallStack().isEmpty()) {
                var callStack = request.getCallStack();
                var nodeIdStack = request.getNodeIdStack();

                // Remove the last element (corresponding to Python's [:-1])
                // This is done to avoid including the current node in the call stack
                if (callStack.size() > 1) {
                    payload.put("call_stack", callStack.subList(0, callStack.size() - 1));
                }
                if (nodeIdStack != null && nodeIdStack.size() > 1) {
                    payload.put("node_id_stack", nodeIdStack.subList(0, nodeIdStack.size() - 1));
                }
            } else {
                // When not sharing call stack, set caller to user
                payload.put("caller", "user");
            }

            // === Step 3: Use WebClient to handle SSE stream ===
            // Use atomic references and counters to handle synchronous waiting for asynchronous reactive streams
            var answer = new AtomicReference<String>("");  // Store final answer
            var latch = new CountDownLatch(1);             // Used for synchronous waiting for stream processing completion
            var errorRef = new AtomicReference<Exception>(); // Store possible exceptions

            // Create SSE stream: send POST request and receive Server-Sent Events
            // Use String type to avoid automatic JSON parsing, manually handle data format
            var url = this.isOxyAgent() ? getServerUrl() + "/sse/chat" : getServerUrl();

            var requestSpec = webClient.post()
                    .uri(url)           // SSE endpoint
                    .contentType(MediaType.APPLICATION_JSON)     // Request body is JSON
                    .accept(MediaType.TEXT_EVENT_STREAM)         // Accept SSE stream
                    .bodyValue(payload);                         // Send constructed payload

            // Add custom HTTP headers
            if (customHeaders != null && !customHeaders.isEmpty()) {
                for (Map.Entry<String, String> header : customHeaders.entrySet()) {
                    requestSpec = requestSpec.header(header.getKey(), header.getValue());
                }
                log.debug("Added {} custom headers to SSE request", customHeaders.size());
            }

            Flux<ServerSentEvent<String>> sseFlux = requestSpec
                    .retrieve()                                  // Get response
                    .bodyToFlux(String.class)                   // First get raw string stream
                    .map(rawData -> {
                        // Manually build ServerSentEvent to avoid automatic JSON parsing
                        return ServerSentEvent.<String>builder()
                                .data(rawData)
                                .build();
                    })
                    .onErrorContinue((throwable, obj) -> {
                        // Ignore parsing errors, continue processing subsequent data
                        log.warn("SSE parsing warning (continuing): {}", throwable.getMessage());
                    });

            StringBuilder result = new StringBuilder();
            // === Step 4: Subscribe to SSE stream and handle events ===
            sseFlux.subscribe(
                    // onNext: Handle each SSE event
                    event -> {
                        try {
                            Object eventData = event.data();
                            log.info(LogUtils.ANSI_GREEN + "Received SSE event: {}" + LogUtils.ANSI_RESET_ALL, eventData);
                            String data;

                            // Now eventData should always be String type
                            data = (String) eventData;

                            // Handle special non-JSON data (such as heartbeat)
                            if ("heartbeat".equals(data) || data.trim().isEmpty()) {
                                log.debug("Received heartbeat or empty data, skipping: {}", data);
                                return; // Skip heartbeat and empty data
                            }

                            // Check if end signal is received
                            if ("done".equals(data) || "DONE".equals(data)) {
                                log.info("Received request to terminate SSE connection: {}. {}", data, getServerUrl());
                                latch.countDown(); // Notify main thread that processing is complete
                                return;
                            }

                            // Handle non-empty event data
                            if (data != null && !data.isBlank()) {
                                Map<String, Object> eventMap;
                                try {
                                    // Try to parse as JSON object
                                    eventMap = JsonUtils.parseObject(data, Map.class);
                                    if (eventMap == null) {
                                        log.debug("Received non-JSON data, skipping: {}", data);
                                        return;
                                    }
                                } catch (Exception e) {
                                    // If not valid JSON, skip this event
                                    log.debug("Failed to parse JSON data (skipping): {} - Error: {}", data, e.getMessage());
                                    return;
                                }
//                                var type = (String) eventMap.get("type");
                                var type = Optional.ofNullable((String) eventMap.get("type"))
                                        .filter(s -> !s.trim().isEmpty())
                                        .orElse((String) eventMap.get("message_type"));

                                // Handle answer type events
                                if ("answer".equals(type)) {
                                    String content = null;
                                    // Try to get from content field
                                    if (eventMap.get("content") instanceof String) {
                                        content = (String) eventMap.get("content");
                                    } else if (eventMap.get("message") instanceof String) {
                                        content = (String) eventMap.get("message");
                                    }
                                    // If content is empty, try to get from data field
                                    else if (eventMap.get("data") != null) {
                                        Object dataField = eventMap.get("data");
                                        if (dataField instanceof String) {
                                            content = (String) dataField;
                                        } else {
                                            // For complex data structures (such as arrays, objects), convert to JSON string
                                            try {
                                                content = JsonUtils.writeValueAsString(dataField);
                                            } catch (Exception e) {
                                                log.debug("Failed to serialize data field to JSON: {}", e.getMessage());
                                                content = dataField.toString();
                                            }
                                        }
                                    }

                                    if (content != null) {
                                        answer.set(content); // Update final answer
//                                        result.append(content);
                                    }
                                    eventMap.put("_is_stored", false);
                                    if (this.isSendAnswer()) {
                                        request.sendMessage(eventMap);
                                    }
                                }
                                // Handle tool call and observation type events
                                else if ("tool_call".equals(type) || "observation".equals(type)) {
                                    // Check caller_category and callee_category, consistent with Python logic
                                    @SuppressWarnings("unchecked")
                                    var contentMap = (Map<String, Object>) eventMap.get("content");
                                    if (contentMap != null) {
                                        var callerCategory = contentMap.getOrDefault("caller_category", "").toString();
                                        var calleeCategory = contentMap.getOrDefault("callee_category", "").toString();

                                        // Filter out user-related calls to avoid infinite loops
                                        if (!"user".equals(callerCategory) && !"user".equals(calleeCategory)) {
                                            // Handle call_stack merge logic (consistent with Python logic)
                                            if (!isShareCallStack) {
                                                @SuppressWarnings("unchecked")
                                                var callStack = (ArrayList<Object>) contentMap.get("call_stack");
                                                if (callStack != null && callStack.size() > 2) {
                                                    // Merge call_stack: request.call_stack + data.content.call_stack[2:]
                                                    // This is done to maintain the integrity of the call chain
                                                    var newCallStack = new ArrayList<Object>(request.getCallStack());
                                                    newCallStack.addAll(callStack.subList(2, callStack.size()));
                                                    contentMap.put("call_stack", newCallStack);
                                                }
                                            }
                                            // Forward event to request handler

                                            request.sendMessage(eventMap);
                                        }
                                    }
                                }
                                // Handle other type events
                                else {
                                    request.sendMessage(eventMap);
                                }
                            }
                        } catch (Exception e) {
                            log.error("Error processing SSE event: {}", e.getMessage(), e);
                            errorRef.set(e);
                            latch.countDown(); // Also notify main thread when error occurs
                        }
                    },
                    // onError: Handle errors in stream
                    error -> {
                        log.error("SSE stream error: {}", error.getMessage(), error);
                        errorRef.set(new RuntimeException("SSE stream error", error));
                        latch.countDown(); // Also notify main thread on error
                    },
                    // onComplete: Handle when stream completes normally
                    () -> {
                        log.info("SSE stream completed");
                        latch.countDown(); // Notify main thread that stream is completed
                    }
            );

            // === Step 5: Wait for stream processing completion and handle results ===
            // Wait for stream processing completion, set 300 seconds timeout to prevent infinite waiting
            boolean completed = latch.await(300, java.util.concurrent.TimeUnit.SECONDS);

            // Check if timeout occurred
            if (!completed) {
                throw new RuntimeException("SSE processing timeout");
            }

            // Check if any error occurred
            if (errorRef.get() != null) {
                throw errorRef.get();
            }

            // Build success response
            return OxyResponse.builder()
                    .oxyRequest(request)
                    .state(OxyState.COMPLETED)
                    .output(answer.get())
                    .build();

        } catch (Exception e) {
            // === Exception handling ===
            // Throw exception to upper layer retry logic for handling
            throw e;
        }
    }

}