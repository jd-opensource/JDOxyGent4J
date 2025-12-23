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
import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.MasFactoryRegistry;
import com.jd.oxygent.core.oxygent.samples.server.utils.FileValidationUtil;
import com.jd.oxygent.core.oxygent.samples.server.utils.RecursivePackageInstantiator;
import com.jd.oxygent.core.oxygent.samples.server.vo.*;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.jd.oxygent.core.oxygent.samples.server.ServerConstants.*;

/**
 * Main routing servlet for OxyGent server that handles various HTTP endpoints
 * including chat, file upload, script management, and system information.
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@WebServlet(name = "RouteServlet", urlPatterns = {
        "/", "/check_alive", "/get_organization", "/get_first_query",
        "/get_welcome_message", "/list_script", "/save_script", "/load_script",
        "/chat", "/sse/chat", "/async/chat", "/node", "/view", "/call", "/upload"
}, loadOnStartup = 1)
public class RouteServlet extends HttpServlet {

    private final Mas mas = MasFactoryRegistry.getFactory().createMas();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Function<Map<String, Object>, Map<String, Object>> funcInterceptor = x -> null;
    private Function<Map<String, Object>, Map<String, Object>> funcFilter = x -> x;
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
                    handleRoot(request, response);
                    break;
                case "/check_alive":
                    handleCheckAlive(request, response);
                    break;
                case "/get_organization":
                    handleGetOrganization(request, response);
                    break;
                case "/get_first_query":
                    handleGetFirstQuery(request, response);
                    break;
                case "/get_welcome_message":
                    handleGetWelcomeMessage(request, response);
                    break;
                case "/list_script":
                    handleListScript(request, response);
                    break;
                case "/save_script":
                    handleSaveScript(request, response);
                    break;
                case "/load_script":
                    handleLoadScript(request, response);
                    break;
                case "/api/group_uid":
                    handleGroupId(request, response);
                    break;
                case "/api/trace_uid":
                    handleTraceId(request, response);
                    break;
                case "/chat":
                    handleChat(request, response);
                    break;
                case "/sse/chat":
                    handleSseChat(request, response);
                    break;
                case "/async/chat":
                    handleAsyncChat(request, response);
                    break;
                case "/node":
                    handleGetNodeInfo(request, response);
                    break;
                case "/view":
                    handleGetTaskInfo(request, response);
                    break;
                case "/call":
                    handleCall(request, response);
                    break;
                case "/upload":
                    handleUploadFile(request, response);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Path not found: " + path);
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
    private void handleRoot(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect("./index.html");
    }

    /**
     * Health check endpoint
     */
    private void handleCheckAlive(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> result = new HashMap<>();
        result.put("alive", 1);
        sendJsonResponse(response, HttpServletResponse.SC_OK, WebResponse.success(result).toMap());
    }

    /**
     * Get agent organization structure
     */
    private void handleGetOrganization(HttpServletRequest request, HttpServletResponse response) throws IOException {
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
    private void handleGetFirstQuery(HttpServletRequest request, HttpServletResponse response) throws IOException {
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
    private void handleGetWelcomeMessage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String welcomeMessage = Config.getServer().getWelcomeMessage() != null
                    ? Config.getServer().getWelcomeMessage() : "";
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
    private void handleListScript(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String scriptSaveDir = Paths.get(Config.getXfile().getSaveDir(), "script").toString();
            Files.createDirectories(Paths.get(scriptSaveDir));

            File dir = new File(scriptSaveDir);
            String[] files = dir.list((d, name) -> name.endsWith(".json"));

            List<String> scripts = new ArrayList<>();
            if (files != null) {
                scripts = Arrays.stream(files)
                        .map(file -> file.substring(0, file.lastIndexOf(".")))
                        .collect(Collectors.toList());
            }

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
    private void handleGroupId(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("uuid", UuidCreator.getShortSuffixComb().toString());
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
    private void handleTraceId(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("uuid", UuidCreator.getShortSuffixComb().toString());
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
            return new HashMap<>();
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
    private void handleSaveScript(HttpServletRequest request, HttpServletResponse response) throws IOException {
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
     * Load script
     */
    private void handleLoadScript(HttpServletRequest request, HttpServletResponse response) throws IOException {
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
    private void handleChat(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Map<String, Object> payload = readRequestBody(request);
            Map<String, String> headers = extractHeaders(request);

            payload = requestToPayload(payload, headers);

            // Apply interceptor
            if (funcInterceptor != null) {
                Object interceptedResponse = funcInterceptor.apply(payload);
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
    private void handleSseChat(HttpServletRequest request, HttpServletResponse response) throws IOException {
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
            if (funcInterceptor != null) {
                Object interceptedResponse = funcInterceptor.apply(payload);
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
            eventStream(redisKey, currentTraceId, task, response);

        } catch (Exception e) {
            log.error("SSE chat failed", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SSE chat failed: " + e.getMessage());
        }
    }

    /**
     * Asynchronous chat interface
     */
    private void handleAsyncChat(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Map<String, Object> payload = readRequestBody(request);
            Map<String, String> headers = extractHeaders(request);

            payload = requestToPayload(payload, headers);

            // Apply interceptor
            if (funcInterceptor != null) {
                Object interceptedResponse = funcInterceptor.apply(payload);
                if (interceptedResponse != null) {
                    sendJsonResponse(response, HttpServletResponse.SC_OK, (Map<String, Object>) interceptedResponse);
                    return;
                }
            }

            String currentTraceId = payload.getOrDefault("current_trace_id", "").toString();
            log.info("Async task created。trace_id: {}", currentTraceId);

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
    private void handleGetNodeInfo(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String itemId = request.getParameter("item_id");
            if (itemId == null || itemId.trim().isEmpty()) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing item_id parameter");
                return;
            }

            // Search nodes
            Map<String, Object> query = new HashMap<>();
            query.put("query", Map.of("term", Map.of("_id", itemId)));

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
                    Map<String, Object> classAttr = (Map<String, Object>) input.get("class_attr");
                    if (classAttr.containsKey("prompt")) {
                        classAttr.remove("prompt");
                    }

                    // Process environment variables
                    Map<String, String> envValueToKey = new HashMap<>();
                    System.getenv().forEach((k, v) -> envValueToKey.put(v, k));

                    // Generate data range
                    Map<String, Map<String, Object>> dataRangeMap = new HashMap<>();
                    nodeData.put("data_range_map", dataRangeMap);

                    List<Map<String, Object>> trees = Arrays.asList(classAttr,
                            (Map<String, Object>) classAttr.getOrDefault("llm_params", new HashMap<>()),
                            (Map<String, Object>) input.get("arguments"));

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
    private void handleGetTaskInfo(HttpServletRequest request, HttpServletResponse response) throws IOException {
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
    private void handleCall(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            ItemRequest item = objectMapper.readValue(readRequestBodyAsString(request), ItemRequest.class);

            // Process environment variables pattern matching
            List<Map<String, Object>> trees = Arrays.asList(item.getClassAttr(),
                    (Map<String, Object>) item.getClassAttr().getOrDefault("llm_params", new HashMap<>()),
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
        if (funcFilter != null) {
            payload = funcFilter.apply(payload);
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
                List<String> existingUrls = (List<String>) payload.getOrDefault("web_file_url_list", new ArrayList<>());
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
        payload.putIfAbsent("shared_data", new HashMap<String, Object>());
        // Add request headers
        ((Map<String, Object>) payload.get("shared_data")).put("_headers", headers);

        return payload;
    }

    /**
     * Event stream handler
     */
    private void eventStream(String redisKey, String currentTraceId, CompletableFuture<OxyResponse> task,
                             HttpServletResponse response) throws Exception {
        try {
            while (true) {
                // Read message from Redis
                Object rpop = mas.getRedisClient().brpop(redisKey);
                if (rpop == null) {
                    Thread.sleep(100);
                    continue;
                }

                // Unpack message
                Map<String, Object> msgMap = mas.unpackMessage(Base64.getDecoder().decode((String) rpop));
                if (msgMap != null) {
                    // Check if it is a close event
                    if (msgMap.containsKey("event")) {
                        sendSseEvent(response, (String) msgMap.get("event"), msgMap);
                        log.info("SSE connection terminated。trace_id: {}", currentTraceId);
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
                }
                // Send message
                sendSseEvent(response, "message", msgMap);
            }
        } catch (InterruptedException e) {
            log.info("SSE connection terminated。trace_id: {}", currentTraceId);
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
    private void handleUploadFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
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
            Map<String, Object> data = new HashMap<>();
            data.put("file_name", fileName);
            sendJsonResponse(response, HttpServletResponse.SC_OK, WebResponse.success(data).toMap());
        } catch (Exception e) {
            log.error("File upload failed", e);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "File upload failed: " + e.getMessage());
        }
    }
}