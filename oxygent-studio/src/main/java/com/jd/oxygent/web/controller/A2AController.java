package com.jd.oxygent.web.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.MasFactoryBean;
import com.jd.oxygent.core.oxygent.schemas.SSEMessage;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.transport.a2a.A2AInMemoryStore;
import com.jd.oxygent.core.oxygent.transport.a2a.A2AMapper;
import com.jd.oxygent.core.oxygent.transport.a2a.A2ACard;
import com.jd.oxygent.core.oxygent.transport.a2a.A2AProtocol;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * A2A (Agent-to-Agent) Protocol Controller.
 *
 * <p>Uses the official A2A Java SDK ({@code io.github.a2asdk:0.3.3.Final}) for
 * type definitions (Task, Message, AgentCard, TaskState, etc.).</p>
 */
@Slf4j
@RestController
@RequestMapping("${oxygent.server.a2a-base-path:/a2a}")
@ConditionalOnProperty(name = "oxygent.server.enable-a2a-server", havingValue = "true")
public class A2AController {

    private static final ObjectMapper A2A_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired
    private MasFactoryBean masFactoryBean;

    private volatile Mas mas;
    private final A2AInMemoryStore store = new A2AInMemoryStore();
    private String a2aBasePath;

    @PostConstruct
    public void init() {
        this.a2aBasePath = Config.getServer().getA2aBasePath();
        log.info("A2AController registered, base_path={}, MAS will be bound on first request", a2aBasePath);
    }

    private Mas getMas() {
        if (mas == null) {
            synchronized (this) {
                if (mas == null) {
                    mas = masFactoryBean.getObject();
                    log.info("A2AController bound to MAS");
                }
            }
        }
        return mas;
    }

    @GetMapping({"/.well-known/agent.json", "/.well-known/agent-card.json"})
    public void getAgentCard(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        Object card = A2ACard.buildAgentCard(baseUrl, a2aBasePath, "0.1.0", getMas());
        writeJson(response, 200, card);
    }

    @PostMapping(value = {"", "/"})
    public Object unifiedPost(@RequestBody Map<String, Object> payload, HttpServletResponse response) throws IOException {
        if (payload == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "payload must be a JSON object"));
        }

        String method = payload.get("method") instanceof String m ? m : null;
        Object reqId = payload.get("id");

        if (method != null && !method.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> params = payload.get("params") instanceof Map<?, ?> p
                    ? (Map<String, Object>) p : new HashMap<>();

            try {
                switch (method) {
                    case "message/send", "SendMessage" -> {
                        writeJson(response, 200, A2AProtocol.rpcOk(reqId, runSendMessage(params)));
                    }
                    case "message/stream", "SendMessageStream" -> {
                        runStreamMessage(response, params, reqId);
                    }
                    case "tasks/get", "GetTask" -> {
                        String taskId = extractTaskId(params);
                        writeJson(response, 200, A2AProtocol.rpcOk(reqId, runGetTask(taskId)));
                    }
                    case "tasks/cancel", "CancelTask" -> {
                        String taskId = extractTaskId(params);
                        writeJson(response, 200, A2AProtocol.rpcOk(reqId, runCancelTask(taskId)));
                    }
                    default -> writeJson(response, 200, A2AProtocol.rpcError(reqId, -32601, "method `" + method + "` not found"));
                }
                return null;
            } catch (Exception e) {
                log.error("A2A JSON-RPC error method={}", method, e);
                writeJson(response, 200, A2AProtocol.rpcError(reqId, -32000, e.getMessage()));
                return null;
            }
        }

        // Plain POST fallback
        String action = resolvePlainAction(payload);
        try {
            switch (action) {
                case "send_message" -> writeJson(response, 200, Map.of("task", runSendMessage(payload)));
                case "get_task" -> writeJson(response, 200, Map.of("task", runGetTask(extractTaskId(payload))));
                case "cancel_task" -> writeJson(response, 200, Map.of("task", runCancelTask(extractTaskId(payload))));
                default -> {
                    return ResponseEntity.badRequest().body(Map.of("error",
                            "unsupported action. use action in send_message/get_task/cancel_task or JSON-RPC method field"));
                }
            }
            return null;
        } catch (Exception e) {
            log.error("A2A plain POST error", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = {"/messages/send", "/messages/send/"})
    public void sendMessage(@RequestBody Map<String, Object> payload, HttpServletResponse response) throws IOException {
        try {
            writeJson(response, 200, Map.of("task", runSendMessage(payload)));
        } catch (Exception e) {
            log.error("A2A message/send error", e);
            writeJson(response, 500, Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = {"/tasks/get", "/tasks/get/"})
    public void getTask(@RequestBody Map<String, Object> payload, HttpServletResponse response) throws IOException {
        try {
            String taskId = extractTaskId(payload);
            writeJson(response, 200, Map.of("task", runGetTask(taskId)));
        } catch (Exception e) {
            writeJson(response, 404, Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = {"/tasks/cancel", "/tasks/cancel/"})
    public void cancelTask(@RequestBody Map<String, Object> payload, HttpServletResponse response) throws IOException {
        try {
            String taskId = extractTaskId(payload);
            writeJson(response, 200, Map.of("task", runCancelTask(taskId)));
        } catch (Exception e) {
            writeJson(response, 404, Map.of("error", e.getMessage()));
        }
    }

    // ==================== Core Logic ====================

    private Object runSendMessage(Map<String, Object> payload) throws Exception {
        Map<String, Object> msgPayload = A2AMapper.normalizeMessagePayload(payload);
        String text = A2AMapper.extractText(msgPayload);
        Map<String, Object> metadata = A2AMapper.extractMetadata(payload);
        String[] ids = A2AMapper.extractContextAndTask(payload, msgPayload);
        String contextId = ids[0];
        String taskId = ids[1];
        List<String> referenceTaskIds = A2AMapper.extractReferenceTaskIds(payload, msgPayload);

        log.info("A2A message/send context_id={}, task_id={}", contextId, taskId);

        if (!store.tryMarkRunning(taskId)) {
            Task existingTask = store.getTask(taskId);
            if (existingTask != null) return existingTask;
            return store.buildTask(taskId, contextId, "Task is running.", taskId, contextId, TaskState.WORKING);
        }

        store.buildTask(taskId, contextId, "Task is pending.", taskId, contextId, TaskState.SUBMITTED);
        store.buildTask(taskId, contextId, "Task is running.", taskId, contextId, TaskState.WORKING);

        try {
            String target = A2ACard.effectiveTarget(getMas(), null);
            Map<String, Object> masPayload = A2AMapper.buildMasPayload(
                    text, contextId, taskId, target,
                    referenceTaskIds, metadata, store.contextSession(contextId)
            );
            if (masPayload == null) {
                return store.buildTask(taskId, contextId, text, taskId, contextId, TaskState.COMPLETED);
            }

            OxyResponse oxyResponse = getMas().chatWithAgent(masPayload);
            String traceId = oxyResponse.getOxyRequest() != null ? oxyResponse.getOxyRequest().getCurrentTraceId() : taskId;
            String groupId = oxyResponse.getOxyRequest() != null ? oxyResponse.getOxyRequest().getGroupId() : contextId;
            String answer = oxyResponse.getOutputAsString();

            store.saveContext(contextId, groupId, traceId, taskId);
            return store.buildTask(taskId, contextId, answer, traceId, groupId, TaskState.COMPLETED);
        } catch (Exception e) {
            store.buildTask(taskId, contextId, "Task failed: " + e.getMessage(), taskId, contextId, TaskState.FAILED, e.getMessage());
            throw e;
        } finally {
            store.unmarkRunning(taskId);
        }
    }

    @SuppressWarnings("unchecked")
    private void runStreamMessage(HttpServletResponse response, Map<String, Object> payload, Object reqId) throws IOException {
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        Map<String, Object> msgPayload = A2AMapper.normalizeMessagePayload(payload);
        String text = A2AMapper.extractText(msgPayload);
        Map<String, Object> metadata = A2AMapper.extractMetadata(payload);
        String[] ids = A2AMapper.extractContextAndTask(payload, msgPayload);
        String contextId = ids[0];
        String taskId = ids[1];
        List<String> referenceTaskIds = A2AMapper.extractReferenceTaskIds(payload, msgPayload);

        log.info("A2A message/stream context_id={}, task_id={}", contextId, taskId);

        if (!store.tryMarkRunning(taskId)) {
            Object event = A2AProtocol.buildAgentMessage("Task is already running.", taskId, contextId);
            writeSseEvent(response, A2AProtocol.rpcOk(reqId, event));
            return;
        }

        String target = A2ACard.effectiveTarget(getMas(), null);
        Map<String, Object> masPayload = A2AMapper.buildMasPayload(
                text, contextId, taskId, target,
                referenceTaskIds, metadata, store.contextSession(contextId)
        );

        if (masPayload == null) {
            store.buildTask(taskId, contextId, text, taskId, contextId, TaskState.COMPLETED);
            store.unmarkRunning(taskId);
            Object event = A2AProtocol.buildAgentMessage(text, taskId, contextId);
            writeSseEvent(response, A2AProtocol.rpcOk(reqId, event));
            return;
        }

        store.buildTask(taskId, contextId, "Task is pending.", taskId, contextId, TaskState.SUBMITTED);
        store.buildTask(taskId, contextId, "Task is running.", taskId, contextId, TaskState.WORKING);

        Mas masRef = getMas();
        String redisKey = masRef.getMessagePrefix() + ":" + masRef.getName() + ":" + taskId;

        CompletableFuture<OxyResponse> masTask = CompletableFuture.supplyAsync(() -> {
            try {
                return masRef.chatWithAgent(masPayload, redisKey);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        StringBuilder emitted = new StringBuilder();
        try {
            while (true) {
                Object rpop = masRef.getRedisClient().brpop(redisKey);
                if (rpop == null) {
                    if (masTask.isDone()) break;
                    Thread.sleep(50);
                    continue;
                }

                SSEMessage<Map<String, Object>> sseMessage = masRef.unpackMessage(Base64.getDecoder().decode((String) rpop));
                if (sseMessage == null || sseMessage.getData() == null) continue;

                Map<String, Object> msgMap = sseMessage.getData();
                if (msgMap.containsKey("event") && "close".equals(msgMap.get("event"))) break;

                String delta = A2AMapper.extractDeltaFromSseData(msgMap, true);
                if (delta == null || delta.isEmpty()) continue;

                emitted.append(delta);
                Object event = A2AProtocol.buildAgentMessage(emitted.toString(), taskId, contextId);
                writeSseEvent(response, A2AProtocol.rpcOk(reqId, event));
            }

            OxyResponse oxyResponse = masTask.get();
            String traceId = oxyResponse.getOxyRequest() != null ? oxyResponse.getOxyRequest().getCurrentTraceId() : taskId;
            String groupId = oxyResponse.getOxyRequest() != null ? oxyResponse.getOxyRequest().getGroupId() : contextId;
            String finalAnswer = emitted.length() > 0 ? emitted.toString() : oxyResponse.getOutputAsString();

            if (emitted.length() == 0) {
                Object event = A2AProtocol.buildAgentMessage(finalAnswer, taskId, contextId);
                writeSseEvent(response, A2AProtocol.rpcOk(reqId, event));
            }

            store.buildTask(taskId, contextId, finalAnswer, traceId, groupId, TaskState.COMPLETED);
            store.saveContext(contextId, groupId, traceId, taskId);
            log.info("A2A message/stream completed task_id={}, context_id={}", taskId, contextId);
        } catch (Exception e) {
            store.buildTask(taskId, contextId, "Task failed: " + e.getMessage(), taskId, contextId, TaskState.FAILED, e.getMessage());
            log.error("A2A stream failed task_id={}", taskId, e);
        } finally {
            store.unmarkRunning(taskId);
        }
    }

    private Object runGetTask(String taskId) throws Exception {
        if (taskId == null || taskId.isEmpty()) {
            throw new IllegalArgumentException("task id is required");
        }
        Task task = store.getTask(taskId);
        if (task == null) {
            throw new RuntimeException("task not found");
        }
        return task;
    }

    private Object runCancelTask(String taskId) throws Exception {
        runGetTask(taskId);
        Task existing = store.getTask(taskId);
        String contextId = existing != null && existing.getContextId() != null ? existing.getContextId() : "";
        store.buildTask(taskId, contextId, "Task canceled.", taskId, contextId, TaskState.CANCELED);
        store.unmarkRunning(taskId);
        log.info("A2A task canceled task_id={}", taskId);
        return store.getTask(taskId);
    }

    // ==================== Helpers ====================

    private void writeJson(HttpServletResponse response, int status, Object data) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(A2A_MAPPER.writeValueAsString(data));
        response.getWriter().flush();
    }

    private void writeSseEvent(HttpServletResponse response, Object data) throws IOException {
        response.getWriter().write("data: " + A2A_MAPPER.writeValueAsString(data) + "\n\n");
        response.getWriter().flush();
    }

    private String extractTaskId(Map<String, Object> params) {
        Object id = params.get("id");
        if (id == null) id = params.get("taskId");
        return id != null ? id.toString() : "";
    }

    private String resolvePlainAction(Map<String, Object> payload) {
        for (String key : List.of("action", "op", "operation")) {
            Object val = payload.get(key);
            if (val instanceof String s && !s.isEmpty()) {
                return normalizeAction(s.toLowerCase());
            }
        }
        if (payload.containsKey("message") || payload.containsKey("query")) return "send_message";
        if (payload.containsKey("taskId") || payload.containsKey("id")) return "get_task";
        return "";
    }

    private String normalizeAction(String action) {
        return switch (action) {
            case "sendmessage", "message/send", "send_message" -> "send_message";
            case "gettask", "tasks/get", "get_task" -> "get_task";
            case "canceltask", "tasks/cancel", "cancel_task" -> "cancel_task";
            default -> action;
        };
    }
}
