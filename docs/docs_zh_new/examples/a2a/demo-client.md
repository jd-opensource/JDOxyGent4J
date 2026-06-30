# A2A Client 示例

演示如何使用 A2AClientAgent 以非流式方式调用 A2A 服务端，发送消息并获取任务状态。

## 前置条件

- JDK 17+
- 先启动 [DemoA2AServer](demo-server.md)（端口 8090）

## 完整代码

```java
package com.jd.oxygent.core.oxygent.samples.examples.a2a;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.A2AClientAgent;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import io.a2a.spec.Task;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Map<String, Object> args = new HashMap<>();
        args.put("query", query);

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

        // 发送查询
        OxyResponse response = callOnce(mas, "1+1等于几");
        String taskId = response.getExtra() != null
                ? (String) response.getExtra().get("task_id") : null;

        System.out.println("\n[result] " + response.getOutput());
        System.out.println("session: context_id=" +
                (response.getExtra() != null ? response.getExtra().get("context_id") : "")
                + ", task_id=" + taskId);

        // 通过 tasks/get 获取任务详情
        if (taskId != null && !taskId.isEmpty()) {
            A2AClientAgent client = (A2AClientAgent) mas.getOxyNameToOxy().get("a2a_client");
            Task task = client.getTask(taskId);
            System.out.println("\n[tasks/get]");
            System.out.println(JsonUtils.writeValueAsString(task));
        }
    }
}
```

## 运行方式

```bash
# 确保 DemoA2AServer 已启动
java -cp <classpath> com.jd.oxygent.core.oxygent.samples.examples.a2a.DemoA2AClient
```

## 预期输出

```
[result] 1+1等于2
session: context_id=abc123, task_id=task-xyz

[tasks/get]
{"id":"task-xyz","status":"completed","artifacts":[...]}
```
