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
 * A2A Streaming Client Demo - Calls a local OxyGent A2A server with streaming mode.
 *
 * <pre>
 * Prerequisite:
 *   Start DemoA2AServer first.
 *
 * Usage:
 *   java -cp ... com.jd.oxygent.core.oxygent.samples.examples.a2a.DemoA2AStreamClient
 * </pre>
 */
@Slf4j
public class DemoA2AStreamClient {

    private static final String SERVER_URL = "http://127.0.0.1:8090/a2a";

    public static List<BaseOxy> createOxySpace() {
        return Arrays.asList(
                A2AClientAgent.builder()
                        .name("stream_client")
                        .serverUrl(SERVER_URL)
                        .streaming(true)
                        .enableTaskPolling(false)
                        .build()
        );
    }

    private static OxyResponse callOnce(Mas mas, String query) throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put("query", query);

        OxyRequest request = OxyRequest.builder()
                .callee("stream_client")
                .arguments(args)
                .isSendMessage(false)
                .isSaveHistory(false)
                .build();
        request.setMas(mas);

        return mas.getOxyNameToOxy().get("stream_client").execute(request);
    }

    public static void main(String[] args) throws Exception {
        var oxySpace = createOxySpace();
        var mas = new Mas("demo-a2a-stream-client", oxySpace);
        mas.setDefaultOxySpace(oxySpace);
        mas.init();

        // Send a streaming query
        OxyResponse response = callOnce(mas, "讲一个100字的故事。");
        String taskId = response.getExtra() != null ? (String) response.getExtra().get("task_id") : null;

        System.out.println("\n\n[final] " + response.getOutput());
        System.out.println("session: context_id=" + (response.getExtra() != null ? response.getExtra().get("context_id") : "")
                + ", task_id=" + taskId);

        // Verify via tasks/get
        if (taskId != null && !taskId.isEmpty()) {
            A2AClientAgent client = (A2AClientAgent) mas.getOxyNameToOxy().get("stream_client");
            Task task = client.getTask(taskId);
            System.out.println("\n[tasks/get]");
            System.out.println(JsonUtils.writeValueAsString(task));
        }
    }
}
