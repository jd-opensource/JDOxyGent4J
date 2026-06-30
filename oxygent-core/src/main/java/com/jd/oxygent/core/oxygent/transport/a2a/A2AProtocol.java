package com.jd.oxygent.core.oxygent.transport.a2a;

import io.a2a.spec.Artifact;
import io.a2a.spec.Message;
import io.a2a.spec.TextPart;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class A2AProtocol {

    public static Map<String, Object> rpcOk(Object reqId, Object result) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("jsonrpc", "2.0");
        resp.put("id", reqId);
        resp.put("result", result);
        return resp;
    }

    public static Map<String, Object> rpcError(Object reqId, int code, String message) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("jsonrpc", "2.0");
        resp.put("id", reqId);
        resp.put("error", Map.of("code", code, "message", message));
        return resp;
    }

    public static Message buildAgentMessage(String text, String taskId, String contextId) {
        return new Message.Builder()
                .role(Message.Role.AGENT)
                .parts(new TextPart(text))
                .messageId(UUID.randomUUID().toString())
                .taskId(taskId)
                .contextId(contextId)
                .build();
    }

    public static Artifact buildFinalArtifact(String text) {
        return new Artifact(
                UUID.randomUUID().toString(),
                "final_answer",
                null,
                List.of(new TextPart(text)),
                null,
                null
        );
    }
}
