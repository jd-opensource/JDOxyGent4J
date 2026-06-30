package com.jd.oxygent.core.oxygent.transport.a2a;

import io.a2a.spec.TaskState;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.schemas.SSEMessage;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class A2AServerGateway {

    private Mas mas;
    private String targetAgentName = "master_agent";
    private final String a2aBasePath;
    private final String agentVersion;
    private final boolean parseStreamDelta;
    private final A2AInMemoryStore store = new A2AInMemoryStore();

    private static final ObjectMapper A2A_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public A2AServerGateway(String a2aBasePath, String agentVersion) {
        this.a2aBasePath = a2aBasePath != null ? a2aBasePath : "/a2a";
        this.agentVersion = agentVersion != null ? agentVersion : "0.1.0";
        this.parseStreamDelta = true;
    }

    public A2AServerGateway() {
        this("/a2a", "0.1.0");
    }

    public void setMas(Mas mas) {
        this.mas = mas;
        if (mas != null && mas.getMasterAgentName() != null && !mas.getMasterAgentName().isEmpty()) {
            this.targetAgentName = mas.getMasterAgentName();
        }
        log.info("A2AServerGateway bound to MAS, target_agent_name={}, a2a_base_path={}", targetAgentName, a2aBasePath);
    }

    public String getA2aBasePath() {
        return a2aBasePath;
    }

    private String effectiveTarget() {
        return A2ACard.effectiveTarget(mas, targetAgentName);
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response, String subPath) throws IOException {
        if ("GET".equals(request.getMethod())
                && ("/.well-known/agent.json".equals(subPath) || "/.well-known/agent-card.json".equals(subPath))) {
            handleAgentCard(request, response);
            return;
        }

        if ("POST".equals(request.getMethod())) {
            switch (subPath) {
                case "", "/" -> handleUnifiedPost(request, response);
                case "/messages/send", "/messages/send/" -> {
                    handleSendMessage(request, response);
                }
                case "/tasks/get", "/tasks/get/" -> {
                    handleGetTask(request, response);
                }
                case "/tasks/cancel", "/tasks/cancel/" -> {
                    handleCancelTask(request, response);
                }
                default -> handleUnifiedPost(request, response);
            }
            return;
        }

        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void handleAgentCard(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        Object card = A2ACard.buildAgentCard(baseUrl, a2aBasePath, agentVersion, mas);
        sendJson(response, HttpServletResponse.SC_OK, card);
    }

    @SuppressWarnings("unchecked")
    private void handleUnifiedPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> payload = readBody(request);
        if (payload == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "payload must be a JSON object");
            return;
        }

        String method = payload.get("method") instanceof String m ? m : null;
        Object reqId = payload.get("id");

        if (method != null && !method.isEmpty()) {
            Map<String, Object> params = (Map<String, Object>) payload.getOrDefault("params", new HashMap<>());
            if (params == null) params = new HashMap<>();

            try {
                switch (method) {
                    case "message/send", "SendMessage" -> {
                        Object task = runSendMessage(params);
                        sendJson(response, HttpServletResponse.SC_OK, A2AProtocol.rpcOk(reqId, task));
                    }
                    case "message/stream", "SendMessageStream" -> {
                        handleStreamMessage(response, params, reqId);
                    }
                    case "tasks/get", "GetTask" -> {
                        String taskId = getStr(params, "id", getStr(params, "taskId", ""));
                        Object task = runGetTask(taskId);
                        sendJson(response, HttpServletResponse.SC_OK, A2AProtocol.rpcOk(reqId, task));
                    }
                    case "tasks/cancel", "CancelTask" -> {
                        String taskId = getStr(params, "id", getStr(params, "taskId", ""));
                        Object task = runCancelTask(taskId);
                        sendJson(response, HttpServletResponse.SC_OK, A2AProtocol.rpcOk(reqId, task));
                    }
                    case "tasks/resubscribe", "ResubscribeTask" -> {
                        String taskId = getStr(params, "id", getStr(params, "taskId", ""));
                        handleResubscribe(response, taskId, reqId);
                    }
                    default -> sendJson(response, HttpServletResponse.SC_OK,
                            A2AProtocol.rpcError(reqId, -32601, "method `" + method + "` not found"));
                }
            } catch (Exception e) {
                log.error("A2A JSON-RPC error", e);
                sendJson(response, HttpServletResponse.SC_OK, A2AProtocol.rpcError(reqId, -32000, e.getMessage()));
            }
            return;
        }

        // Plain POST dispatch
        String action = resolvePlainAction(payload);
        String taskId = getStr(payload, "id", getStr(payload, "taskId", ""));

        try {
            switch (action) {
                case "send_message" -> {
                    Object task = runSendMessage(payload);
                    sendJson(response, HttpServletResponse.SC_OK, Map.of("task", task));
                }
                case "get_task" -> {
                    Object task = runGetTask(taskId);
                    sendJson(response, HttpServletResponse.SC_OK, Map.of("task", task));
                }
                case "cancel_task" -> {
                    Object task = runCancelTask(taskId);
                    sendJson(response, HttpServletResponse.SC_OK, Map.of("task", task));
                }
                default -> response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "unsupported action. use action in send_message/get_task/cancel_task or JSON-RPC method field");
            }
        } catch (Exception e) {
            log.error("A2A plain POST error", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private void handleSendMessage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> payload = readBody(request);
        if (payload == null) payload = new HashMap<>();
        payload.putIfAbsent("action", "send_message");
        try {
            Object task = runSendMessage(payload);
            sendJson(response, HttpServletResponse.SC_OK, Map.of("task", task));
        } catch (Exception e) {
            log.error("A2A message/send error", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private void handleGetTask(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> payload = readBody(request);
        if (payload == null) payload = new HashMap<>();
        String taskId = getStr(payload, "id", getStr(payload, "taskId", ""));
        try {
            Object task = runGetTask(taskId);
            sendJson(response, HttpServletResponse.SC_OK, Map.of("task", task));
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        }
    }

    private void handleCancelTask(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> payload = readBody(request);
        if (payload == null) payload = new HashMap<>();
        String taskId = getStr(payload, "id", getStr(payload, "taskId", ""));
        try {
            Object task = runCancelTask(taskId);
            sendJson(response, HttpServletResponse.SC_OK, Map.of("task", task));
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        }
    }

    private Object runSendMessage(Map<String, Object> payload) throws Exception {
        Map<String, Object> msgPayload = A2AMapper.normalizeMessagePayload(payload);
        String text = A2AMapper.extractText(msgPayload);
        Map<String, Object> metadata = A2AMapper.extractMetadata(payload);
        String[] ids = A2AMapper.extractContextAndTask(payload, msgPayload);
        String contextId = ids[0];
        String taskId = ids[1];
        List<String> referenceTaskIds = A2AMapper.extractReferenceTaskIds(payload, msgPayload);

        log.info("A2A message/send received context_id={}, task_id={}", contextId, taskId);

        if (!store.tryMarkRunning(taskId)) {
            Object existingTask = store.getTask(taskId);
            if (existingTask != null) return existingTask;
            return store.buildTask(taskId, contextId, "Task is running.", taskId, contextId, TaskState.WORKING);
        }

        store.buildTask(taskId, contextId, "Task is pending.", taskId, contextId, TaskState.SUBMITTED);
        store.buildTask(taskId, contextId, "Task is running.", taskId, contextId, TaskState.WORKING);

        try {
            Map<String, Object> masPayload = A2AMapper.buildMasPayload(
                    text, contextId, taskId, effectiveTarget(),
                    referenceTaskIds, metadata, store.contextSession(contextId)
            );
            if (masPayload == null) {
                return store.buildTask(taskId, contextId, text, taskId, contextId, TaskState.COMPLETED);
            }

            OxyResponse oxyResponse = mas.chatWithAgent(masPayload);
            String traceId = oxyResponse.getOxyRequest() != null ? oxyResponse.getOxyRequest().getCurrentTraceId() : taskId;
            String groupId = oxyResponse.getOxyRequest() != null ? oxyResponse.getOxyRequest().getGroupId() : contextId;
            String answer = oxyResponse.getOutputAsString();

            store.saveContext(contextId, groupId, traceId, taskId);

            Object task = store.buildTask(taskId, contextId, answer, traceId, groupId, TaskState.COMPLETED);
            log.info("A2A message/send completed task_id={}, context_id={}", taskId, contextId);
            return task;
        } catch (Exception e) {
            store.buildTask(taskId, contextId, "Task failed: " + e.getMessage(), taskId, contextId, TaskState.FAILED, e.getMessage());
            throw e;
        } finally {
            store.unmarkRunning(taskId);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleStreamMessage(HttpServletResponse response, Map<String, Object> payload, Object reqId) throws IOException {
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

        log.info("A2A message/stream received context_id={}, task_id={}", contextId, taskId);

        if (!store.tryMarkRunning(taskId)) {
            Object event = A2AProtocol.buildAgentMessage("Task is already running.", taskId, contextId);
            sendSseEvent(response, A2AProtocol.rpcOk(reqId, event));
            return;
        }

        Map<String, Object> masPayload = A2AMapper.buildMasPayload(
                text, contextId, taskId, effectiveTarget(),
                referenceTaskIds, metadata, store.contextSession(contextId)
        );
        if (masPayload == null) {
            store.buildTask(taskId, contextId, text, taskId, contextId, TaskState.COMPLETED);
            store.unmarkRunning(taskId);
            Object event = A2AProtocol.buildAgentMessage(text, taskId, contextId);
            sendSseEvent(response, A2AProtocol.rpcOk(reqId, event));
            return;
        }

        store.buildTask(taskId, contextId, "Task is pending.", taskId, contextId, TaskState.SUBMITTED);
        store.buildTask(taskId, contextId, "Task is running.", taskId, contextId, TaskState.WORKING);

        String redisKey = mas.getMessagePrefix() + ":" + mas.getName() + ":" + taskId;

        CompletableFuture<OxyResponse> masTask = CompletableFuture.supplyAsync(() -> {
            try {
                return mas.chatWithAgent(masPayload, redisKey);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        StringBuilder emitted = new StringBuilder();
        try {
            while (true) {
                Object rpop = mas.getRedisClient().brpop(redisKey);
                if (rpop == null) {
                    if (masTask.isDone()) break;
                    Thread.sleep(50);
                    continue;
                }

                SSEMessage<Map<String, Object>> sseMessage = mas.unpackMessage(Base64.getDecoder().decode((String) rpop));
                if (sseMessage == null || sseMessage.getData() == null) continue;

                Map<String, Object> msgMap = sseMessage.getData();
                if (msgMap.containsKey("event") && "close".equals(msgMap.get("event"))) {
                    break;
                }

                String delta = A2AMapper.extractDeltaFromSseData(msgMap, parseStreamDelta);
                if (delta == null || delta.isEmpty()) continue;

                emitted.append(delta);
                Object event = A2AProtocol.buildAgentMessage(emitted.toString(), taskId, contextId);
                sendSseEvent(response, A2AProtocol.rpcOk(reqId, event));
            }

            OxyResponse oxyResponse = masTask.get();
            String traceId = oxyResponse.getOxyRequest() != null ? oxyResponse.getOxyRequest().getCurrentTraceId() : taskId;
            String groupId = oxyResponse.getOxyRequest() != null ? oxyResponse.getOxyRequest().getGroupId() : contextId;
            String finalAnswer = emitted.length() > 0 ? emitted.toString() : oxyResponse.getOutputAsString();

            if (emitted.length() == 0) {
                Object event = A2AProtocol.buildAgentMessage(finalAnswer, taskId, contextId);
                sendSseEvent(response, A2AProtocol.rpcOk(reqId, event));
            }

            store.buildTask(taskId, contextId, finalAnswer, traceId, groupId, TaskState.COMPLETED);
            store.saveContext(contextId, groupId, traceId, taskId);
            log.info("A2A message/stream completed task_id={}, context_id={}", taskId, contextId);

        } catch (Exception e) {
            String finalAnswer = emitted.length() > 0 ? emitted.toString() : "";
            store.buildTask(taskId, contextId, "Task failed: " + e.getMessage(), taskId, contextId, TaskState.FAILED, e.getMessage());
            log.error("A2A message/stream failed task_id={}", taskId, e);
        } finally {
            store.unmarkRunning(taskId);
        }
    }

    private void handleResubscribe(HttpServletResponse response, String taskId, Object reqId) throws IOException {
        try {
            io.a2a.spec.Task task = store.getTask(taskId);
            if (task == null) throw new RuntimeException("task not found");

            String text = "";
            if (task.getStatus() != null && task.getStatus().message() != null && task.getStatus().message().getParts() != null) {
                for (var part : task.getStatus().message().getParts()) {
                    if (part instanceof io.a2a.spec.TextPart tp) {
                        text = tp.getText();
                        break;
                    }
                }
            }
            String contextId = task.getContextId() != null ? task.getContextId() : "";

            response.setContentType("text/event-stream;charset=UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            Object event = A2AProtocol.buildAgentMessage(text, taskId, contextId);
            sendSseEvent(response, A2AProtocol.rpcOk(reqId, event));
        } catch (Exception e) {
            sendJson(response, HttpServletResponse.SC_OK, A2AProtocol.rpcError(reqId, -32004, e.getMessage()));
        }
    }

    private Object runGetTask(String taskId) throws Exception {
        if (taskId == null || taskId.isEmpty()) {
            throw new IllegalArgumentException("task id is required");
        }
        Object task = store.getTask(taskId);
        if (task == null) {
            throw new RuntimeException("task not found");
        }
        return task;
    }

    @SuppressWarnings("unchecked")
    private Object runCancelTask(String taskId) throws Exception {
        runGetTask(taskId);
        io.a2a.spec.Task existing = store.getTask(taskId);
        String contextId = existing != null ? existing.getContextId() : "";
        store.buildTask(taskId, contextId != null ? contextId : "", "Task canceled.", taskId, contextId != null ? contextId : "", TaskState.CANCELED);
        store.unmarkRunning(taskId);
        log.info("A2A task canceled task_id={}", taskId);
        return store.getTask(taskId);
    }

    private String resolvePlainAction(Map<String, Object> payload) {
        String action = "";
        for (String key : List.of("action", "op", "operation")) {
            Object val = payload.get(key);
            if (val instanceof String s && !s.isEmpty()) {
                action = s.toLowerCase();
                break;
            }
        }
        if (!action.isEmpty()) {
            return normalizeAction(action);
        }
        if (payload.containsKey("message") || payload.containsKey("query")) {
            return "send_message";
        }
        if (payload.containsKey("taskId") || payload.containsKey("id")) {
            return "get_task";
        }
        return "";
    }

    private String normalizeAction(String action) {
        return switch (action) {
            case "sendmessage", "message/send", "send_message" -> "send_message";
            case "gettask", "tasks/get", "get_task" -> "get_task";
            case "canceltask", "tasks/cancel", "cancel_task" -> "cancel_task";
            case "resubscribe", "tasks/resubscribe" -> "resubscribe";
            default -> action;
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readBody(HttpServletRequest request) throws IOException {
        String body = new String(request.getInputStream().readAllBytes(), "UTF-8");
        if (body.isEmpty()) return new HashMap<>();
        return JsonUtils.readValue(body, Map.class);
    }

    private void sendJson(HttpServletResponse response, int status, Object data) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(A2A_MAPPER.writeValueAsString(data));
        response.getWriter().flush();
    }

    private void sendSseEvent(HttpServletResponse response, Object data) throws IOException {
        response.getWriter().write("data: " + A2A_MAPPER.writeValueAsString(data) + "\n\n");
        response.getWriter().flush();
    }

    private String getStr(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        if (val != null && !val.toString().isEmpty()) return val.toString();
        return defaultVal;
    }
}
