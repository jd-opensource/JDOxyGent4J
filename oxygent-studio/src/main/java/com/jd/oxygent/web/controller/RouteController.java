package com.jd.oxygent.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.jd.oxygent.web.adapter.FileItemAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.view.RedirectView;

import java.io.File;
import java.io.IOException;
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

import static com.jd.oxygent.core.oxygent.samples.server.ServerConstants.RESTRICTED_HEADERS;

/**
 * Web routing controller for OxyGent.
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/")
public class RouteController {

    private final Mas mas = MasFactoryRegistry.getFactory().createMas();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ObjectMapper webMvcObjectMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    Config config;

    private Function<Map<String, Object>, Map<String, Object>> funcInterceptor = x -> null;

    // Functional component (for request filtering and interception)
    private Function<Map<String, Object>, Map<String, Object>> funcFilter = x -> x;

    /**
     * Redirect clients to the packaged web frontend.
     *
     * @return RedirectView redirect to ./web/index.html
     */
    @GetMapping("/")
    public RedirectView readRoot() {
        return new RedirectView("./web/index.html");
    }

    /**
     * Health check endpoint.
     *
     * @return Map returns {"alive": 1} when service is running
     */
    @GetMapping("/check_alive")
    public ResponseEntity<Map<String, Object>> checkAlive() {
        Map<String, Object> response = new HashMap<>();
        response.put("alive", 1);
        return ResponseEntity.ok(response);
    }

    /**
     * Get agent organization structure.
     * <p>
     * Populate for all nodes:
     * - path: path from the root (master agent) to the current node
     * - id_dict: dictionary mapping agent names to their unique IDs
     *
     * @return ResponseEntity with organization WebResponse
     */
    @GetMapping("/get_organization")
    public ResponseEntity<Map<String, Object>> getOrganization() {
        try {

            // Convert to a structure with path
            OrganizationWrapper organizedWithPath = AgentNodeConverter.convertToOrganization(mas.getAgentOrganization());

            return ResponseEntity.ok(WebResponse.success(organizedWithPath).toMap());
        } catch (Exception e) {
            log.error("Failed to get organization", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(500, "Failed to get organization").toMap());
        }
    }

    /**
     * Get the first query.
     * <p>
     * When the frontend loads, it will send the first query to the user.
     *
     * @return ResponseEntity containing the first query WebResponse
     */
    @GetMapping("/get_first_query")
    public ResponseEntity<Map<String, Object>> getFirstQuery() {
        try {
            String firstQuery = mas.getFirstQuery() != null && !mas.getFirstQuery().isEmpty() ? mas.getFirstQuery() : config.getServer().getFirstQuery();
            Map<String, Object> data = Map.of("first_query", firstQuery);
            return ResponseEntity.ok(WebResponse.success(data).toMap());
        } catch (Exception e) {
            log.error("Failed to get first query", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(500, "Failed to get first query").toMap());
        }
    }

    /**
     * Get the welcome message.
     *
     * @return ResponseEntity containing the welcome message WebResponse
     */
    @GetMapping("/get_welcome_message")
    public ResponseEntity<Map<String, Object>> getWelcomeMessage() {
        try {
            String welcomeMessage = config.getServer().getWelcomeMessage() != null ? config.getServer().getWelcomeMessage() : "";
            Map<String, Object> data = Map.of("welcome_message", welcomeMessage);
            return ResponseEntity.ok(WebResponse.success(data).toMap());
        } catch (Exception e) {
            log.error("Failed to get welcome message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(500, "Failed to get welcome message").toMap());
        }
    }

    /**
     * List all saved scripts.
     *
     * @return ResponseEntity containing a list of script names
     */
    @GetMapping("/list_script")
    public ResponseEntity<Map<String, Object>> listScript() {
        try {
            String scriptSaveDir = Paths.get(config.getXfile().getSaveDir(), "script").toString();
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
            return ResponseEntity.ok(WebResponse.success(data).toMap());

        } catch (IOException e) {
            log.error("Failed to list scripts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(500, "Failed to list scripts").toMap());
        }
    }

    /**
     * Persist script definitions to $CACHE_DIR/script.
     *
     * @param script Script metadata and payload to store
     * @return ResponseEntity containing the generated script_id
     */
    @PostMapping("/save_script")
    public ResponseEntity<Map<String, Object>> saveScript(@RequestBody ScriptRequest script) {
        try {
            String scriptSaveDir = Paths.get(config.getXfile().getSaveDir(), "script").toString();
            Files.createDirectories(Paths.get(scriptSaveDir));

            Path filePath = Paths.get(scriptSaveDir, script.getName() + ".json");
            String jsonContent = objectMapper.writeValueAsString(script.getContents());
            Files.write(filePath, jsonContent.getBytes());

            Map<String, Object> data = Map.of("script_id", script.getName() + ".json");
            return ResponseEntity.ok(WebResponse.success(data).toMap());

        } catch (IOException e) {
            log.error("Failed to save script", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(500, "Failed to save script").toMap());
        }
    }

    /**
     * Load a previously saved script.
     *
     * @param itemId Script identifier
     * @return ResponseEntity containing the original contents array
     */
    @GetMapping("/load_script")
    public ResponseEntity<Map<String, Object>> loadScript(@RequestParam("item_id") String itemId) {
        try {
            String scriptSaveDir = Paths.get(config.getXfile().getSaveDir(), "script").toString();
            Path jsonPath = Paths.get(scriptSaveDir, itemId + ".json");

            if (!Files.exists(jsonPath)) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(WebResponse.error(500, "File not found").toMap());
            }

            String content = Files.readString(jsonPath);
            List<Object> contents = objectMapper.readValue(content, List.class);

            Map<String, Object> data = Map.of("contents", contents);
            return ResponseEntity.ok(WebResponse.success(data).toMap());

        } catch (IOException e) {
            log.error("Failed to load script", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(500, "Failed to load script").toMap());
        }
    }


    /**
     * @return Get session ID
     */
    @GetMapping("/api/group_uid")
    public ResponseEntity<Map<String, Object>> groupId() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("uuid", UuidCreator.getShortSuffixComb().toString());
            return ResponseEntity.ok(WebResponse.success(response).toMap());
        } catch (Exception e) {
            log.error("Failed to generate group_uid", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(500, "Internal server error requesting groupid").toMap());
        }
    }

    /**
     * @return Get single request trace ID
     */
    @GetMapping("/api/trace_uid")
    public ResponseEntity<Map<String, Object>> traceId() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("uuid", UuidCreator.getShortSuffixComb().toString());
            return ResponseEntity.ok(WebResponse.success(response).toMap());
        } catch (Exception e) {
            log.error("Failed to generate trace_uid", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(500, "Internal server error requesting traceid").toMap());
        }
    }

    /**
     * Convert request into payload.
     */
    private Map<String, Object> requestToPayload(Map<String, Object> payload, Map<String, String> headers) throws Exception {

        // Apply filter
        if (funcFilter != null) {
            payload = funcFilter.apply(payload);
        }

        // Set default query
        payload.putIfAbsent("query", "");

        // Handle attachments
        if (payload.containsKey("attachments")) {
            List<String> attachments = (List<String>) payload.get("attachments");
            List<String> attachmentsWithPath = new ArrayList<>();
            List<String> remoteUrls = new ArrayList<>();

            for (String attachment : attachments) {
                boolean isRemote = attachment.startsWith("http://") || attachment.startsWith("https://");
                String filePath = isRemote ? attachment : config.getXfile().getSaveDir() + "/uploads/" + attachment;
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

            // Compose query parts (a2a style)
            String query = payload.getOrDefault("query", "").toString();
            payload.put("query", CommonUtils.composeQueryParts(query, attachmentsWithPath));
        }

        // Set current trace_id
        payload.putIfAbsent("current_trace_id", CommonUtils.generateShortUUID());
        // Get request headers
        payload.putIfAbsent("shared_data", new HashMap<String, Object>());
        // Add headers
        ((Map<String, Object>) payload.get("shared_data")).put("_headers", headers);

        return payload;
    }

    /**
     * Chat endpoint - synchronous mode.
     * <p>
     * Accept user query and return the complete response.
     *
     * @param payload HTTP request body
     * @param headers HTTP request headers
     * @return ResponseEntity containing chat response
     */
    @RequestMapping(value = "/chat", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> payload, @RequestHeader Map<String, String> headers) {
        try {
            requestToPayload(payload, headers);

            // Apply interceptor
            if (funcInterceptor != null) {
                Object interceptedResponse = funcInterceptor.apply(payload);
                if (interceptedResponse != null) {
                    return ResponseEntity.ok(interceptedResponse);
                }
            }

            // Execute chat
            OxyResponse oxyResponse = mas.chatWithAgent(payload, null);

            return ResponseEntity.ok(oxyResponse.getOutput());
        } catch (Exception e) {
            log.error("Chat failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(500, "Chat failed: " + e.getMessage()).toMap());
        }
    }

    /**
     * SSE chat endpoint - Server-Sent Events mode.
     * <p>
     * Stream chat responses via SSE.
     *
     * @param payloadJson HTTP request body
     * @param headers     HTTP request headers
     * @return SseEmitter SSE emitter
     */
    @RequestMapping(value = "/sse/chat", method = {RequestMethod.GET})
    public SseEmitter sseChatByGet(@RequestParam("payload") String payloadJson, @RequestHeader Map<String, String> headers) throws JsonProcessingException {
        return this.sseChat(webMvcObjectMapper.readValue(payloadJson, Map.class), headers);
    }

    /**
     * SSE chat endpoint - Server-Sent Events mode.
     * <p>
     * Stream chat responses via SSE.
     *
     * @param payload HTTP request body
     * @param headers HTTP request headers
     * @return SseEmitter SSE emitter
     */
    @RequestMapping(value = "/sse/chat", method = {RequestMethod.POST})
    public SseEmitter sseChat(@RequestBody Map<String, Object> payload, @RequestHeader Map<String, String> headers) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        try {

            Map<String, String> safeHeaders = headers.entrySet().stream()
                    .filter(entry -> !RESTRICTED_HEADERS.contains(entry.getKey().toLowerCase()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            requestToPayload(payload, safeHeaders);

            // Apply interceptor
            if (funcInterceptor != null) {
                Object interceptedResponse = funcInterceptor.apply(payload);
                if (interceptedResponse != null) {
                    emitter.send(interceptedResponse);
                    emitter.complete();
                    return emitter;
                }
            }

            String currentTraceId = payload.getOrDefault("current_trace_id", "").toString();
            log.info("SSE connection established. trace_id: {}", currentTraceId);

            String redisKey = mas.getMessagePrefix() + ":" + mas.getName() + ":" + currentTraceId;

            // Execute chat asynchronously
            CompletableFuture<OxyResponse> task = CompletableFuture.supplyAsync(() -> {
                        try {
                            return mas.chatWithAgent(payload, redisKey);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
            );

            // Handle task completion
            task.whenComplete((result, throwable) -> {
                this.mas.getActiveTasks().remove(currentTraceId);
                if (throwable != null) {
                    log.error("Chat task failed", throwable);
                    emitter.completeWithError(throwable);
                }
            });
            this.mas.getActiveTasks().put(currentTraceId, task);
            // Start event stream
            CompletableFuture.runAsync(() -> {
                try {
                    eventStream(redisKey, currentTraceId, task, emitter);
                } catch (Exception e) {
                    log.error("Event stream failed", e);
                    emitter.completeWithError(e);
                }
            });

        } catch (Exception e) {
            log.error("SSE chat failed", e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * Async chat endpoint.
     * <p>
     * Return immediately; task runs in the background.
     *
     * @param payload HTTP request body
     * @param headers HTTP request headers
     * @return ResponseEntity containing confirmation WebResponse
     */
    @RequestMapping(value = "/async/chat", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> asyncChat(@RequestBody Map<String, Object> payload, @RequestHeader Map<String, String> headers) {
        try {

            requestToPayload(payload, headers);

            // Apply interceptor
            if (funcInterceptor != null) {
                Object interceptedResponse = funcInterceptor.apply(payload);
                if (interceptedResponse != null) {
                    return ResponseEntity.ok((Map<String, Object>) interceptedResponse);
                }
            }

            String currentTraceId = payload.getOrDefault("current_trace_id", "").toString();
            log.info("Async task created. trace_id: {}", currentTraceId);

            String redisKey = mas.getMessagePrefix() + ":" + mas.getName() + ":" + currentTraceId;

            // Execute chat asynchronously
            CompletableFuture<OxyResponse> task = CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            return mas.chatWithAgent(payload, redisKey);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
            );

            // Handle task completion
            task.whenComplete((result, throwable) -> {
                this.mas.getActiveTasks().remove(currentTraceId);
                if (throwable != null) {
                    log.error("Async chat task failed", throwable);
                }
            });

            this.mas.getActiveTasks().put(currentTraceId, task);

            return ResponseEntity.ok(WebResponse.success(null).toMap());
        } catch (Exception e) {
            log.error("Async chat failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(500, "Async chat failed: " + e.getMessage()).toMap());
        }
    }

    /**
     * Event stream handler.
     * <p>
     * Read messages from Redis and send via SSE.
     */
    private void eventStream(String redisKey, String currentTraceId, CompletableFuture<OxyResponse> task, SseEmitter emitter) throws Exception {
        try {
            while (true) {
                // Read messages from Redis
                Object rpop = mas.getRedisClient().brpop(redisKey);
                if (rpop == null) {
                    Thread.sleep(100);
                    continue;
                }

                // Unpack message
                Map<String, Object> msgMap = mas.unpackMessage(Base64.getDecoder().decode((String) rpop));
                if (msgMap != null) {
                    // Check for close event
                    if (msgMap.containsKey("event")) {
                        emitter.send(SseEmitter.event().data(msgMap).name((String) msgMap.get("event")));
                        log.info("SSE connection terminated. trace_id: {}", currentTraceId);
                        emitter.complete();
                        break;
                    }

                    // Handle tool_call message
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

                    // Handle observation message
                    if ("observation".equals(msgMap.get("type"))) {
                        Map<String, Object> content = (Map<String, Object>) msgMap.get("content");
                        if (content != null && content.containsKey("output")) {
                            content.put("output", CommonUtils.toJson(content.get("output")));
                        }
                    }
                }
                // Send message
                emitter.send(SseEmitter.event().data(msgMap).name("message"));
            }
        } catch (InterruptedException e) {
            log.info("SSE connection terminated. trace_id: {}", currentTraceId);
            if (this.mas.getActiveTasks().containsKey(currentTraceId)) {
                ((CompletableFuture<?>) this.mas.getActiveTasks().get(currentTraceId)).cancel(true);
            }
            emitter.complete();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Event stream processing failed", e);
            emitter.completeWithError(e);
        }
    }

    /**
     * Retrieve execution node details using node_id or trace_id.
     *
     * @param itemId Node identifier or trace identifier
     * @return ResponseEntity containing node details WebResponse
     */
    @GetMapping("/node")
    public ResponseEntity<Map<String, Object>> getNodeInfo(@RequestParam("item_id") String itemId) {
        try {
            // Search node
            Map<String, Object> query = new HashMap<>();
            query.put("query", Map.of("term", Map.of("_id", itemId)));

            Map<String, Object> esResponse = mas.getEsClient().search(com.jd.oxygent.core.Config.getAppName() + "_node", query);

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

            // Fetch all nodes
            Map<String, Object> traceQuery = Map.of(
                    "query", Map.of("term", Map.of("trace_id", traceId)),
                    "size", 10000,
                    "sort", List.of(Map.of("create_time", Map.of("order", "asc")))
            );

            esResponse = mas.getEsClient().search(com.jd.oxygent.core.Config.getAppName() + "_node", traceQuery);

            List<String> nodeIds = new ArrayList<>();
            hits = (List<Map<String, Object>>) ((Map<String, Object>) esResponse.get("hits")).get("hits");

            for (Map<String, Object> data : hits) {
                Map<String, Object> source = (Map<String, Object>) data.get("_source");
                nodeIds.add(source.getOrDefault("node_id", "").toString());
            }

            if (nodeIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(WebResponse.error(400, "Invalid node ID").toMap());
            }

            if (traceId.equals(itemId)) {
                // Re-fetch node_id data
                itemId = nodeIds.get(0);
                query = Map.of("query", Map.of("term", Map.of("_id", itemId)));
                esResponse = mas.getEsClient().search(com.jd.oxygent.core.Config.getAppName() + "_node", query);
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

                    List<Map<String, Object>> trees = Arrays.asList(classAttr, (Map<String, Object>) classAttr.getOrDefault("llm_params", new HashMap<>()), (Map<String, Object>) input.get("arguments"));

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

                            if ((v instanceof Integer || v instanceof Double ||
                                    v instanceof Float) && !(v instanceof Boolean)) {
                                double numValue = Double.parseDouble(v.toString());
                                double maxValue = numValue <= 1 ? 1 : numValue * 10;
                                dataRangeMap.put(k, Map.of(
                                        "min", 0,
                                        "max", maxValue
                                ));
                            }
                        }
                    }

                    // Transform data
                    DataUtils.changeNodeValue(nodeData);

                    return ResponseEntity.ok(WebResponse.success(nodeData).toMap());
                }
            }

            return ResponseEntity.badRequest()
                    .body(WebResponse.error(400, "Node not found").toMap());

        } catch (Exception e) {
            log.error("Failed to get node info", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(500, "Encountered an issue").toMap());
        }
    }

    /**
     * Get full view information of task execution.
     *
     * @param itemId Node identifier or trace identifier
     * @return ResponseEntity containing nodes list and trace ID WebResponse
     */
    @GetMapping("/view")
    public ResponseEntity<Map<String, Object>> getTaskInfo(@RequestParam("item_id") String itemId) {
        try {
            // Check whether node_id
            Map<String, Object> query = Map.of("query", Map.of("term", Map.of("_id", itemId)));
            Map<String, Object> esResponse = mas.getEsClient().search(com.jd.oxygent.core.Config.getAppName() + "_node", query);

            String traceId;
            List<Map<String, Object>> hits = (List<Map<String, Object>>) ((Map<String, Object>) esResponse.get("hits")).get("hits");

            if (!hits.isEmpty()) {
                Map<String, Object> nodeData = (Map<String, Object>) hits.get(0).get("_source");
                traceId = nodeData.getOrDefault("trace_id", "").toString();
            } else {
                traceId = itemId;
            }

            // Fetch all nodes
            Map<String, Object> traceQuery = Map.of(
                    "query", Map.of("term", Map.of("trace_id", traceId)),
                    "size", 10000,
                    "sort", List.of(Map.of("create_time", Map.of("order", "asc")))
            );

            esResponse = mas.getEsClient().search(com.jd.oxygent.core.Config.getAppName() + "_node", traceQuery);

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

            return ResponseEntity.ok(WebResponse.success(taskData).toMap());

        } catch (Exception e) {
            log.error("Failed to get task info", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(500, "Encountered an issue").toMap());
        }
    }

    private static final Pattern PATTERN = Pattern.compile("^\\$\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}$");

    /**
     * Call OxyGent agent based on Item request.
     *
     * @param item Request body containing class_attr and arguments
     * @return ResponseEntity containing model output WebResponse
     */
    @PostMapping("/call")
    public ResponseEntity<Map<String, Object>> call(@RequestBody ItemRequest item) {
        try {
            // Handle environment variable pattern matching
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
            return ResponseEntity.ok(WebResponse.success(data).toMap());

        } catch (Exception e) {
            log.error("Call failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(500, "Encountered an issue: " + e.getMessage()).toMap());
        }
    }

    /**
     * Upload file endpoint.
     * Accept user-uploaded file and save to server uploads directory.
     * Filename will have a timestamp prefix to avoid conflicts.
     *
     * @param file File uploaded via HTTP multipart/form-data
     * @return ResponseEntity containing the filename WebResponse
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            FileValidationUtil.validateFile(new FileItemAdapter(file));
            // Generate unique filename
            String uploadDir = Paths.get(Config.getXfile().getSaveDir(), "uploads").toString();
            Files.createDirectories(Paths.get(uploadDir));

            String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
            String fileName = timestamp + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir, fileName);

            // Save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Return file path
            Map<String, Object> data = new HashMap<>();
            data.put("file_name", fileName);
            return ResponseEntity.ok(WebResponse.success(data).toMap());
        } catch (Exception e) {
            log.error("File upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(500, "File upload failed: " + e.getMessage()).toMap());
        }
    }
}