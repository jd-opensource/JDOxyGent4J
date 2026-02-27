package com.jd.oxygent.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.uuid.UuidCreator;
import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.EvaluationManager;
import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.liveprompt.DynamicAgentManager;
import com.jd.oxygent.core.oxygent.liveprompt.PromptManager;
import com.jd.oxygent.core.oxygent.liveprompt.PromptOptimizer;
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
import com.jd.oxygent.core.oxygent.schemas.memory.Memory;
import com.jd.oxygent.core.oxygent.schemas.memory.Message;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.utils.ClassModelDumpUtils;
import com.jd.oxygent.core.oxygent.utils.CommonUtils;
import com.jd.oxygent.core.oxygent.utils.DataUtils;
import com.jd.oxygent.core.oxygent.utils.StringUtils;
import com.jd.oxygent.web.adapter.FileItemAdapter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
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
    private final EvaluationManager evaluationManager = EvaluationManager.getInstance();
    private final PromptManager promptManager = PromptManager.getInstance();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ObjectMapper webMvcObjectMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    Config config;

    /**
     * Redirect clients to the packaged web frontend.
     *
     * @return RedirectView redirect to ./web/index.html
     */
    @GetMapping("/")
    public RedirectView readRoot() {
        return new RedirectView("web.bak/index.html");
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
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to get organization").toMap());
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
            String firstQuery = mas.getFirstQuery() != null && !mas.getFirstQuery().isEmpty() ? mas.getFirstQuery() : Config.getServer().getFirstQuery();
            Map<String, Object> data = Map.of("first_query", firstQuery);
            return ResponseEntity.ok(WebResponse.success(data).toMap());
        } catch (Exception e) {
            log.error("Failed to get first query", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to get first query").toMap());
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
            String welcomeMessage = Config.getAgent().getWelcomeMessage();
            Map<String, Object> data = Map.of("welcome_message", welcomeMessage);
            return ResponseEntity.ok(WebResponse.success(data).toMap());
        } catch (Exception e) {
            log.error("Failed to get welcome message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to get welcome message").toMap());
        }
    }

    @GetMapping("/get_description")
    public ResponseEntity<Map<String, Object>> getDescription() {
        try {
            String desc = mas.getOxyByName(mas.getMasterAgentName()).getDesc();
            Map<String, Object> data = Map.of("description", desc);
            return ResponseEntity.ok(WebResponse.success(data).toMap());
        } catch (Exception e) {
            log.error("Failed to get description message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to get description message").toMap());
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
            return ResponseEntity.ok(WebResponse.success(data).toMap());

        } catch (IOException e) {
            log.error("Failed to list scripts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to list scripts").toMap());
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
            String scriptSaveDir = Paths.get(Config.getXfile().getSaveDir(), "script").toString();
            Files.createDirectories(Paths.get(scriptSaveDir));

            Path filePath = Paths.get(scriptSaveDir, script.getName() + ".json");
            String jsonContent = objectMapper.writeValueAsString(script.getContents());
            Files.write(filePath, jsonContent.getBytes());

            Map<String, Object> data = Map.of("script_id", script.getName() + ".json");
            return ResponseEntity.ok(WebResponse.success(data).toMap());

        } catch (IOException e) {
            log.error("Failed to save script", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to save script").toMap());
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
            String scriptSaveDir = Paths.get(Config.getXfile().getSaveDir(), "script").toString();
            Path jsonPath = Paths.get(scriptSaveDir, itemId + ".json");

            if (!Files.exists(jsonPath)) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "File not found").toMap());
            }

            String content = Files.readString(jsonPath);
            List<Object> contents = objectMapper.readValue(content, List.class);

            Map<String, Object> data = Map.of("contents", contents);
            return ResponseEntity.ok(WebResponse.success(data).toMap());

        } catch (IOException e) {
            log.error("Failed to load script", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to load script").toMap());
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
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error requesting groupid").toMap());
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
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error requesting traceid").toMap());
        }
    }

    /**
     * Convert request into payload.
     */
    private Map<String, Object> requestToPayload(Map<String, Object> payload, Map<String, String> headers) throws Exception {

        // Apply filter
        if (mas.getFuncFilter()!= null) {
            payload = mas.getFuncFilter().apply(payload);
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
            if (mas.getFuncInterceptor() != null) {
                Object interceptedResponse = mas.getFuncInterceptor().apply(payload);
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
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Chat failed: " + e.getMessage()).toMap());
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
            if (mas.getFuncInterceptor()!= null) {
                Object interceptedResponse = mas.getFuncInterceptor().apply(payload);
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
                    processRedisMessage(redisKey, currentTraceId, task, emitter);
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

    @RequestMapping(value = "/async/chat", method = {RequestMethod.GET})
    public ResponseEntity<Map<String, Object>> asyncByGet(@RequestParam("payload") String payloadJson, @RequestHeader Map<String, String> headers) throws JsonProcessingException {
        return this.asyncChat(webMvcObjectMapper.readValue(payloadJson, Map.class), headers);
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
    @RequestMapping(value = "/async/chat", method = {RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> asyncChat(@RequestBody Map<String, Object> payload, @RequestHeader Map<String, String> headers) {
        try {
            requestToPayload(payload, headers);
            // Apply interceptor
            if (mas.getFuncInterceptor() != null) {
                Object interceptedResponse = mas.getFuncInterceptor().apply(payload);
                if (interceptedResponse != null) {
                    return ResponseEntity.ok((Map<String, Object>) interceptedResponse);
                }
            }
            String currentTraceIdTemp = payload.getOrDefault("current_trace_id", "").toString();
            if (StringUtils.isBlank(currentTraceIdTemp)) {
                currentTraceIdTemp = CommonUtils.generateShortUUID();
            }
            String currentTraceId = currentTraceIdTemp;
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
            return ResponseEntity.ok(WebResponse.success(currentTraceId).toMap());
        } catch (Exception e) {
            log.error("Async chat failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Async chat failed: " + e.getMessage()).toMap());
        }
    }

    @RequestMapping(value = "/async/trace", method = {RequestMethod.GET, RequestMethod.POST})
    public SseEmitter asyncTrace(@RequestParam("trace_id") String currentTraceId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        try {
            log.info("asyncTrace trace_id: {}", currentTraceId);
            String redisKey = mas.getMessagePrefix() + ":" + mas.getName() + ":" + currentTraceId;
            CompletableFuture<OxyResponse> task = (CompletableFuture<OxyResponse>) this.mas.getActiveTasks().get(currentTraceId);
            // Start event stream
            CompletableFuture.runAsync(() -> {
                try {
                    processRedisMessage(redisKey, currentTraceId, task, emitter);
                } catch (Exception e) {
                    log.error("Event stream failed", e);
                    emitter.completeWithError(e);
                }
            });
        } catch (Exception e) {
            log.error("asyncTrace failed", e);
            emitter.completeWithError(e);
        }
        return emitter;
    }

    /**
     * Event stream handler.
     * <p>
     * Read messages from Redis and send via SSE.
     */
    private void processRedisMessage(String redisKey, String currentTraceId, CompletableFuture<OxyResponse> task, SseEmitter emitter) throws Exception {
        try {
            while (true) {
                // Read messages from Redis
                Object rpop = mas.getRedisClient().brpop(redisKey);
                if (rpop == null) {
                    Thread.sleep(100);
                    continue;
                }

                // Unpack message
                SSEMessage<Map<String, Object>> sseMessage = mas.unpackMessage(Base64.getDecoder().decode((String) rpop));
                if (sseMessage != null && sseMessage.getData() != null) {
                    Map<String, Object> msgMap = sseMessage.getData();
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
                    emitter.send(SseEmitter.event().data(msgMap).name(sseMessage.getEvent()));
                    // Check for close event
                    if ("close".equals(sseMessage.getEvent())) {
                        log.info("SSE connection terminated. trace_id: {}", currentTraceId);
                        emitter.complete();
                        break;
                    }
                }
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
                        .body(WebResponse.error(HttpStatus.BAD_REQUEST.value(), "Invalid node ID").toMap());
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
                                    v instanceof Double) && !(v instanceof Boolean)) {
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
                    .body(WebResponse.error(HttpStatus.BAD_REQUEST.value(), "Node not found").toMap());

        } catch (Exception e) {
            log.error("Failed to get node info", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Encountered an issue").toMap());
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
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Encountered an issue").toMap());
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
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Encountered an issue: " + e.getMessage()).toMap());
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
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "File upload failed: " + e.getMessage()).toMap());
        }
    }

    /**
     * feedback
     *
     * @return
     */
    @PostMapping("/feedback")
    public ResponseEntity<Map<String, Object>> feedback(@RequestBody Map<String, String> payload) {
        String channelId = payload.getOrDefault("channel_id", "");
        if (!mas.feedbackDict.containsKey(channelId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(WebResponse.error(HttpStatus.BAD_REQUEST.value(), "illegal channel_id: " + channelId).toMap());
        }
        LinkedBlockingQueue<String> feedbackQueue = mas.feedbackDict.get(channelId);
        String data = payload.getOrDefault("data", "");
        if (feedbackQueue == null) {
            feedbackQueue = new LinkedBlockingQueue<>();
            mas.feedbackDict.put(channelId, feedbackQueue);
        }
        try {
            feedbackQueue.put(data);
            feedbackQueue.put("");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(WebResponse.success("success").toMap());
    }

    /**
     * Get detailed agent information for frontend display.
     *
     * @return Mas.feedbackDict.get(channelId)
     */
    @PostMapping("/get_agents")
    public ResponseEntity<Map<String, Object>> getAgents(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(WebResponse.success(mas.getAgentOrganization()).toMap());
    }

    /**
     * Handle rating submission
     */
    @PostMapping("/rating")
    public ResponseEntity<Map<String, Object>> rating(@RequestBody Map<String, Object> requestBody, HttpServletRequest request) {
        try {
            RatingRequest ratingRequest = objectMapper.convertValue(requestBody, RatingRequest.class);
            RatingResponse response = evaluationManager.createRating(ratingRequest, request, null);
            return ResponseEntity.ok(WebResponse.success(response).toMap());
        } catch (Exception e) {
            log.error("Rating failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Rating failed: " + e.getMessage()).toMap());
        }
    }

    /**
     * Get conversation history with ratings
     */
    @GetMapping("/history_with_ratings")
    public ResponseEntity<Map<String, Object>> historyWithRatings(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(value = "rating_filter", defaultValue = "all") String ratingFilter,
            @RequestParam(value = "search_term", defaultValue = "") String searchTerm) {
        try {
            Map<String, Object> conversations = evaluationManager.historyWithRatings(ratingFilter, searchTerm, page, pageSize);
            return ResponseEntity.ok(WebResponse.success(conversations).toMap());
        } catch (Exception e) {
            log.error("Get history with ratings failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to get history with ratings: " + e.getMessage()).toMap());
        }
    }

    /**
     * Get rating analytics
     */
    @GetMapping("/analytics/ratings")
    public ResponseEntity<Map<String, Object>> analyticsRatings(
            @RequestParam(value = "days", defaultValue = "7") int days) {
        try {
            Map<String, Object> stats = evaluationManager.getOverallRatingStats(days);
            return ResponseEntity.ok(WebResponse.success(stats).toMap());
        } catch (Exception e) {
            log.error("Get rating analytics failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to get rating analytics: " + e.getMessage()).toMap());
        }
    }

    /**
     * Clear all ratings
     */
    @PostMapping("/rating/clear_all")
    public ResponseEntity<Map<String, Object>> clearAllRatings() {
        try {
            Map<String, Object> result = evaluationManager.clearAllRatingData();
            return ResponseEntity.ok(WebResponse.success(result).toMap());
        } catch (Exception e) {
            log.error("Clear all ratings failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to clear all ratings: " + e.getMessage()).toMap());
        }
    }

    /**
     * Setup rating indices
     */
    @PostMapping("/rating/setup_indices")
    public ResponseEntity<Map<String, Object>> setupRatingIndices() {
        try {
            Map<String, Object> result = evaluationManager.ensureRatingIndicesWithCorrectMapping();
            return ResponseEntity.ok(WebResponse.success(result).toMap());
        } catch (Exception e) {
            log.error("Setup rating indices failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to setup rating indices: " + e.getMessage()).toMap());
        }
    }

    /**
     * Rebuild rating stats for a specific trace
     */
    @PostMapping("/rating/{trace_id}/rebuild_stats")
    public ResponseEntity<Map<String, Object>> rebuildStats(@PathVariable("trace_id") String traceId) {
        try {
            RatingStats stats = evaluationManager.updateRatingStats(traceId, null);
            return ResponseEntity.ok(WebResponse.success(stats).toMap());
        } catch (Exception e) {
            log.error("Rebuild stats failed for trace: " + traceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to rebuild stats: " + e.getMessage()).toMap());
        }
    }

    /**
     * Get current rating for a trace
     */
    @GetMapping("/rating/{trace_id}/current")
    public ResponseEntity<Map<String, Object>> getCurrentRating(@PathVariable("trace_id") String traceId) {
        try {
            Optional<RatingStats> statsOpt = evaluationManager.getRatingStats(traceId);
            return ResponseEntity.ok(WebResponse.success(statsOpt.orElse(null)).toMap());
        } catch (Exception e) {
            log.error("Get current rating failed for trace: " + traceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to get current rating: " + e.getMessage()).toMap());
        }
    }

    /**
     * Get rating history for a trace
     */
    @GetMapping("/rating/{trace_id}/history")
    public ResponseEntity<Map<String, Object>> getRatingHistory(@PathVariable("trace_id") String traceId) {
        try {
            List<ConversationRating> ratings = evaluationManager.getRatingHistory(traceId);
            return ResponseEntity.ok(WebResponse.success(ratings).toMap());
        } catch (Exception e) {
            log.error("Get rating history failed for trace: " + traceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to get rating history: " + e.getMessage()).toMap());
        }
    }

    /**
     * Delete rating for a trace
     */
    @DeleteMapping("/rating/{trace_id}")
    public ResponseEntity<Map<String, Object>> deleteRating(@PathVariable("trace_id") String traceId) {
        try {
            evaluationManager.deleteRating(traceId);
            return ResponseEntity.ok(WebResponse.success("Successfully deleted rating").toMap());
        } catch (Exception e) {
            log.error("Delete rating failed for trace: " + traceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to delete rating: " + e.getMessage()).toMap());
        }
    }

    // Debug API Endpoints

    /**
     * Debug endpoint: Check rating statistics storage for specific trace_id.
     */
    @GetMapping("/debug/rating_stats/{trace_id}")
    public ResponseEntity<Map<String, Object>> debugRatingStats(@PathVariable("trace_id") String traceId) {
        try {
            Optional<RatingStats> stats = evaluationManager.getRatingStats(traceId);
            Map<String, Object> data = Map.of(
                    "trace_id", traceId,
                    "stats", stats.isPresent() ? objectMapper.convertValue(stats, Map.class) : null,
                    "found", stats.isPresent()
            );
            return ResponseEntity.ok(WebResponse.success(data).toMap());
        } catch (Exception e) {
            log.error("Debug rating stats error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Debug query failed: " + e.getMessage()).toMap());
        }
    }

    /**
     * Debug endpoint: Query complete information for specified trace_id.
     */
    @GetMapping("/debug/trace/{trace_id}")
    public ResponseEntity<Map<String, Object>> debugTraceInfo(@PathVariable("trace_id") String traceId) {
        try {
            // Query trace information
            Map<String, Object> query = Map.of(
                    "query", Map.of("term", Map.of("trace_id", traceId)),
                    "size", 1
            );

            Map<String, Object> traceResponse = mas.getEsClient().search(
                    Config.getAppName() + "_trace",
                    query
            );

            Map<String, Object> traceInfo = null;
            List<Map<String, Object>> hits = (List<Map<String, Object>>) ((Map<String, Object>) traceResponse.get("hits")).get("hits");
            if (!hits.isEmpty()) {
                traceInfo = (Map<String, Object>) hits.get(0).get("_source");
            }

            Map<String, Object> data = Map.of(
                    "trace_id", traceId,
                    "trace_info", traceInfo,
                    "found", traceInfo != null
            );
            return ResponseEntity.ok(WebResponse.success(data).toMap());
        } catch (Exception e) {
            log.error("Debug trace info error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Query failed: " + e.getMessage()).toMap());
        }
    }

    // Prompt Management API Endpoints

    /**
     * List prompts
     */
    @GetMapping("/api/prompts/")
    public ResponseEntity<Map<String, Object>> listPrompts(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "agent_type", required = false) String agentType,
            @RequestParam(value = "is_active", required = false) Boolean isActive,
            @RequestParam(value = "tags", required = false) String tagsStr) {
        try {
            List<String> tags = tagsStr != null ? Arrays.asList(tagsStr.split(",")) : null;
            List<Map<String, Object>> prompts = promptManager.listPrompts(category, agentType, isActive, tags);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Successfully retrieved prompt list");
            responseData.put("data", prompts);
            
            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            log.error("List prompts failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to list prompts: " + e.getMessage()).toMap());
        }
    }

    /**
     * Optimize a prompt using AI-powered analysis.
     * This endpoint analyzes the current prompt and provides an improved version
     * based on the specified optimization strategy and framework constraints.
     */
    @PostMapping("/api/prompts/optimize")
    public ResponseEntity<Map<String, Object>> optimizePrompt(@RequestBody Map<String, Object> requestBody) {
        try {
            String promptKey = (String) requestBody.get("prompt_key");
            String agentType = (String) requestBody.getOrDefault("agent_type", "general");
            String optimizationStrategy = (String) requestBody.getOrDefault("optimization_strategy", "comprehensive");
            String customRequirements = (String) requestBody.getOrDefault("custom_requirements", "");
            Boolean autoApply = (Boolean) requestBody.getOrDefault("auto_apply", false);
            String llmModel = (String) requestBody.get("llm_model");

            // Get current prompt
            Map<String, Object> currentPromptData = promptManager.getPrompt(promptKey, true);
            if (currentPromptData == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(WebResponse.error(HttpStatus.NOT_FOUND.value(), "Prompt not found").toMap());
            }

            String currentPromptContent = (String) currentPromptData.get("prompt_content");

            // Optimize prompt using PromptOptimizer
            Map<String, Object> optimizationResult = PromptOptimizer.getInstance(mas, llmModel).optimize(currentPromptContent, agentType, optimizationStrategy, customRequirements, null);

            if (optimizationResult.get("error") != null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Optimization failed: " + optimizationResult.get("error")).toMap());
            } else if (autoApply && optimizationResult.containsKey("optimized_prompt")) {
                Map<String, Object> updateData = new HashMap<>(currentPromptData);
                updateData.put("prompt_content", optimizationResult.get("optimized_prompt"));
                boolean saveSuccess = promptManager.savePrompt(
                        promptKey,
                        (String) updateData.get("prompt_content"),
                        (String) updateData.get("description"),
                        (String) updateData.get("category"),
                        (String) updateData.get("agent_type"),
                        1,
                        (Boolean) updateData.get("is_active"),
                        (List<String>) updateData.get("tags"),
                        (String) updateData.get("created_by")
                );
                if (saveSuccess) {
                    DynamicAgentManager.hotReloadPrompt(promptKey);
                    optimizationResult.put("auto_applied", true);
                    optimizationResult.put("new_version", Integer.parseInt(currentPromptData.getOrDefault("version", 1).toString()) + 1);
                    Map<String, Object> responseData = Map.of(
                            "success", true,
                            "message", "Successfully optimized prompt",
                            "data", optimizationResult
                    );
                } else {
                    optimizationResult.put("auto_applied", false);
                    optimizationResult.put("save_error", "Failed to save optimized prompt");
                    Map<String, Object> responseData = Map.of(
                            "success", true,
                            "message", "Successfully optimized prompt",
                            "data", optimizationResult
                    );
                }
            } else {
                optimizationResult.put("auto_applied", false);
            }
            return ResponseEntity.ok(Map.of("success", true,
                    "message", "Successfully optimized prompt",
                    "data", optimizationResult));
        } catch (Exception e) {
            log.error("Optimize prompt failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Optimization failed: " + e.getMessage()).toMap());
        }
    }

    /**
     * Create prompt
     */
    @PostMapping("/api/prompts/")
    public ResponseEntity<Map<String, Object>> createPrompt(@RequestBody Map<String, Object> requestBody) {
        try {
            String promptKey = (String) requestBody.get("prompt_key");
            String promptContent = (String) requestBody.get("prompt_content");
            String description = (String) requestBody.getOrDefault("description", "");
            String category = (String) requestBody.getOrDefault("category", "custom");
            String agentType = (String) requestBody.getOrDefault("agent_type", "");
            boolean isActive = (boolean) requestBody.getOrDefault("is_active", true);
            List<String> tags = (List<String>) requestBody.getOrDefault("tags", new ArrayList<>());
            String createdBy = (String) requestBody.getOrDefault("created_by", "user");
            
            // Check if prompt already exists
            Map<String, Object> existing = promptManager.getPrompt(promptKey, true);
            if (existing != null) {
                return ResponseEntity.badRequest()
                        .body(WebResponse.error(HttpStatus.BAD_REQUEST.value(), "Prompt already exists").toMap());
            }
            
            boolean success = promptManager.savePrompt(promptKey, promptContent, description, category, agentType,
                    1, isActive, tags, createdBy);
            
            if (!success) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to create prompt").toMap());
            }
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Successfully created prompt");
            responseData.put("data", Map.of("prompt_key", promptKey));
            
            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            log.error("Create prompt failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to create prompt: " + e.getMessage()).toMap());
        }
    }

    /**
     * Search prompts
     */
    @GetMapping("/api/prompts/search/")
    public ResponseEntity<Map<String, Object>> searchPrompts(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "category", required = false) String category) {
        try {
            if (keyword == null || keyword.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(WebResponse.error(HttpStatus.BAD_REQUEST.value(), "Keyword is required").toMap());
            }
            
            List<Map<String, Object>> results = promptManager.searchPrompts(keyword, category);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Successfully searched prompts");
            responseData.put("data", results);
            
            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            log.error("Search prompts failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to search prompts: " + e.getMessage()).toMap());
        }
    }

    /**
     * Hot reload all prompts
     */
    @PostMapping("/api/prompts/hot-reload/all")
    public ResponseEntity<Map<String, Object>> hotReloadAllPrompts() {
        try {
            boolean success = DynamicAgentManager.hotReloadAllPrompts();
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", success);
            responseData.put("message", "Successfully completed batch hot reload");
            responseData.put("data", Map.of(
                    "reload_success", success,
                    "reload_time", LocalDateTime.now().format(DATE_TIME_FORMATTER)
            ));
            
            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            log.error("Hot reload all prompts failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to hot reload all prompts: " + e.getMessage()).toMap());
        }
    }

    /**
     * Hot reload agent prompts
     */
    @PostMapping("/api/prompts/hot-reload/agent/{agent_name}")
    public ResponseEntity<Map<String, Object>> hotReloadAgentPrompts(@PathVariable("agent_name") String agentName) {
        try {
            boolean success = DynamicAgentManager.hotReloadAgent(agentName);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", success);
            responseData.put("message", "Successfully hot reloaded agent prompt");
            responseData.put("data", Map.of(
                    "agent_name", agentName,
                    "hot_reload_success", success
            ));
            
            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            log.error("Hot reload agent prompt failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to hot reload agent prompt: " + e.getMessage()).toMap());
        }
    }

    /**
     * Hot reload specific prompt
     */
    @PostMapping("/api/prompts/hot-reload/{prompt_key}")
    public ResponseEntity<Map<String, Object>> hotReloadPrompt(@PathVariable("prompt_key") String promptKey) {
        try {
            boolean success = DynamicAgentManager.hotReloadPrompt(promptKey);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", success);
            responseData.put("message", "Successfully hot reloaded prompt");
            responseData.put("data", Map.of(
                    "prompt_key", promptKey,
                    "hot_reload_success", success
            ));
            
            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            log.error("Hot reload prompt failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to hot reload prompt: " + e.getMessage()).toMap());
        }
    }

    /**
     * Get prompt
     */
    @GetMapping("/api/prompts/{prompt_key}")
    public ResponseEntity<Map<String, Object>> getPrompt(@PathVariable("prompt_key") String promptKey) {
        try {
            Map<String, Object> prompt = promptManager.getPrompt(promptKey, true);
            
            if (prompt == null) {
                return ResponseEntity.notFound().build();
            }
            
            prompt.put("id", promptKey);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Successfully retrieved prompt");
            responseData.put("data", prompt);
            
            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            log.error("Get prompt failed: " + promptKey, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to get prompt: " + e.getMessage()).toMap());
        }
    }

    /**
     * Update prompt
     */
    @PutMapping("/api/prompts/{prompt_key}")
    public ResponseEntity<Map<String, Object>> updatePrompt(@PathVariable("prompt_key") String promptKey,
                                                           @RequestBody Map<String, Object> requestBody) {
        try {
            // Get existing prompt
            Map<String, Object> existing = promptManager.getPrompt(promptKey, true);
            if (existing == null) {
                return ResponseEntity.notFound().build();
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
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", false);
                responseData.put("message", "No changes detected; update the prompt before saving.");
                responseData.put("data", Map.of("prompt_key", promptKey));
                return ResponseEntity.ok(responseData);
            }
            
            // Update prompt
            boolean success = promptManager.savePrompt(promptKey, promptContent, description, category, agentType,
                    1, isActive, tags, (String) existing.getOrDefault("created_by", "user"));
            
            if (!success) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to update prompt").toMap());
            }
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Successfully updated prompt");
            responseData.put("data", Map.of("prompt_key", promptKey));
            
            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            log.error("Update prompt failed: " + promptKey, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to update prompt: " + e.getMessage()).toMap());
        }
    }

    /**
     * Delete prompt
     */
    @DeleteMapping("/api/prompts/{prompt_key}")
    public ResponseEntity<Map<String, Object>> deletePrompt(@PathVariable("prompt_key") String promptKey) {
        try {
            boolean success = promptManager.deletePrompt(promptKey);
            
            if (!success) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Successfully deleted prompt");
            responseData.put("data", Map.of("prompt_key", promptKey));
            
            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            log.error("Delete prompt failed: " + promptKey, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to delete prompt: " + e.getMessage()).toMap());
        }
    }

    /**
     * Get prompt version history
     */
    @GetMapping("/api/prompts/{prompt_key}/history")
    public ResponseEntity<Map<String, Object>> getPromptHistory(@PathVariable("prompt_key") String promptKey) {
        try {
            List<Map<String, Object>> history = promptManager.getPromptHistory(promptKey);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Successfully retrieved prompt history");
            responseData.put("data", history);
            
            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            log.error("Get prompt history failed: " + promptKey, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to get prompt history: " + e.getMessage()).toMap());
        }
    }

    /**
     * Get specific version of a prompt
     */
    @GetMapping("/api/prompts/{prompt_key}/version/{version}")
    public ResponseEntity<Map<String, Object>> getPromptVersion(@PathVariable("prompt_key") String promptKey, @PathVariable("version") Integer version) {
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
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(WebResponse.error(HttpStatus.NOT_FOUND.value(), "Version " + version + " not found for prompt " + promptKey).toMap());
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Successfully retrieved prompt history");
            responseData.put("data", targetVersion);

            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            log.error("Get prompt history failed: " + promptKey, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to get prompt history: " + e.getMessage()).toMap());
        }
    }

    /**
     * Revert prompt to specific version
     */
    @PostMapping("/api/prompts/{prompt_key}/revert/{target_version}")
    public ResponseEntity<Map<String, Object>> revertPromptToVersion(@PathVariable("prompt_key") String promptKey, @PathVariable("target_version") Integer targetVersion) {
        try {
            // Check if prompt exists
            Map<String, Object> existing = promptManager.getPrompt(promptKey, false);
            if (existing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(WebResponse.error(HttpStatus.NOT_FOUND.value(), "Prompt not found").toMap());
            }

            // Revert to target version
            boolean success = promptManager.revertToVersion(promptKey, targetVersion);

            if (!success) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(WebResponse.error(HttpStatus.BAD_REQUEST.value(), "Failed to revert to version " + targetVersion).toMap());
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
            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            log.error("Get prompt history failed: " + promptKey, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to get prompt history: " + e.getMessage()).toMap());
        }
    }
}