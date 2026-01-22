/*
 * Copyright (c) 2024 OxyGent Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

package com.jd.oxygent.core.oxygent.samples.server.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.uuid.UuidCreator;
import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.EvaluationManager;
import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.liveprompt.DynamicAgentManager;
import com.jd.oxygent.core.oxygent.liveprompt.PromptManager;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.MasFactoryRegistry;
import com.jd.oxygent.core.oxygent.samples.server.utils.FileValidationUtil;
import com.jd.oxygent.core.oxygent.samples.server.utils.RecursivePackageInstantiator;
import com.jd.oxygent.core.oxygent.samples.server.vo.AgentNodeConverter;
import com.jd.oxygent.core.oxygent.samples.server.vo.ItemRequest;
import com.jd.oxygent.core.oxygent.samples.server.vo.OrganizationWrapper;
import com.jd.oxygent.core.oxygent.samples.server.vo.ScriptRequest;
import com.jd.oxygent.core.oxygent.samples.server.vo.WebResponse;
import com.jd.oxygent.core.oxygent.schemas.SSEMessage;
import com.jd.oxygent.core.oxygent.schemas.evaluation.ConversationRating;
import com.jd.oxygent.core.oxygent.schemas.evaluation.RatingRequest;
import com.jd.oxygent.core.oxygent.schemas.evaluation.RatingResponse;
import com.jd.oxygent.core.oxygent.schemas.evaluation.RatingStats;
import com.jd.oxygent.core.oxygent.schemas.evaluation.RatingType;
import com.jd.oxygent.core.oxygent.schemas.memory.Memory;
import com.jd.oxygent.core.oxygent.schemas.memory.Message;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.utils.ClassModelDumpUtils;
import com.jd.oxygent.core.oxygent.utils.CommonUtils;
import com.jd.oxygent.core.oxygent.utils.DataUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileUpload;
import org.apache.tomcat.util.http.fileupload.FileUploadBase;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletRequestContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.jd.oxygent.core.oxygent.samples.server.ServerConstants.DEFAULT_FILE_STORE_TEMP_DIR;
import static com.jd.oxygent.core.oxygent.samples.server.ServerConstants.DEFAULT_MEMORY_SIZE_THRESHOLD;
import static com.jd.oxygent.core.oxygent.samples.server.ServerConstants.DEFAULT_UPLOAD_ALL_FILE_MAX_SIZE_THRESHOLD;
import static com.jd.oxygent.core.oxygent.samples.server.ServerConstants.DEFAULT_UPLOAD_FILE_MAX_SIZE_THRESHOLD;
import static com.jd.oxygent.core.oxygent.samples.server.ServerConstants.RESTRICTED_HEADERS;

/**
 * Main routing servlet for OxyGent server that handles various HTTP endpoints
 * including chat, file upload, script management, and system information.
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@WebServlet(name = "RouteServlet", loadOnStartup = 1)
public class RouteServlet extends HttpServlet {

    private final Mas mas = MasFactoryRegistry.getFactory().createMas();
    private final EvaluationManager evaluationManager = EvaluationManager.getInstance();
    private final PromptManager promptManager = PromptManager.getInstance();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = requestURI.substring(contextPath.length());

        log.debug("Process request: {} {}", request.getMethod(), path);

        try {
            switch (path) {
                case "/":
                    root(request, response);
                    break;
                case "/check_alive":
                    checkAlive(request, response);
                    break;
                case "/get_organization":
                    getOrganization(request, response);
                    break;
                case "/get_first_query":
                    getFirstQuery(request, response);
                    break;
                case "/get_welcome_message":
                    getWelcomeMessage(request, response);
                    break;
                case "/list_script":
                    listScript(request, response);
                    break;
                case "/save_script":
                    saveScript(request, response);
                    break;
                case "/load_script":
                    loadScript(request, response);
                    break;
                case "/api/group_uid":
                    groupId(request, response);
                    break;
                case "/api/trace_uid":
                    traceId(request, response);
                    break;
                case "/chat":
                    chat(request, response);
                    break;
                case "/sse/chat":
                    sseChat(request, response);
                    break;
                case "/async/chat":
                    asyncChat(request, response);
                    break;
                case "/node":
                    getNodeInfo(request, response);
                    break;
                case "/view":
                    getTaskInfo(request, response);
                    break;
                case "/call":
                    call(request, response);
                    break;
                case "/upload":
                    uploadFile(request, response);
                    break;
                case "/feedback":
                    feedback(request, response);
                    break;
                case "/get_agents":
                    getAgents(request, response);
                    break;
                case "/rating":
                    rating(request, response);
                    break;
                case "/history_with_ratings":
                    historyWithRatings(request, response);
                    break;
                case "/analytics/ratings":
                    analyticsRatings(request, response);
                    break;
                default:
                    // Handle prompt API routes
                    if (path.startsWith("/api/prompts/")) {
                        promptApiRoutes(path, request, response);
                    }
                    // Handle other rating-related routes
                    else if (path.startsWith("/rating/")) {
                        ratingRoutes(path, request, response);
                    } else {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Path not found: " + path);
                    }
                    break;
            }
        } catch (Exception e) {
            log.error("Process requestfailed: " + path, e);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * Redirect client to packaged web frontend
     */
    private void root(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect("./index.html");
    }

    /**
     * Health check endpoint
     */
    private void checkAlive(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> result = Map.of("alive", 1);
        sendJsonResponse(response, HttpServletResponse.SC_OK, WebResponse.success(result).toMap());
    }

    /**
     * Get agent organization structure
     */
    private void getOrganization(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            OrganizationWrapper organizedWithPath = AgentNodeConverter.convertToOrganization(mas.getAgentOrganization());
            sendJsonResponse(response, HttpServletResponse.SC_OK, WebResponse.success(organizedWithPath).toMap());
        } catch (Exception e) {
            log.error("Getorganization structurefailed", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Getorganization structurefailed").toMap());
        }
    }

    /**
     * Get first query
     */
    private void getFirstQuery(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String firstQuery = mas.getFirstQuery() != null && !mas.getFirstQuery().isEmpty()
                    ? mas.getFirstQuery() : Config.getServer().getFirstQuery();
            Map<String, Object> data = Map.of("first_query", firstQuery);
            sendJsonResponse(response, HttpServletResponse.SC_OK, WebResponse.success(data).toMap());
        } catch (Exception e) {
            log.error("Getfirst queryfailed", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Getfirst queryfailed").toMap());
        }
    }

    /**
     * Get welcome message
     */
    private void getWelcomeMessage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String welcomeMessage = Config.getAgent().getWelcomeMessage();
            Map<String, Object> data = Map.of("welcome_message", welcomeMessage);
            sendJsonResponse(response, HttpServletResponse.SC_OK, WebResponse.success(data).toMap());
        } catch (Exception e) {
            log.error("Getwelcome messagefailed", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Getwelcome messagefailed").toMap());
        }
    }

    /**
     * List all saved scripts
     */
    private void listScript(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String scriptSaveDir = Paths.get(Config.getXfile().getSaveDir(), "script").toString();
            Files.createDirectories(Paths.get(scriptSaveDir));

            File dir = new File(scriptSaveDir);
            String[] files = dir.list((d, name) -> name.endsWith(".json"));

            List<String> scripts = files != null ? Arrays.stream(files)
                    .map(file -> file.substring(0, file.lastIndexOf(".")))
                    .collect(Collectors.toList()) : List.of();

            Map<String, Object> data = Map.of("scripts", scripts);
            sendJsonResponse(response, HttpServletResponse.SC_OK, WebResponse.success(data).toMap());

        } catch (IOException e) {
            log.error("Listscriptfailed", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Listscriptfailed").toMap());
        }
    }

    /**
     * Get session ID
     */
    private void groupId(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Map<String, Object> result = Map.of("uuid", UuidCreator.getShortSuffixComb().toString());
            sendJsonResponse(response, HttpServletResponse.SC_OK, WebResponse.success(result).toMap());
        } catch (Exception e) {
            log.error("group_uid generate failed", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Request group ID internal server error").toMap());
        }
    }

    /**
     * Get single request trace ID
     */
    private void traceId(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Map<String, Object> result = Map.of("uuid", UuidCreator.getShortSuffixComb().toString());
            sendJsonResponse(response, HttpServletResponse.SC_OK, WebResponse.success(result).toMap());
        } catch (Exception e) {
            log.error("trace_uid generate failed", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Request trace ID internal server error").toMap());
        }
    }

    /**
     * Helper method: Send JSON response
     */
    private void sendJsonResponse(HttpServletResponse response, int status, Map<String, Object> data) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.write(objectMapper.writeValueAsString(data));
        }
    }

    /**
     * Helper method: Send error response
     */
    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.sendError(status, message);
    }

    /**
     * Read JSON from request body
     */
    private Map<String, Object> readRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return objectMapper.readValue(sb.toString(), Map.class);
    }

    /**
     * Read JSON from request parameters
     */
    private Map<String, Object> readRequestParam(HttpServletRequest request, String paramName) throws IOException {
        String paramValue = request.getParameter(paramName);
        if (paramValue == null || paramValue.trim().isEmpty()) {
            return Map.of();
        }
        return objectMapper.readValue(paramValue, Map.class);
    }

    /**
     * Read string from request body
     */
    private String readRequestBodyAsString(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /**
     * Extract request headers
     */
    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if(!RESTRICTED_HEADERS.contains(headerName.toLowerCase())){
                headers.put(headerName, request.getHeader(headerName));
            }
        }
        return headers;
    }

    /**
     * Save script
     */
    private void saveScript(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            ScriptRequest script = objectMapper.readValue(readRequestBodyAsString(request), ScriptRequest.class);

            String scriptSaveDir = Paths.get(Config.getXfile().getSaveDir(), "script").toString();
            Files.createDirectories(Paths.get(scriptSaveDir));

            Path filePath = Paths.get(scriptSaveDir, script.getName() + ".json");
            String jsonContent = objectMapper.writeValueAsString(script.getContents());
            Files.write(filePath, jsonContent.getBytes());

            Map<String, Object> data = Map.of("script_id", script.getName() + ".json");
            sendJsonResponse(response, HttpServletResponse.SC_OK, WebResponse.success(data).toMap());

        } catch (IOException e) {
            log.error("Save script failed", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Save script failed").toMap());
        }
    }

    /**
     * Load a previously saved script.
     *
     *     Args:
     *         script_id: Timestamp‑based identifier returned by :func:`save_script`.
     *
     *     Returns:
     *         dict: ``WebResponse`` containing the original ``contents`` array or an
     *         error message when the file is missing.
     */
    private void loadScript(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String itemId = request.getParameter("item_id");
            if (itemId == null || itemId.trim().isEmpty()) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing item_id parameter");
                return;
            }

            String scriptSaveDir = Paths.get(Config.getXfile().getSaveDir(), "script").toString();
            Path jsonPath = Paths.get(scriptSaveDir, itemId + ".json");

            if (!Files.exists(jsonPath)) {
                sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        WebResponse.error(500, "File does not exist").toMap());
                return;
            }

            String content = Files.readString(jsonPath);
            List<Object> contents = objectMapper.readValue(content, List.class);

            Map<String, Object> data = Map.of("contents", contents);
            sendJsonResponse(response, HttpServletResponse.SC_OK, WebResponse.success(data).toMap());

        } catch (IOException e) {
            log.error("Load script failed", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Load script failed").toMap());
        }
    }

    /**
     * Chat interface - synchronous mode
     */
    private void chat(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Map<String, Object> payload = readRequestBody(request);
            Map<String, String> headers = extractHeaders(request);

            payload = requestToPayload(payload, headers);

            // Apply interceptor
            if (mas.getFuncInterceptor() != null) {
                Object interceptedResponse = mas.getFuncInterceptor().apply(payload);
                if (interceptedResponse != null) {
                    sendJsonResponse(response, HttpServletResponse.SC_OK, (Map<String, Object>) interceptedResponse);
                    return;
                }
            }

            // Execute chat
            OxyResponse oxyResponse = mas.chatWithAgent(payload, null);
            sendJsonResponse(response, HttpServletResponse.SC_OK, WebResponse.success(oxyResponse.getOutput()).toMap());

        } catch (Exception e) {
            log.error("Chat failed", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Chat failed: " + e.getMessage()).toMap());
        }
    }

    /**
     * SSE chat interface - server-sent events mode
     */
    private void sseChat(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        try {
            Map<String, Object> payload;
            if ("GET".equals(request.getMethod())) {
                String payloadJson = request.getParameter("payload");
                if (payloadJson == null || payloadJson.trim().isEmpty()) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing payload parameter");
                    return;
                }
                payload = objectMapper.readValue(payloadJson, Map.class);
            } else {
                payload = readRequestBody(request);
            }

            Map<String, String> headers = extractHeaders(request);
            payload = requestToPayload(payload, headers);

            // Apply interceptor
            if (mas.getFuncInterceptor() != null) {
                Object interceptedResponse = mas.getFuncInterceptor().apply(payload);
                if (interceptedResponse != null) {
                    sendSseEvent(response, "message", interceptedResponse);
                    return;
                }
            }

            String currentTraceId = payload.getOrDefault("current_trace_id", "").toString();
            log.info("SSE connection established. trace_id: {}", currentTraceId);

            String redisKey = mas.getMessagePrefix() + ":" + mas.getName() + ":" + currentTraceId;

            // Execute chat asynchronously
            Map<String, Object> finalPayload = payload;
            CompletableFuture<OxyResponse> task = CompletableFuture.supplyAsync(() -> {
                try {
                    return mas.chatWithAgent(finalPayload, redisKey);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            // Handle task completion
            task.whenComplete((result, throwable) -> {
                this.mas.getActiveTasks().remove(currentTraceId);
                if (throwable != null) {
                    log.error("Chat task failed", throwable);
                    try {
                        sendSseEvent(response, "error", Map.of("error", throwable.getMessage()));
                    } catch (IOException e) {
                        log.error("Send SSE error event failed", e);
                    }
                }
            });

            this.mas.getActiveTasks().put(currentTraceId, task);

            // Start event stream
            processRedisMessage(redisKey, currentTraceId, task, response);

        } catch (Exception e) {
            log.error("SSE chat failed", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SSE chat failed: " + e.getMessage());
        }
    }

    /**
     * Feedback interface
     * @param request
     * @param response
     * @throws IOException
     */
    private void feedback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> payload = readRequestBody(request);
        String channelId = (String) payload.getOrDefault("channel_id", "");
        if (!mas.feedbackDict.containsKey(channelId)) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "illegal channel_id: " + channelId);
        }
        LinkedBlockingQueue<String> feedbackQueue = mas.feedbackDict.get(channelId);
        String data = (String) payload.getOrDefault("data", "");
        if (feedbackQueue == null) {
            feedbackQueue = new LinkedBlockingQueue<>();
            mas.feedbackDict.put(channelId, feedbackQueue);
        }
        try {
            feedbackQueue.put(data);
            feedbackQueue.put(""); // stream end symbol
        } catch (InterruptedException e) {
            log.error("Feedback queue put failed", e);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
        sendJsonResponse(response, HttpServletResponse.SC_OK, Map.of("channel_id", channelId));
    }

    /**
     * Asynchronous chat interface
     */
    private void asyncChat(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Map<String, Object> payload = readRequestBody(request);
            Map<String, String> headers = extractHeaders(request);

            payload = requestToPayload(payload, headers);

            // Apply interceptor
            if (mas.getFuncInterceptor() != null) {
                Object interceptedResponse = mas.getFuncInterceptor().apply(payload);
                if (interceptedResponse != null) {
                    sendJsonResponse(response, HttpServletResponse.SC_OK, (Map<String, Object>) interceptedResponse);
                    return;
                }
            }

            String currentTraceId = payload.getOrDefault("current_trace_id", "").toString();
            log.info("Async task created. trace_id: {}", currentTraceId);

            String redisKey = mas.getMessagePrefix() + ":" + mas.getName() + ":" + currentTraceId;

            // Execute chat asynchronously
            Map<String, Object> finalPayload = payload;
            CompletableFuture<OxyResponse> task = CompletableFuture.supplyAsync(() -> {
                try {
                    return mas.chatWithAgent(finalPayload, redisKey);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            // Handle task completion
            task.whenComplete((result, throwable) -> {
                this.mas.getActiveTasks().remove(currentTraceId);
                if (throwable != null) {
                    log.error("Async Chat task failed", throwable);
                }
            });

            this.mas.getActiveTasks().put(currentTraceId, task);
            sendJsonResponse(response, HttpServletResponse.SC_OK, WebResponse.success(null).toMap());

        } catch (Exception e) {
            log.error("Async chat failed", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Async chat failed: " + e.getMessage()).toMap());
        }
    }

    /**
     * Get node information
     */
    private void getNodeInfo(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String itemId = request.getParameter("item_id");
            if (itemId == null || itemId.trim().isEmpty()) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing item_id parameter");
                return;
            }

            // Search nodes
            Map<String, Object> query = Map.of("query", Map.of("term", Map.of("_id", itemId)));

            Map<String, Object> esResponse = mas.getEsClient().search(Config.getAppName() + "_node", query);

            String traceId;
            Map<String, Object> nodeData = null;

            List<Map<String, Object>> hits = (List<Map<String, Object>>) ((Map<String, Object>) esResponse.get("hits")).get("hits");

            if (!hits.isEmpty()) {
                nodeData = (Map<String, Object>) hits.get(0).get("_source");
                traceId = nodeData.getOrDefault("trace_id", "").toString();
            } else {
                // Use item_id as trace_id
                traceId = itemId;
            }

            // Get all nodes
            Map<String, Object> traceQuery = Map.of(
                    "query", Map.of("term", Map.of("trace_id", traceId)),
                    "size", 10000,
                    "sort", List.of(Map.of("create_time", Map.of("order", "asc")))
            );

            esResponse = mas.getEsClient().search(Config.getAppName() + "_node", traceQuery);

            List<String> nodeIds = new ArrayList<>();
            hits = (List<Map<String, Object>>) ((Map<String, Object>) esResponse.get("hits")).get("hits");

            for (Map<String, Object> data : hits) {
                Map<String, Object> source = (Map<String, Object>) data.get("_source");
                nodeIds.add(source.getOrDefault("node_id", "").toString());
            }

            if (nodeIds.isEmpty()) {
                sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        WebResponse.error(400, "Invalid node ID").toMap());
                return;
            }

            if (traceId.equals(itemId)) {
                // Re-get node_id data
                itemId = nodeIds.get(0);
                query = Map.of("query", Map.of("term", Map.of("_id", itemId)));
                esResponse = mas.getEsClient().search(Config.getAppName() + "_node", query);
                hits = (List<Map<String, Object>>) ((Map<String, Object>) esResponse.get("hits")).get("hits");
                nodeData = (Map<String, Object>) hits.get(0).get("_source");
            }

            // Find previous and next nodes
            for (int i = 0; i < nodeIds.size(); i++) {
                if (itemId.equals(nodeIds.get(i))) {
                    nodeData.put("pre_id", i >= 1 ? nodeIds.get(i - 1) : "");
                    nodeData.put("next_id", i <= nodeIds.size() - 2 ? nodeIds.get(i + 1) : "");

                    // Process input data
                    if (nodeData.containsKey("input")) {
                        String inputStr = nodeData.getOrDefault("input", "").toString();
                        Map<String, Object> input = objectMapper.readValue(inputStr.replace("\r", "\\r").replace("\n", "\\n"), Map.class);
                        nodeData.put("input", input);
                    }

                    // Remove prompt
                    Map<String, Object> input = (Map<String, Object>) nodeData.get("input");
                    Map<String, Object> classAttr = input != null ? (Map<String, Object>) input.get("class_attr") : Map.of();
                    if (classAttr.containsKey("prompt")) {
                        classAttr.remove("prompt");
                    }

                    // Process environment variablesx
                    Map<String, String> envValueToKey = new HashMap<>();
                    System.getenv().forEach((k, v) -> envValueToKey.put(v, k));

                    // Generate data range
                    Map<String, Map<String, Object>> dataRangeMap = new HashMap<>();
                    nodeData.put("data_range_map", dataRangeMap);

                    List<Map<String, Object>> trees = Arrays.asList(classAttr,
                            (Map<String, Object>) classAttr.getOrDefault("llm_params", Map.of()),
                            input != null ? (Map<String, Object>) input.get("arguments") : Map.of());

                    for (Map<String, Object> tree : trees) {
                        if (tree == null) {
                            continue;
                        }
                        for (Map.Entry<String, Object> entry : tree.entrySet()) {
                            String k = entry.getKey();
                            Object v = entry.getValue();

                            if (v instanceof String && envValueToKey.containsKey(v)) {
                                tree.put(k, "${" + envValueToKey.get(v) + "}");
                            }

                            if ((v instanceof Integer || v instanceof Double || v instanceof Float)
                                    && !(v instanceof Boolean)) {
                                double numValue = Double.parseDouble(v.toString());
                                double maxValue = numValue <= 1 ? 1 : numValue * 10;
                                dataRangeMap.put(k, Map.of(
                                        "min", 0,
                                        "max", maxValue
                                ));
                            }
                        }
                    }

                    // Convert data
                    DataUtils.changeNodeValue(nodeData);

                    sendJsonResponse(response, HttpServletResponse.SC_OK, WebResponse.success(nodeData).toMap());
                    return;
                }
            }

            sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    WebResponse.error(400, "Node not found").toMap());

        } catch (Exception e) {
            log.error("Get node information failed", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Encountered problem").toMap());
        }
    }

    /**
     * Get task information
     */
    private void getTaskInfo(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String itemId = request.getParameter("item_id");
            if (itemId == null || itemId.trim().isEmpty()) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing item_id parameter");
                return;
            }

            // Check if it is node_id
            Map<String, Object> query = Map.of("query", Map.of("term", Map.of("_id", itemId)));
            Map<String, Object> esResponse = mas.getEsClient().search(Config.getAppName() + "_node", query);

            String traceId;
            List<Map<String, Object>> hits = (List<Map<String, Object>>) ((Map<String, Object>) esResponse.get("hits")).get("hits");

            if (!hits.isEmpty()) {
                Map<String, Object> nodeData = (Map<String, Object>) hits.get(0).get("_source");
                traceId = nodeData.getOrDefault("trace_id", "").toString();
            } else {
                traceId = itemId;
            }

            // Get all nodes
            Map<String, Object> traceQuery = Map.of(
                    "query", Map.of("term", Map.of("trace_id", traceId)),
                    "size", 10000,
                    "sort", List.of(Map.of("create_time", Map.of("order", "asc")))
            );

            esResponse = mas.getEsClient().search(Config.getAppName() + "_node", traceQuery);

            List<Map<String, Object>> nodes = new ArrayList<>();
            hits = (List<Map<String, Object>>) ((Map<String, Object>) esResponse.get("hits")).get("hits");

            for (Map<String, Object> data : hits) {
                Map<String, Object> source = (Map<String, Object>) data.get("_source");

                // Ensure specific fields exist
                source.putIfAbsent("call_stack", source.get("call_stack"));
                source.putIfAbsent("node_id_stack", source.get("node_id_stack"));

                if (source.get("pre_node_ids") == null || "".equals(source.get("pre_node_ids"))) {
                    source.put("pre_node_ids", new ArrayList<String>());
                }

                nodes.add(source);
            }

            // Add successor and child nodes
            DataUtils.addPostAndChildNodeIds(nodes);

            // Add index
            for (int index = 0; index < nodes.size(); index++) {
                nodes.get(index).put("index", index);
                DataUtils.changeNodeValue(nodes.get(index));
            }

            Map<String, Object> taskData = Map.of(
                    "nodes", nodes,
                    "trace_id", traceId
            );

            sendJsonResponse(response, HttpServletResponse.SC_OK, WebResponse.success(taskData).toMap());

        } catch (Exception e) {
            log.error("Get task information failed", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Encountered problem").toMap());
        }
    }

    private static final Pattern PATTERN = Pattern.compile("^\\$\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}$");

    /**
     * Call OxyGent agent
     */
    private void call(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            ItemRequest item = objectMapper.readValue(readRequestBodyAsString(request), ItemRequest.class);

            // Process environment variables pattern matching
            List<Map<String, Object>> trees = Arrays.asList(item.getClassAttr(),
                    (Map<String, Object>) item.getClassAttr().getOrDefault("llm_params", Map.of()),
                    item.getArguments()
            );

            for (Map<String, Object> tree : trees) {
                if (tree == null) {
                    continue;
                }
                for (Map.Entry<String, Object> entry : tree.entrySet()) {
                    Object v = entry.getValue();
                    if (!(v instanceof String)) {
                        continue;
                    }

                    Matcher matcher = PATTERN.matcher(((String) v).trim());
                    if (matcher.matches()) {
                        String envKey = matcher.group(1);
                        String envValue = System.getenv(envKey);
                        tree.put(entry.getKey(), envValue != null ? envValue : v);
                    }
                }
            }

            // Set name
            item.getClassAttr().put("name", (item.getClassAttr().getOrDefault("class_name", "").toString()).toLowerCase());

            // Type conversion
            Map<String, Class<?>> llmParamsTypeDict = Map.of(
                    "temperature", Double.class,
                    "max_tokens", Integer.class,
                    "top_p", Double.class
            );

            Map<String, Object> llmParams = (Map<String, Object>) item.getClassAttr().get("llm_params");
            if (llmParams != null) {
                for (Map.Entry<String, Object> entry : llmParams.entrySet()) {
                    if (llmParamsTypeDict.containsKey(entry.getKey())) {
                        Class<?> targetType = llmParamsTypeDict.get(entry.getKey());
                        Object value = entry.getValue();

                        if (targetType == Double.class) {
                            llmParams.put(entry.getKey(), Double.parseDouble(value.toString()));
                        } else if (targetType == Integer.class) {
                            llmParams.put(entry.getKey(), Integer.parseInt(value.toString()));
                        }
                    }
                }
            }

            // Create Oxy instance and execute
            String className = item.getClassAttr().getOrDefault("class_name", "").toString();

            BaseOxy oxy = RecursivePackageInstantiator.createInstance(className, item.getClassAttr());

            Map<String, Object> args = new HashMap<>();
            for (Map.Entry<String, Object> entry : item.getArguments().entrySet()) {
                args.put(ClassModelDumpUtils.toSnakeCase(entry.getKey()), entry.getValue());
            }
            OxyRequest oxyRequest = new OxyRequest();
            oxyRequest.setArguments(args);

            List<Map<String, Object>> msgs = (List<Map<String, Object>>) (item.getArguments().get("messages"));

            for (Map<String, Object> msg : msgs) {
                String roleValue = msg.get("role").toString();
                msg.put("role", roleValue.toLowerCase());
            }
            List<Message> messages = Message.dictListToMessages(msgs);

            Memory memory = new Memory();
            memory.setMessages(messages);

            oxyRequest.getArguments().put("messages", memory);

            OxyResponse oxyResponse = oxy.execute(oxyRequest);

            Map<String, Object> data = Map.of("output", oxyResponse.getOutput());
            sendJsonResponse(response, HttpServletResponse.SC_OK, WebResponse.success(data).toMap());

        } catch (Exception e) {
            log.error("Call failed", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Encountered problem: " + e.getMessage()).toMap());
        }
    }

    /**
     * Convert request to payload
     */
    private Map<String, Object> requestToPayload(Map<String, Object> payload, Map<String, String> headers) throws Exception {

        // Apply filter
        if (mas.getFuncFilter()!= null) {
            payload = mas.getFuncFilter().apply(payload);
        }

        // Set default query
        payload.putIfAbsent("query", "");

        // Process attachments
        if (payload.containsKey("attachments")) {
            List<String> attachments = (List<String>) payload.get("attachments");
            List<String> attachmentsWithPath = new ArrayList<>();
            List<String> remoteUrls = new ArrayList<>();

            for (String attachment : attachments) {
                boolean isRemote = attachment.startsWith("http://") || attachment.startsWith("https://");
                String filePath = isRemote ? attachment : Config.getXfile().getSaveDir() + "/uploads/" + attachment;
                attachmentsWithPath.add(filePath);
                if (isRemote) {
                    remoteUrls.add(filePath);
                }
            }

            // Deduplicate attachments
            payload.put("attachments", attachmentsWithPath);
            if (!remoteUrls.isEmpty()) {
                List<String> existingUrls = (List<String>) payload.getOrDefault("web_file_url_list", List.of());
                Set<String> allUrls = new LinkedHashSet<>(existingUrls);
                allUrls.addAll(remoteUrls);
                payload.put("web_file_url_list", new ArrayList<>(allUrls));
            }

            // Combine query parts (a2a style)
            String query = payload.getOrDefault("query", "").toString();
            payload.put("query", CommonUtils.composeQueryParts(query, attachmentsWithPath));
        }

        // Set current trace_id
            payload.putIfAbsent("current_trace_id", CommonUtils.generateShortUUID());
            // Get request headers
            payload.putIfAbsent("shared_data", Map.of());
            // Add request headers
            ((Map<String, Object>) payload.get("shared_data")).put("_headers", headers);

        return payload;
    }

    /**
     * Event stream handler
     */
    private void processRedisMessage(String redisKey, String currentTraceId, CompletableFuture<OxyResponse> task,
                                     HttpServletResponse response) throws Exception {
        try {
            while (true) {
                // Read message from Redis, Polling to prevent latency
                Object rpop = mas.getRedisClient().brpop(redisKey);
                if (rpop == null) {
                    Thread.sleep(100);
                    continue;
                }

                // Unpack message
                SSEMessage<Map<String, Object>> sseMessage = mas.unpackMessage(Base64.getDecoder().decode((String) rpop));
                if (sseMessage != null && sseMessage.getData() != null) {
                    Map<String, Object> msgMap = sseMessage.getData();
                    // Check if it is a close event
                    if (msgMap.containsKey("event")) {
                        sendSseEvent(response, (String) msgMap.get("event"), msgMap);
                        log.info("SSE connection terminated. trace_id: {}", currentTraceId);
                        return;
                    }

                    // Process tool_call messages
                    if ("tool_call".equals(msgMap.get("type"))) {
                        Map<String, Object> content = (Map<String, Object>) msgMap.get("content");
                        if (content != null) {
                            Map<String, Object> arguments = (Map<String, Object>) content.get("arguments");
                            if (arguments != null && arguments.get("query") instanceof List) {
                                List<Map<String, Object>> queryList = (List<Map<String, Object>>) arguments.get("query");
                                for (Map<String, Object> msg : queryList) {
                                    if ("text".equals(msg.get("type"))) {
                                        arguments.put("query", msg.getOrDefault("text", ""));
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    // Process observation messages
                    if ("observation".equals(msgMap.get("type"))) {
                        Map<String, Object> content = (Map<String, Object>) msgMap.get("content");
                        if (content != null && content.containsKey("output")) {
                            content.put("output", CommonUtils.toJson(content.get("output")));
                        }
                    }
                    // Send message
                    sendSseEvent(response, "message", msgMap);
                }
            }
        } catch (InterruptedException e) {
            log.info("SSE connection terminated. trace_id: {}", currentTraceId);
            if (this.mas.getActiveTasks().containsKey(currentTraceId)) {
                ((CompletableFuture<?>) this.mas.getActiveTasks().get(currentTraceId)).cancel(true);
            }
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Event stream processing failed", e);
            try {
                sendSseEvent(response, "error", Map.of("error", e.getMessage()));
            } catch (IOException ioException) {
                log.error("Send SSE error event failed", ioException);
            }
        }
    }

    /**
     * Send SSE event
     */
    private void sendSseEvent(HttpServletResponse response, String eventName, Object data) throws IOException {
        response.getWriter().write("event: " + eventName + "\n");
        response.getWriter().write("data: " + objectMapper.writeValueAsString(data) + "\n\n");
        response.getWriter().flush();
    }

    /**
     * File upload endpoint
     * Accepts user uploaded files and saves them to the server's uploads directory.
     * File names are prefixed with timestamps to avoid conflicts.
     */
    private void uploadFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // Configure upload parameters
            DiskFileItemFactory factory = new DiskFileItemFactory();
            factory.setSizeThreshold(DEFAULT_MEMORY_SIZE_THRESHOLD); // 1MB memory threshold
            factory.setRepository(new File(System.getProperty(DEFAULT_FILE_STORE_TEMP_DIR))); // temporary directory

            // Use FileUploadBase instead of ServletFileUpload
            FileUploadBase upload = new FileUpload();
            upload.setFileItemFactory(factory);
            upload.setFileSizeMax(DEFAULT_UPLOAD_FILE_MAX_SIZE_THRESHOLD); // 10MB file size limit
            upload.setSizeMax(DEFAULT_UPLOAD_ALL_FILE_MAX_SIZE_THRESHOLD); // 50MB total request size limit
            // Parse request - use ServletRequestContext to wrap HttpServletRequest
            List<FileItem> items = upload.parseRequest(new ServletRequestContext(request));

            // Find file item, currently business only supports single file upload
            FileItem fileItem = null;
            for (FileItem item : items) {
                if (!item.isFormField() && "file".equals(item.getFieldName())) {
                    fileItem = item;
                    break;
                }
            }

            FileValidationUtil.validateFile(fileItem);
            // Generate unique filename
            String uploadDir = Paths.get(Config.getXfile().getSaveDir(), "uploads").toString();
            Files.createDirectories(Paths.get(uploadDir));

            String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
            String fileName = timestamp + "_" + fileItem.getName();
            Path filePath = Paths.get(uploadDir, fileName);

            // Save file
            Files.copy(fileItem.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Return file path
            Map<String, Object> data = Map.of("file_name", fileName);
            sendJsonResponse(response, HttpServletResponse.SC_OK, WebResponse.success(data).toMap());
        } catch (Exception e) {
            log.error("File upload failed", e);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "File upload failed: " + e.getMessage());
        }
    }

    /**
     * Handle prompt API routes
     */
    private void promptApiRoutes(String path, HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Parse path segments
        String[] segments = path.startsWith("/") ? path.substring(1).split("/") : path.split("/");
        String method = request.getMethod();

        try {
            // Handle different prompt API endpoints
            if (path.equals("/api/prompts/")) {
                if (method.equals("GET")) {
                    listPrompts(request, response);
                } else if (method.equals("POST")) {
                    createPrompt(request, response);
                } else {
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
                }
            } else if (path.equals("/api/prompts/search/")) {
                // /api/prompts/search/
                if (method.equals("GET")) {
                    searchPrompts(request, response);
                } else {
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
                }
            } else if (segments.length >= 5 && segments[3].equals("hot-reload") && segments[4].equals("all")) {
                // /api/prompts/hot-reload/all
                if (method.equals("POST")) {
                    hotReloadAllAgentPrompts(response);
                } else {
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
                }
            } else if (segments.length >= 5 && segments[3].equals("hot-reload") && segments[4].equals("agent")) {
                // /api/prompts/hot-reload/agent/{agent_name}
                if (method.equals("POST")) {
                    String agentName = segments[5];
                    hotReloadAgentPrompt(agentName, response);
                } else {
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
                }
            } else if (segments.length >= 5 && segments[3].equals("hot-reload")) {
                // /api/prompts/hot-reload/{prompt_key}
                if (method.equals("POST")) {
                    String promptKey = segments[4];
                    hotReloadPrompt(promptKey, response);
                } else {
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
                }
            } else if (segments.length >= 4 && segments[3].equals("history")) {
                // /api/prompts/{prompt_key}/history
                if (method.equals("GET")) {
                    String promptKey = segments[2];
                    getPromptHistory(promptKey, response);
                } else {
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
                }
            } else if (segments.length >= 4 && segments[3].equals("version")) {
                // /api/prompts/{prompt_key}/version/{version}
                if (method.equals("GET")) {
                    String promptKey = segments[2];
                    int version = Integer.parseInt(segments[4]);
                    getPromptVersion(promptKey, version, response);
                } else {
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
                }
            } else if (segments.length >= 4 && segments[3].equals("revert")) {
                // /api/prompts/{prompt_key}/revert/{target_version}
                if (method.equals("POST")) {
                    String promptKey = segments[2];
                    int targetVersion = Integer.parseInt(segments[4]);
                    revertPromptToVersion(promptKey, targetVersion, response);
                } else {
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
                }
            } else if (segments.length >= 3) {
                // /api/prompts/{prompt_key}
                String promptKey = segments[2];
                if (method.equals("GET")) {
                    getPrompt(promptKey, response);
                } else if (method.equals("PUT")) {
                    updatePrompt(promptKey, request, response);
                } else if (method.equals("DELETE")) {
                    deletePrompt(promptKey, response);
                } else {
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
                }
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Path not found: " + path);
            }
        } catch (Exception e) {
            log.error("Handle prompt API failed: " + path, e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Internal server error: " + e.getMessage()).toMap());
        }
    }

    /**
     * Handle rating-related routes
     */
    private void ratingRoutes(String path, HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Parse path segments
        String[] segments = path.split("/");
        String method = request.getMethod();

        try {
            // Handle different rating endpoints
            if (segments.length == 3 && segments[2].equals("clear_all")) {
                // /rating/clear_all
                if (method.equals("DELETE")) {
                    clearAllRatingData(response);
                } else {
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
                }
            } else if (segments.length == 3 && segments[2].equals("setup_indices")) {
                // /rating/setup_indices
                if (method.equals("POST")) {
                    setupRatingIndices(response);
                } else {
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
                }
            } else if (segments.length == 4 && segments[3].equals("rebuild_stats")) {
                // /rating/{trace_id}/rebuild_stats
                if (method.equals("POST")) {
                    String traceId = segments[2];
                    rebuildRatingStats(traceId, response);
                } else {
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
                }
            } else if (segments.length == 4 && segments[2].equals("current")) {
                // /rating/{trace_id}/current
                if (method.equals("GET")) {
                    String traceId = segments[3];
                    String erp = request.getParameter("erp");
                    getCurrentRating(traceId, erp, response);
                } else {
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
                }
            } else if (segments.length == 4 && segments[2].equals("history")) {
                // /rating/{trace_id}/history
                if (method.equals("GET")) {
                    String traceId = segments[3];
                    String erp = request.getParameter("erp");
                    getRatingHistory(traceId, erp, response);
                } else {
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
                }
            } else if (segments.length == 3) {
                // /rating/{trace_id}
                if (method.equals("GET")) {
                    String traceId = segments[2];
                    getRatingStats(traceId, response);
                } else if (method.equals("DELETE")) {
                    String ratingId = segments[2];
                    deleteRating(ratingId, response);
                } else {
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
                }
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Path not found: " + path);
            }
        } catch (Exception e) {
            log.error("Handle rating route failed: " + path, e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Internal server error: " + e.getMessage()).toMap());
        }
    }

    /**
     * List prompts
     */
    private void listPrompts(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String category = request.getParameter("category");
            String agentType = request.getParameter("agent_type");
            String isActiveStr = request.getParameter("is_active");
            Boolean isActive = isActiveStr != null ? Boolean.parseBoolean(isActiveStr) : null;
            String tagsStr = request.getParameter("tags");
            List<String> tags = tagsStr != null ? Arrays.asList(tagsStr.split(",")) : null;

            List<Map<String, Object>> prompts = promptManager.listPrompts(category, agentType, isActive, tags);

            Map<String, Object> responseData = Map.of(
                    "success", true,
                    "message", "Successfully retrieved prompt list",
                    "data", prompts
            );

            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
        } catch (Exception e) {
            log.error("List prompts failed", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Failed to list prompts: " + e.getMessage()).toMap());
        }
    }

    /**
     * Search prompts
     */
    private void searchPrompts(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String keyword = request.getParameter("keyword");
            String category = request.getParameter("category");

            if (keyword == null || keyword.isEmpty()) {
                sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST, WebResponse.error(400, "Keyword is required").toMap());
                return;
            }

            List<Map<String, Object>> results = promptManager.searchPrompts(keyword, category);

            Map<String, Object> responseData = Map.of(
                    "success", true,
                    "message", "Successfully searched prompts",
                    "data", results
            );

            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
        } catch (Exception e) {
            log.error("Search prompts failed", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Failed to search prompts: " + e.getMessage()).toMap());
        }
    }

    /**
     * Hot reload specified prompt to all related agents
     */
    private void hotReloadPrompt(String promptKey, HttpServletResponse response) throws IOException {
        try {
            boolean success = DynamicAgentManager.hotReloadPrompt(promptKey);

            Map<String, Object> responseData = Map.of(
                    "success", success,
                    "message", "Successfully hot reloaded prompt",
                    "data", Map.of(
                            "prompt_key", promptKey,
                            "hot_reload_success", success
                    )
            );

            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
        } catch (Exception e) {
            log.error("Hot reload prompt failed", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Failed to hot reload prompt: " + e.getMessage()).toMap());
        }
    }

    /**
     * Hot reload prompt for specified agent
     */
    private void hotReloadAgentPrompt(String agentName, HttpServletResponse response) throws IOException {
        try {
            boolean success = DynamicAgentManager.hotReloadAgent(agentName);

            Map<String, Object> responseData = Map.of(
                    "success", success,
                    "message", "Successfully hot reloaded agent prompt",
                    "data", Map.of(
                            "agent_name", agentName,
                            "hot_reload_success", success
                    )
            );

            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
        } catch (Exception e) {
            log.error("Hot reload agent prompt failed", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Failed to hot reload agent prompt: " + e.getMessage()).toMap());
        }
    }

    /**
     * Hot reload all agent prompts
     */
    private void hotReloadAllAgentPrompts(HttpServletResponse response) throws IOException {
        try {
            boolean results = DynamicAgentManager.hotReloadAllPrompts();

            Map<String, Object> responseData = Map.of(
                    "success", results,
                    "message", "Successfully completed batch hot reload",
                    "data", Map.of(
                            "reload_success", results,
                            "reload_time", LocalDateTime.now().format(DATE_TIME_FORMATTER)
                    )
            );
            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
        } catch (Exception e) {
            log.error("Hot reload all prompts failed", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Failed to hot reload all prompts: " + e.getMessage()).toMap());
        }
    }

    /**
     * Get single prompt
     */
    private void getPrompt(String promptKey, HttpServletResponse response) throws IOException {
        try {
            Map<String, Object> prompt = promptManager.getPrompt(promptKey, true);

            if (prompt == null) {
                sendJsonResponse(response, HttpServletResponse.SC_NOT_FOUND,
                        WebResponse.error(404, "Prompt not found").toMap());
                return;
            }

            prompt.put("id", promptKey);

            Map<String, Object> responseData = Map.of(
                    "success", true,
                    "message", "Successfully retrieved prompt",
                    "data", prompt
            );

            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
        } catch (Exception e) {
            log.error("Get prompt failed: " + promptKey, e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Failed to get prompt: " + e.getMessage()).toMap());
        }
    }

    /**
     * Create prompt
     */
    private void createPrompt(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Map<String, Object> requestBody = readRequestBody(request);

            String promptKey = (String) requestBody.get("prompt_key");
            String promptContent = (String) requestBody.get("prompt_content");
            String description = (String) requestBody.getOrDefault("description", "");
            String category = (String) requestBody.getOrDefault("category", "custom");
            String agentType = (String) requestBody.getOrDefault("agent_type", "");
            boolean isActive = (boolean) requestBody.getOrDefault("is_active", true);
            List<String> tags = (List<String>) requestBody.getOrDefault("tags", List.of());
            String createdBy = (String) requestBody.getOrDefault("created_by", "user");

            // Check if prompt already exists
            Map<String, Object> existing = promptManager.getPrompt(promptKey, true);
            if (existing != null) {
                sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        WebResponse.error(400, "Prompt already exists").toMap());
                return;
            }

            boolean success = promptManager.savePrompt(promptKey, promptContent, description, category, agentType,
                    1, isActive, tags, createdBy);

            if (!success) {
                sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        WebResponse.error(500, "Failed to create prompt").toMap());
                return;
            }

            Map<String, Object> responseData = Map.of(
                    "success", true,
                    "message", "Successfully created prompt",
                    "data", Map.of("prompt_key", promptKey)
            );

            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
        } catch (Exception e) {
            log.error("Create prompt failed", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Failed to create prompt: " + e.getMessage()).toMap());
        }
    }

    /**
     * Update prompt
     */
    private void updatePrompt(String promptKey, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Map<String, Object> requestBody = readRequestBody(request);

            // Get existing prompt
            Map<String, Object> existing = promptManager.getPrompt(promptKey, true);
            if (existing == null) {
                sendJsonResponse(response, HttpServletResponse.SC_NOT_FOUND,
                        WebResponse.error(404, "Prompt not found").toMap());
                return;
            }

            // Extract update data
            String promptContent = (String) requestBody.get("prompt_content");
            String description = (String) requestBody.getOrDefault("description", existing.getOrDefault("description", ""));
            String category = (String) requestBody.getOrDefault("category", existing.getOrDefault("category", "custom"));
            String agentType = (String) requestBody.getOrDefault("agent_type", existing.getOrDefault("agent_type", ""));
            List<String> tags = (List<String>) requestBody.getOrDefault("tags", existing.getOrDefault("tags", new ArrayList<>()));
            Boolean isActive = (Boolean) requestBody.getOrDefault("is_active", existing.getOrDefault("is_active", true));

            // Check if there are changes
            boolean hasChanges = promptContent != null && !promptContent.equals(existing.get("prompt_content"));

            if (!hasChanges) {
                Map<String, Object> responseData = Map.of(
                        "success", false,
                        "message", "No changes detected; update the prompt before saving.",
                        "data", Map.of("prompt_key", promptKey)
                );
                sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
                return;
            }

            // Update prompt
            boolean success = promptManager.savePrompt(promptKey, promptContent, description, category, agentType,
                    1, isActive, tags, (String) existing.getOrDefault("created_by", "user"));

            if (!success) {
                sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        WebResponse.error(500, "Failed to update prompt").toMap());
                return;
            }

            Map<String, Object> responseData = Map.of(
                    "success", true,
                    "message", "Successfully updated prompt",
                    "data", Map.of("prompt_key", promptKey)
            );

            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
        } catch (Exception e) {
            log.error("Update prompt failed: " + promptKey, e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Failed to update prompt: " + e.getMessage()).toMap());
        }
    }

    /**
     * Delete prompt
     */
    private void deletePrompt(String promptKey, HttpServletResponse response) throws IOException {
        try {
            boolean success = promptManager.deletePrompt(promptKey);

            if (!success) {
                sendJsonResponse(response, HttpServletResponse.SC_NOT_FOUND,
                        WebResponse.error(404, "Prompt not found").toMap());
                return;
            }

            Map<String, Object> responseData = Map.of(
                    "success", true,
                    "message", "Successfully deleted prompt",
                    "data", Map.of("prompt_key", promptKey)
            );

            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
        } catch (Exception e) {
            log.error("Delete prompt failed: " + promptKey, e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Failed to delete prompt: " + e.getMessage()).toMap());
        }
    }

    /**
     * Get prompt version history
     */
    private void getPromptHistory(String promptKey, HttpServletResponse response) throws IOException {
        try {
            List<Map<String, Object>> history = promptManager.getPromptHistory(promptKey);

            Map<String, Object> responseData = Map.of(
                    "success", true,
                    "message", "Successfully retrieved prompt history",
                    "data", history
            );

            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
        } catch (Exception e) {
            log.error("Get prompt history failed: " + promptKey, e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Failed to get prompt history: " + e.getMessage()).toMap());
        }
    }

    /**
     * Revert prompt to specific version
     */
    private void revertPromptToVersion(String promptKey, int targetVersion, HttpServletResponse response) throws IOException {
        try {
            // Check if prompt exists
            Map<String, Object> existing = promptManager.getPrompt(promptKey, false);
            if (existing == null) {
                sendJsonResponse(response, HttpServletResponse.SC_NOT_FOUND,
                        WebResponse.error(404, "Prompt not found").toMap());
                return;
            }

            // Revert to target version
            boolean success = promptManager.revertToVersion(promptKey, targetVersion);

            if (!success) {
                sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        WebResponse.error(400, "Failed to revert to version " + targetVersion).toMap());
                return;
            }

            Map<String, Object> responseData = Map.of(
                    "success", true,
                    "message", "Successfully reverted " + promptKey + " to version " + targetVersion,
                    "data", Map.of(
                            "prompt_key", promptKey,
                            "reverted_to_version", targetVersion,
                            "revert_time", LocalDateTime.now().format(DATE_TIME_FORMATTER)
                    )
            );

            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
        } catch (Exception e) {
            log.error("Revert prompt failed: " + promptKey + ", version: " + targetVersion, e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Failed to revert prompt: " + e.getMessage()).toMap());
        }
    }

    /**
     * Get specific version of a prompt
     */
    private void getPromptVersion(String promptKey, int version, HttpServletResponse response) throws IOException {
        try {
            // Get version history
            List<Map<String, Object>> history = promptManager.getPromptHistory(promptKey);

            // Find the specific version
            Map<String, Object> targetVersion = null;
            for (Map<String, Object> hist : history) {
                if (Integer.parseInt(hist.get("version").toString()) == version) {
                    targetVersion = hist;
                    break;
                }
            }

            if (targetVersion == null) {
                sendJsonResponse(response, HttpServletResponse.SC_NOT_FOUND,
                        WebResponse.error(404, "Version " + version + " not found for prompt " + promptKey).toMap());
                return;
            }

            Map<String, Object> responseData = Map.of(
                    "success", true,
                    "message", "Successfully retrieved version " + version + " of prompt " + promptKey,
                    "data", targetVersion
            );

            sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
        } catch (Exception e) {
            log.error("Get prompt version failed: " + promptKey + ", version: " + version, e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Failed to get prompt version: " + e.getMessage()).toMap());
        }
    }

    /**
     * Get detailed agent information for frontend display.
     * @param request
     * @param response
     * @throws IOException
     */
    private void getAgents(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // Extract agent information from MAS
            List<Map<String, Object>> agents = new ArrayList<>();

            // Get agents from oxy_name_to_oxy registry
            for (Map.Entry<String, BaseOxy> entry : mas.getOxyNameToOxy().entrySet()) {
                String agentName = entry.getValue().getName();
                BaseOxy oxyInstance = entry.getValue();

                Map<String, Object> agentInfo = new HashMap<>();
                agentInfo.put("name", agentName);
                agentInfo.put("desc", "");
                agentInfo.put("type", "agent");
                agentInfo.put("class_name", oxyInstance.getClass().getSimpleName());
                agentInfo.put("path", List.of(agentName));

                agents.add(agentInfo);
            }

            Map<String, Object> data = Map.of("agents", agents);
            sendJsonResponse(response, HttpServletResponse.SC_OK,
                    WebResponse.success(data).toMap());

//            sendSseEvent(response, "message", Map.of("agents", mas.getAgentOrganization()));
        } catch (Exception e) {
            log.error("Get agents failed", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Failed to get agents: " + e.getMessage()).toMap());
        }
    }

    /**
     * Create or update conversation rating
     */
    private void rating(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Map<String, Object> payload = readRequestBody(request);
            RatingRequest ratingRequest = new RatingRequest((String) payload.get("trace_id"), RatingType.valueOf(((String) payload.get("rating_type")).toUpperCase()), (String) payload.get("comment"), (String) payload.get("erp"));
            RatingResponse result = evaluationManager.createRating(ratingRequest, request, null);

            Map<String, Object> data = new HashMap<>();
            data.put("rating_id", result.getRatingId());
            if (result.getCurrentStats() != null) {
                data.put("stats", result.getCurrentStats().toMap());
            }

            if (result.isSuccess()) {
                sendJsonResponse(response, HttpServletResponse.SC_OK,
                        WebResponse.success(data).toMap());
            } else {
                sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        WebResponse.error(400, result.getMessage()).toMap());
            }
        } catch (Exception e) {
            log.error("Create rating error", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Rating operation failed: " + e.getMessage()).toMap());
        }
    }

    /**
     * Get rating statistics for a specific conversation
     */
    private void getRatingStats(String traceId, HttpServletResponse response) throws IOException {
        try {
            Optional<RatingStats> stats = evaluationManager.getRatingStats(traceId);
            if (stats.isPresent()) {
                sendJsonResponse(response, HttpServletResponse.SC_OK, WebResponse.success(stats.get()).toMap());
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("trace_id", traceId);
                data.put("like_count", 0);
                data.put("dislike_count", 0);
                data.put("total_ratings", 0);
                data.put("satisfaction_rate", 0.0);
                sendJsonResponse(response, HttpServletResponse.SC_OK, WebResponse.success(stats, "No rating data available").toMap());
            }
        } catch (Exception e) {
            log.error("Get rating stats failed: " + traceId, e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Failed to get rating statistics: " + e.getMessage()).toMap());
        }
    }

    /**
     * Get current rating record for a specific conversation
     */
    private void getCurrentRating(String traceId, String erp, HttpServletResponse response) throws IOException {
        try {
            List<ConversationRating> ratings = evaluationManager.getRatingHistory(traceId, Optional.of(erp));
            ConversationRating currentRating = ratings.isEmpty() ? null : ratings.get(0);
            Map<String, Object> data = new HashMap<>();
            data.put("trace_id", traceId);
            if (currentRating != null) {
                data.put("current_rating", currentRating.toMap());
            } else {
                data.put("current_rating", null);
            }
            sendJsonResponse(response, HttpServletResponse.SC_OK,
                    WebResponse.success(data).toMap());
        } catch (Exception e) {
            log.error("Get current rating failed: " + traceId, e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Failed to get current rating: " + e.getMessage()).toMap());
        }
    }

    /**
     * Get all rating history records for a specific conversation
     */
    private void getRatingHistory(String traceId, String erp, HttpServletResponse response) throws IOException {
        try {
            List<ConversationRating> history = evaluationManager.getRatingHistory(traceId, Optional.of(erp));
            List<Map<String, Object>> ratingsData = history.stream()
                    .map(ConversationRating::toMap)
                    .collect(Collectors.toList());
            Map<String, Object> data = new HashMap<>();
            data.put("trace_id", traceId);
            data.put("ratings", ratingsData);
            data.put("count", ratingsData.size());
            data.put("erp_filter", erp);
            sendJsonResponse(response, HttpServletResponse.SC_OK,
                    WebResponse.success(data).toMap());
        } catch (Exception e) {
            log.error("Get rating history failed: " + traceId, e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Failed to get rating history: " + e.getMessage()).toMap());
        }
    }

    /**
     * Clear all rating data
     */
    private void clearAllRatingData(HttpServletResponse response) throws IOException {
        try {
            Map<String, Object> result = evaluationManager.clearAllRatingData();
            sendJsonResponse(response, HttpServletResponse.SC_OK,
                    WebResponse.success(result).toMap());
        } catch (Exception e) {
            log.error("Clear all rating data failed", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Failed to clear rating data: " + e.getMessage()).toMap());
        }
    }

    /**
     * Setup rating-related indexes
     */
    private void setupRatingIndices(HttpServletResponse response) throws IOException {
        try {
            Map<String, Object> result = evaluationManager.ensureRatingIndicesWithCorrectMapping();
            if ((Boolean) result.get("success")) {
                List<String> createdIndices = new ArrayList<>();
                if ((Boolean) result.get("rating_index_created")) {
                    createdIndices.add("rating index");
                }
                if ((Boolean) result.get("rating_stats_index_created")) {
                    createdIndices.add("rating stats index");
                }
                String message;
                if (!createdIndices.isEmpty()) {
                    message = "Successfully created indexes: " + String.join(", ", createdIndices);
                } else {
                    message = "All indexes already exist, no creation needed";
                }
                sendJsonResponse(response, HttpServletResponse.SC_OK,
                        WebResponse.success(result).toMap());
            } else {
                sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        WebResponse.error(500, "Failed to setup indexes, errors: " + result.get("errors")).toMap());
            }
        } catch (Exception e) {
            log.error("Setup rating indices failed", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Failed to setup indexes: " + e.getMessage()).toMap());
        }
    }

    /**
     * Rebuild rating statistics for specific conversation
     */
    private void rebuildRatingStats(String traceId, HttpServletResponse response) throws IOException {
        try {
            RatingStats stats = evaluationManager.updateRatingStats(traceId, null);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("trace_id", traceId);
            responseData.put("rebuilt_stats", stats);
            responseData.put("message", "Statistics recalculated");

            sendJsonResponse(response, HttpServletResponse.SC_OK,
                    WebResponse.success(responseData).toMap());
        } catch (Exception e) {
            log.error("Rebuild rating stats failed: " + traceId, e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Failed to rebuild statistics: " + e.getMessage()).toMap());
        }
    }

    /**
     * Delete specified rating record
     */
    private void deleteRating(String ratingId, HttpServletResponse response) throws IOException {
        try {
            boolean success = evaluationManager.deleteRating(ratingId);
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("deleted", success);
            responseData.put("rating_id", ratingId);

            sendJsonResponse(response, HttpServletResponse.SC_OK,
                    WebResponse.success(responseData).toMap());
        } catch (Exception e) {
            log.error("Delete rating failed: " + ratingId, e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Failed to delete rating: " + e.getMessage()).toMap());
        }
    }


    /**
     * Get conversation history with ratings.
     * @return WebResponse with conversation groups
     */
    private void historyWithRatings(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            int page = Integer.parseInt(CommonUtils.getOrDefault(request.getParameter("page"), "1"));
            int pageSize = Integer.parseInt(CommonUtils.getOrDefault(request.getParameter("page_size"), "20"));
            String ratingFilter = CommonUtils.getOrDefault(request.getParameter("rating_filter"), "all");
            String searchTerm = CommonUtils.getOrDefault(request.getParameter("search_term"), "");

            Map<String, Object> responseData = evaluationManager.historyWithRatings(ratingFilter, searchTerm, page, pageSize);
            sendJsonResponse(response, HttpServletResponse.SC_OK, WebResponse.success(responseData).toMap());
        } catch (Exception e) {
            log.error("Get history with ratings failed", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Failed to get conversation history: " + e.getMessage()).toMap());
        }
    }

    /**
     * Get rating statistics and analysis data
     */
    private void analyticsRatings(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            int days = Integer.parseInt(CommonUtils.getOrDefault(request.getParameter("days"), "7"));
            Map<String, Object> stats = evaluationManager.getOverallRatingStats(days);
            sendJsonResponse(response, HttpServletResponse.SC_OK,
                    WebResponse.success(stats).toMap());
        } catch (Exception e) {
            log.error("Get rating analytics failed", e);
            sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    WebResponse.error(500, "Failed to get rating analytics: " + e.getMessage()).toMap());
        }
    }

}