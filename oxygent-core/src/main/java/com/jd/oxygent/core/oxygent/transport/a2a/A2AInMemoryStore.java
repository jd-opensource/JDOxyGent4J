package com.jd.oxygent.core.oxygent.transport.a2a;

import io.a2a.spec.Artifact;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class A2AInMemoryStore {

    private final Map<String, Task> taskStore = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> contextStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap.KeySetView<String, Boolean> runningTaskIds = ConcurrentHashMap.newKeySet();

    public Map<String, Object> contextSession(String contextId) {
        Map<String, Object> session = contextStore.get(contextId);
        return session != null ? session : Map.of();
    }

    public void saveContext(String contextId, String groupId, String traceId, String taskId) {
        Map<String, Object> session = new ConcurrentHashMap<>();
        session.put("group_id", groupId != null ? groupId : "");
        session.put("last_trace_id", traceId != null ? traceId : "");
        session.put("last_task_id", taskId != null ? taskId : "");
        contextStore.put(contextId, session);
    }

    public boolean tryMarkRunning(String taskId) {
        return runningTaskIds.add(taskId);
    }

    public void unmarkRunning(String taskId) {
        runningTaskIds.remove(taskId);
    }

    public Task getTask(String taskId) {
        return taskStore.get(taskId);
    }

    public Task buildTask(
            String taskId,
            String contextId,
            String answer,
            String traceId,
            String groupId,
            TaskState state,
            String error
    ) {
        Message statusMessage = A2AProtocol.buildAgentMessage(
                answer != null && !answer.isEmpty() ? answer : "Task is processing.",
                taskId, contextId);

        TaskStatus status = new TaskStatus(state, statusMessage, null);

        Task.Builder builder = new Task.Builder()
                .id(taskId)
                .contextId(contextId)
                .status(status)
                .metadata(Map.of(
                        "traceId", traceId != null ? traceId : taskId,
                        "groupId", groupId != null ? groupId : contextId
                ));

        if (state == TaskState.COMPLETED) {
            Artifact artifact = A2AProtocol.buildFinalArtifact(answer);
            builder.artifacts(List.of(artifact));
        }

        Task task = builder.build();
        taskStore.put(taskId, task);
        return task;
    }

    public Task buildTask(String taskId, String contextId, String answer, String traceId, String groupId, TaskState state) {
        return buildTask(taskId, contextId, answer, traceId, groupId, state, null);
    }
}
