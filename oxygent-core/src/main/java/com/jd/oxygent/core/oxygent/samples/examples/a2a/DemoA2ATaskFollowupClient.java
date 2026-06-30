package com.jd.oxygent.core.oxygent.samples.examples.a2a;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.A2AClientAgent;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A2A Task Follow-up Client Demo - Validates multi-turn conversation with
 * context_id + reference_task_ids for A2A context propagation.
 *
 * <pre>
 * Prerequisite:
 *   Start DemoA2AServer first.
 *
 * Usage:
 *   java -cp ... com.jd.oxygent.core.oxygent.samples.examples.a2a.DemoA2ATaskFollowupClient
 * </pre>
 */
@Slf4j
public class DemoA2ATaskFollowupClient {

    private static final String SERVER_URL = "http://127.0.0.1:8090/a2a";

    public static List<BaseOxy> createOxySpace() {
        return Arrays.asList(
                A2AClientAgent.builder()
                        .name("a2a_client")
                        .serverUrl(SERVER_URL)
                        .streaming(false)
                        .enableTaskPolling(true)
                        .taskPollIntervalSeconds(3.0)
                        .taskPollMaxWaitSeconds(10.0)
                        .build()
        );
    }

    private static OxyResponse callOnce(Mas mas, String query, String contextId,
                                         String taskId, List<String> referenceTaskIds) throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put("query", query);
        if (contextId != null) args.put("context_id", contextId);
        if (taskId != null) args.put("task_id", taskId);
        if (referenceTaskIds != null) args.put("reference_task_ids", referenceTaskIds);

        OxyRequest request = OxyRequest.builder()
                .callee("a2a_client")
                .arguments(args)
                .isSendMessage(false)
                .isSaveHistory(false)
                .build();
        request.setMas(mas);

        return mas.getOxyNameToOxy().get("a2a_client").execute(request);
    }

    public static void main(String[] args) throws Exception {
        var oxySpace = createOxySpace();
        var mas = new Mas("demo-a2a-task-followup", oxySpace);
        mas.setDefaultOxySpace(oxySpace);
        mas.init();

        // Turn 1: Ask which number is largest
        OxyResponse first = callOnce(mas, "哪个数字最大，直接给出结果，1，5，7", null, null, null);
        System.out.println("\n[turn1] " + first.getOutput());

        String contextId = first.getExtra() != null ? (String) first.getExtra().get("context_id") : null;
        String firstTaskId = first.getExtra() != null ? (String) first.getExtra().get("task_id") : null;
        System.out.println("session: context_id=" + contextId + ", task_id=" + firstTaskId);

        if (contextId == null || contextId.isEmpty() || firstTaskId == null || firstTaskId.isEmpty()) {
            throw new RuntimeException("missing session ids from turn1 response");
        }

        // Wait briefly between turns
        Thread.sleep(1000);

        // Turn 2: Follow up with reference to first task for context continuity
        OxyResponse second = callOnce(mas, "哪个数字最小", contextId, null, List.of(firstTaskId));
        System.out.println("\n[turn2] " + second.getOutput());
        System.out.println("session: context_id=" + (second.getExtra() != null ? second.getExtra().get("context_id") : "")
                + ", task_id=" + (second.getExtra() != null ? second.getExtra().get("task_id") : ""));
    }
}
