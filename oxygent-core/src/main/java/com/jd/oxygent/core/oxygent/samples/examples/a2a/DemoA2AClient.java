package com.jd.oxygent.core.oxygent.samples.examples.a2a;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.A2AClientAgent;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import io.a2a.spec.Task;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A2A Client Demo - Calls a local OxyGent A2A server with non-streaming mode.
 *
 * <pre>
 * Prerequisite:
 *   Start DemoA2AServer first.
 *
 * Usage:
 *   java -cp ... com.jd.oxygent.core.oxygent.samples.examples.a2a.DemoA2AClient
 * </pre>
 */
@Slf4j
public class DemoA2AClient {

    private static final String SERVER_URL = "http://127.0.0.1:8090/a2a";

    public static List<BaseOxy> createOxySpace() {
        return Arrays.asList(
                A2AClientAgent.builder()
                        .name("a2a_client")
                        .serverUrl(SERVER_URL)
                        .streaming(false)
                        .enableTaskPolling(false)
                        .build()
        );
    }

    private static OxyResponse callOnce(Mas mas, String query) throws Exception {
        return callOnce(mas, query, null, null, null);
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
        var mas = new Mas("demo-a2a-client", oxySpace);
        mas.setDefaultOxySpace(oxySpace);
        mas.init();

        // Send a query
        OxyResponse response = callOnce(mas, "1+1等于几");
        String taskId = response.getExtra() != null ? (String) response.getExtra().get("task_id") : null;

        System.out.println("\n[result] " + response.getOutput());
        System.out.println("session: context_id=" + (response.getExtra() != null ? response.getExtra().get("context_id") : "")
                + ", task_id=" + taskId);

        // Retrieve task via tasks/get
        if (taskId != null && !taskId.isEmpty()) {
            A2AClientAgent client = (A2AClientAgent) mas.getOxyNameToOxy().get("a2a_client");
            Task task = client.getTask(taskId);
            System.out.println("\n[tasks/get]");
            System.out.println(JsonUtils.writeValueAsString(task));
        }
    }
}
