package com.jd.oxygent.core.oxygent.oxy.agents;

import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyState;
import io.a2a.A2A;
import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.MessageEvent;
import io.a2a.client.TaskEvent;
import io.a2a.client.TaskUpdateEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Artifact;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TextPart;
import io.a2a.spec.UpdateEvent;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * A2A Client Agent — connects to A2A-compatible servers using the official A2A Java SDK.
 *
 * <p>Uses {@code io.github.a2asdk:a2a-java-sdk-client:0.3.3.Final} for:
 * agent card resolution, message creation, task lifecycle, and streaming events.
 * Compatible with Python {@code a2a-sdk==0.3.x} protocol.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@Slf4j
public class A2AClientAgent extends RemoteAgent {

    @Builder.Default
    private boolean streaming = false;

    @Builder.Default
    private boolean enableTaskPolling = false;

    @Builder.Default
    private double taskPollIntervalSeconds = 3.0;

    @Builder.Default
    private double taskPollMaxWaitSeconds = 60.0;

    @Builder.Default
    private Map<String, String> headers = new HashMap<>();

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @Builder.Default
    private String cardPath = ".well-known/agent.json";

    @Builder.Default
    private List<String> taskTerminalStates = List.of("completed", "failed", "canceled", "rejected");

    private AgentCard agentCard;
    private Client a2aClient;

    @Override
    public void init() {
        super.init();
        resolveAndBuildClient();
    }

    private void resolveAndBuildClient() {
        try {
            String baseUrl = getServerUrl().endsWith("/") ? getServerUrl() : getServerUrl() + "/";
            log.info("Resolving A2A agent card from: {}", baseUrl);
            agentCard = A2A.getAgentCard(baseUrl, cardPath, headers);

            a2aClient = Client.builder(agentCard)
                    .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder().build())
                    .clientConfig(ClientConfig.builder()
                            .setStreaming(streaming)
                            .build())
                    .build();

            if (this.getDesc() == null || this.getDesc().isEmpty()) {
                String desc = agentCard.description();
                if (desc != null && !desc.isEmpty()) {
                    this.setDesc(desc);
                    this.setDescForLlm();
                }
            }
            log.info("A2A client initialized: name={}, url={}, streaming={}", agentCard.name(), agentCard.url(), streaming);
        } catch (Exception e) {
            log.warn("Failed to initialize A2A client from {}: {}", getServerUrl(), extractErrorMessage(e));
        }
    }

    @Override
    public OxyResponse _execute(OxyRequest request) {
        if (a2aClient == null) {
            return OxyResponse.builder()
                    .oxyRequest(request)
                    .state(OxyState.FAILED)
                    .output("A2A client not initialized. Check server connection.")
                    .build();
        }

        String query = request.getQuery();
        if (query == null || query.isEmpty()) {
            Object args = request.getArguments();
            query = args != null ? args.toString() : "";
        }

        Map<String, Object> sessionIds = loadSessionIds(request);
        String contextId = (String) sessionIds.get("context_id");
        String taskId = (String) sessionIds.get("task_id");
        @SuppressWarnings("unchecked")
        List<String> referenceTaskIds = (List<String>) sessionIds.getOrDefault("reference_task_ids", List.of());

        try {
            Message message = buildMessage(query, contextId, taskId, referenceTaskIds);

            String answer;
            if (streaming) {
                answer = sendStream(message, sessionIds);
            } else {
                answer = sendNonStream(message, sessionIds);
            }

            contextId = (String) sessionIds.get("context_id");
            taskId = (String) sessionIds.get("task_id");

            if (enableTaskPolling && taskId != null && !taskId.isEmpty()) {
                answer = pollTask(taskId, answer, sessionIds);
                contextId = (String) sessionIds.get("context_id");
            }

            saveSessionIds(request, contextId, taskId);

            Map<String, Object> extra = new HashMap<>();
            extra.put("context_id", contextId);
            extra.put("task_id", taskId);

            return OxyResponse.builder()
                    .oxyRequest(request)
                    .state(OxyState.COMPLETED)
                    .output(answer != null ? answer.trim() : "")
                    .extra(extra)
                    .build();

        } catch (Exception e) {
            String errorMsg = extractErrorMessage(e);
            log.error("A2A execute failed agent={}: {}", getName(), errorMsg, e);
            return OxyResponse.builder()
                    .oxyRequest(request)
                    .state(OxyState.FAILED)
                    .output("A2A execution failed: " + errorMsg)
                    .build();
        }
    }

    // ==================== SDK-based Message Building ====================

    private Message buildMessage(String query, String contextId, String taskId, List<String> referenceTaskIds) {
        Message message = A2A.createUserTextMessage(query, taskId, contextId);
        if (referenceTaskIds != null && !referenceTaskIds.isEmpty()) {
            message = new Message.Builder(message).referenceTaskIds(referenceTaskIds).build();
        }
        if (metadata != null && !metadata.isEmpty()) {
            message = new Message.Builder(message).metadata(metadata).build();
        }
        return message;
    }

    // ==================== SDK-based Send (non-streaming) ====================

    private String sendNonStream(Message message, Map<String, Object> sessionIds) throws Exception {
        AtomicReference<String> answer = new AtomicReference<>("");
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        List<BiConsumer<ClientEvent, AgentCard>> listeners = List.of((event, card) -> {
            updateSessionFromEvent(event, sessionIds);
            String text = extractTextFromEvent(event);
            if (text != null && !text.isEmpty()) {
                answer.set(text);
            }
            latch.countDown();
        });

        log.info("A2A message/send to {} context_id={} task_id={}", agentCard.url(), sessionIds.get("context_id"), sessionIds.get("task_id"));

        a2aClient.sendMessage(message, listeners, error -> {
            errorRef.set(new RuntimeException("A2A send error", error));
            latch.countDown();
        }, null);

        latch.await(120, java.util.concurrent.TimeUnit.SECONDS);
        if (errorRef.get() != null) throw errorRef.get();
        return answer.get();
    }

    // ==================== SDK-based Send (streaming) ====================

    private String sendStream(Message message, Map<String, Object> sessionIds) throws Exception {
        StringBuilder emitted = new StringBuilder();
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        List<BiConsumer<ClientEvent, AgentCard>> listeners = List.of((event, card) -> {
            updateSessionFromEvent(event, sessionIds);
            String text = extractTextFromEvent(event);
            if (text != null && !text.isEmpty()) {
                String current = emitted.toString();
                String delta = text.startsWith(current) ? text.substring(current.length()) : text;
                if (!delta.isEmpty()) {
                    emitted.replace(0, emitted.length(), text.startsWith(current) ? text : current + text);
                    System.out.print(delta);
                    System.out.flush();
                }
            }
        });

        log.info("A2A message/stream to {} context_id={} task_id={}", agentCard.url(), sessionIds.get("context_id"), sessionIds.get("task_id"));

        a2aClient.sendMessage(message, listeners, error -> {
            if (emitted.length() == 0) {
                errorRef.set(new RuntimeException("A2A stream error", error));
            }
            latch.countDown();
        }, null);

        latch.await(300, java.util.concurrent.TimeUnit.SECONDS);
        if (errorRef.get() != null) throw errorRef.get();
        return emitted.toString();
    }

    // ==================== SDK-based Task Operations ====================

    public Task getTask(String taskId) {
        try {
            return a2aClient.getTask(new TaskQueryParams(taskId), null);
        } catch (Exception e) {
            log.warn("getTask failed: {}", extractErrorMessage(e));
            return null;
        }
    }

    public Task cancelTask(String taskId) {
        try {
            return a2aClient.cancelTask(new TaskIdParams(taskId), null);
        } catch (Exception e) {
            log.warn("cancelTask failed: {}", extractErrorMessage(e));
            return null;
        }
    }

    // ==================== SDK-based Task Polling ====================

    private String pollTask(String taskId, String currentAnswer, Map<String, Object> sessionIds) {
        long start = System.currentTimeMillis();
        int rounds = 0;
        String answer = currentAnswer;

        while (true) {
            rounds++;
            try {
                Task task = getTask(taskId);
                if (task != null) {
                    String text = extractTextFromTask(task);
                    if (text != null && !text.isEmpty()) {
                        answer = text;
                    }
                    if (task.getContextId() != null) {
                        sessionIds.put("context_id", task.getContextId());
                    }
                    TaskState state = task.getStatus() != null ? task.getStatus().state() : null;
                    if (state != null && state.isFinal()) {
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("Task polling failed agent={} task_id={}: {}", getName(), taskId, extractErrorMessage(e));
                break;
            }

            if ((System.currentTimeMillis() - start) / 1000.0 >= taskPollMaxWaitSeconds) {
                break;
            }

            try {
                Thread.sleep((long) (taskPollIntervalSeconds * 1000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.debug("Task polling finished agent={} task_id={} rounds={}", getName(), taskId, rounds);
        return answer;
    }

    // ==================== Text Extraction (mirrors Python get_message_text / get_text_parts) ====================

    private static String extractTextFromParts(List<Part<?>> parts) {
        if (parts == null) return "";
        return parts.stream()
                .filter(p -> p instanceof TextPart)
                .map(p -> ((TextPart) p).getText())
                .collect(Collectors.joining("\n"))
                .trim();
    }

    private static String extractTextFromTask(Task task) {
        if (task == null) return "";
        List<String> chunks = new ArrayList<>();

        if (task.getStatus() != null && task.getStatus().message() != null) {
            String text = extractTextFromParts(task.getStatus().message().getParts());
            if (!text.isEmpty()) chunks.add(text);
        }

        if (task.getArtifacts() != null) {
            for (Artifact artifact : task.getArtifacts()) {
                String text = extractTextFromParts(artifact.parts());
                if (!text.isEmpty()) chunks.add(text);
            }
        }

        return chunks.stream().distinct().collect(Collectors.joining("\n")).trim();
    }

    private static String extractTextFromEvent(ClientEvent event) {
        if (event instanceof MessageEvent me) {
            return extractTextFromParts(me.getMessage().getParts());
        }
        if (event instanceof TaskEvent te) {
            return extractTextFromTask(te.getTask());
        }
        if (event instanceof TaskUpdateEvent tue) {
            UpdateEvent update = tue.getUpdateEvent();
            if (update instanceof TaskStatusUpdateEvent statusUpdate) {
                Message msg = statusUpdate.getStatus() != null ? statusUpdate.getStatus().message() : null;
                return msg != null ? extractTextFromParts(msg.getParts()) : "";
            }
            if (update instanceof TaskArtifactUpdateEvent artifactUpdate) {
                Artifact artifact = artifactUpdate.getArtifact();
                return artifact != null ? extractTextFromParts(artifact.parts()) : "";
            }
        }
        return "";
    }

    // ==================== Session ID tracking from events ====================

    private void updateSessionFromEvent(ClientEvent event, Map<String, Object> sessionIds) {
        if (event instanceof MessageEvent me) {
            Message msg = me.getMessage();
            if (msg.getContextId() != null) sessionIds.put("context_id", msg.getContextId());
            if (msg.getTaskId() != null) sessionIds.put("task_id", msg.getTaskId());
        } else if (event instanceof TaskEvent te) {
            Task task = te.getTask();
            if (task.getContextId() != null) sessionIds.put("context_id", task.getContextId());
            if (task.getId() != null) sessionIds.put("task_id", task.getId());
        } else if (event instanceof TaskUpdateEvent tue) {
            Task task = tue.getTask();
            if (task != null) {
                if (task.getContextId() != null) sessionIds.put("context_id", task.getContextId());
                if (task.getId() != null) sessionIds.put("task_id", task.getId());
            }
        }
    }

    // ==================== Session Management ====================

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadSessionIds(OxyRequest request) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> args = request.getArguments() != null ? request.getArguments() : new HashMap<>();

        String sharedKey = "_session_" + getName();
        Map<String, Object> shared = new HashMap<>();
        if (request.getSharedData() != null && request.getSharedData().get(sharedKey) instanceof Map) {
            shared = (Map<String, Object>) request.getSharedData().get(sharedKey);
        }

        String contextId = getStrFromArgs(args, "context_id", (String) shared.get("context_id"));
        String taskId = getStrFromArgs(args, "task_id", (String) shared.get("task_id"));

        Object refIds = args.get("reference_task_ids");
        if (refIds == null) refIds = args.get("referenceTaskIds");
        List<String> referenceTaskIds = new ArrayList<>();
        if (refIds instanceof List<?> refList) {
            for (Object r : refList) {
                if (r != null) referenceTaskIds.add(r.toString());
            }
        }

        result.put("context_id", contextId);
        result.put("task_id", taskId);
        result.put("reference_task_ids", referenceTaskIds);
        return result;
    }

    private void saveSessionIds(OxyRequest request, String contextId, String taskId) {
        String sharedKey = "_session_" + getName();
        Map<String, Object> session = new HashMap<>();
        session.put("context_id", contextId);
        session.put("task_id", taskId);
        if (request.getSharedData() == null) {
            request.setSharedData(new HashMap<>());
        }
        request.getSharedData().put(sharedKey, session);
    }

    private String getStrFromArgs(Map<String, Object> args, String key, String fallback) {
        Object val = args.get(key);
        if (val instanceof String s && !s.isEmpty()) return s;
        return fallback;
    }

    private static String extractErrorMessage(Throwable e) {
        if (e == null) return "Unknown error";
        String msg = e.getMessage();
        Throwable cause = e.getCause();
        while ((msg == null || msg.isEmpty() || "null".equals(msg)) && cause != null) {
            msg = cause.getMessage();
            if (msg == null || msg.isEmpty()) {
                msg = cause.getClass().getSimpleName();
            }
            cause = cause.getCause();
        }
        if (msg == null || msg.isEmpty() || "null".equals(msg)) {
            msg = e.getClass().getSimpleName();
        }
        return msg;
    }
}
