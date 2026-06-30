package com.jd.oxygent.core.oxygent.transport.a2a;

import com.jd.oxygent.core.oxygent.utils.JsonUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class A2AMapper {

    @SuppressWarnings("unchecked")
    public static Map<String, Object> normalizeMessagePayload(Map<String, Object> payload) {
        Object message = payload.get("message");
        if (message instanceof Map) {
            return (Map<String, Object>) message;
        }
        return payload;
    }

    @SuppressWarnings("unchecked")
    public static String extractText(Map<String, Object> payload) {
        if (payload == null) return "";

        for (String key : List.of("query", "content", "text")) {
            Object value = payload.get(key);
            if (value instanceof String str && !str.trim().isEmpty()) {
                return str;
            }
        }

        Object parts = payload.get("parts");
        if (parts instanceof List<?> partsList) {
            for (Object partObj : partsList) {
                if (!(partObj instanceof Map)) continue;
                Map<String, Object> part = (Map<String, Object>) partObj;
                Object text = part.get("text");
                if (text instanceof String str && !str.trim().isEmpty()) {
                    return str;
                }
                Object partInner = part.get("part");
                if (partInner instanceof Map<?, ?> partInnerMap
                        && "text".equals(partInnerMap.get("kind"))
                        && partInnerMap.get("text") instanceof String innerText) {
                    return innerText;
                }
            }
        }

        Object message = payload.get("message");
        if (message instanceof Map<?, ?> messageMap) {
            Object messageParts = messageMap.get("parts");
            if (messageParts instanceof List<?> msgPartsList) {
                for (Object partObj : msgPartsList) {
                    if (partObj instanceof Map<?, ?> part
                            && "text".equals(part.get("kind"))
                            && part.get("text") instanceof String txt) {
                        return txt;
                    }
                }
            }
        }

        return JsonUtils.toJSONString(payload);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> extractMetadata(Map<String, Object> payload) {
        Object md = payload.get("metadata");
        if (md instanceof Map) {
            return (Map<String, Object>) md;
        }
        return new HashMap<>();
    }

    public static String[] extractContextAndTask(Map<String, Object> payload, Map<String, Object> fallbackMessage) {
        Map<String, Object> msg = fallbackMessage != null ? fallbackMessage : new HashMap<>();

        String taskId = firstNonEmpty(
                getStr(payload, "taskId"),
                getStr(payload, "task_id"),
                getStr(msg, "taskId"),
                getStr(msg, "task_id")
        );
        if (taskId == null || taskId.isEmpty()) {
            taskId = UUID.randomUUID().toString();
        }

        String contextId = firstNonEmpty(
                getStr(payload, "contextId"),
                getStr(payload, "context_id"),
                getStr(msg, "contextId"),
                getStr(msg, "context_id")
        );
        if (contextId == null || contextId.isEmpty()) {
            contextId = UUID.randomUUID().toString();
        }

        return new String[]{contextId, taskId};
    }

    @SuppressWarnings("unchecked")
    public static List<String> extractReferenceTaskIds(Map<String, Object> payload, Map<String, Object> fallbackMessage) {
        Map<String, Object> msg = fallbackMessage != null ? fallbackMessage : new HashMap<>();

        Object refs = payload.get("referenceTaskIds");
        if (refs == null) refs = payload.get("reference_task_ids");
        if (refs == null) refs = msg.get("referenceTaskIds");
        if (refs == null) refs = msg.get("reference_task_ids");

        if (refs instanceof List<?> refList) {
            return refList.stream()
                    .filter(x -> x != null)
                    .map(Object::toString)
                    .toList();
        }
        return List.of();
    }

    public static Map<String, Object> buildMasPayload(
            String text,
            String contextId,
            String taskId,
            String target,
            List<String> referenceTaskIds,
            Map<String, Object> metadata,
            Map<String, Object> contextSession
    ) {
        if (target == null || target.isEmpty()) {
            return null;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("query", text);
        payload.put("callee", target);
        payload.put("group_id", contextId);
        payload.put("current_trace_id", taskId);
        payload.put("from_trace_id", "");

        Map<String, Object> session = contextSession != null ? contextSession : new HashMap<>();
        String refLast = "";
        if (referenceTaskIds != null && !referenceTaskIds.isEmpty()) {
            refLast = referenceTaskIds.get(referenceTaskIds.size() - 1);
        }

        if (!refLast.isEmpty() && !refLast.equals(taskId)) {
            payload.put("from_trace_id", refLast);
        } else {
            String lastTraceId = (String) session.getOrDefault("last_trace_id", "");
            if (!lastTraceId.isEmpty() && !lastTraceId.equals(taskId)) {
                payload.put("from_trace_id", lastTraceId);
            }
        }

        if (metadata != null && !metadata.isEmpty()) {
            payload.put("group_data", Map.of("a2a_metadata", metadata));
        }
        return payload;
    }

    @SuppressWarnings("unchecked")
    public static String extractDeltaFromSseData(Object data, boolean parseDelta) {
        Object parsed = data;
        if (data instanceof String str) {
            if (!parseDelta) return str;
            try {
                parsed = JsonUtils.parseObject(str, Map.class);
            } catch (Exception e) {
                return str;
            }
        }

        if (!(parsed instanceof Map<?, ?> parsedMap)) {
            return parsed != null ? parsed.toString() : "";
        }

        if (!parseDelta) {
            return JsonUtils.toJSONString(parsed);
        }

        Object typeObj = parsedMap.get("type");
        String msgType = typeObj != null ? typeObj.toString() : "";
        if ("stream".equals(msgType)) {
            Object content = parsedMap.get("content");
            if (content instanceof Map<?, ?> contentMap) {
                Object delta = contentMap.get("delta");
                return delta != null ? delta.toString() : "";
            }
            return "";
        }
        if ("stream_end".equals(msgType)) {
            return "";
        }

        Object content = parsedMap.get("content");
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof Map<?, ?> contentMap) {
            Object output = contentMap.get("output");
            if (output instanceof String) {
                return (String) output;
            }
        }
        return "";
    }

    private static String getStr(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private static String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.isEmpty()) return v;
        }
        return null;
    }
}
