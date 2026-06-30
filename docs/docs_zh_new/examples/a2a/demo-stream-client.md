# A2A Stream Client 示例

演示如何使用 A2AClientAgent 以流式（streaming）方式调用 A2A 服务端，实时接收生成内容。

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

        // 发送流式查询
        OxyResponse response = callOnce(mas, "讲一个100字的故事。");
        String taskId = response.getExtra() != null
                ? (String) response.getExtra().get("task_id") : null;

        System.out.println("\n\n[final] " + response.getOutput());
        System.out.println("session: context_id=" +
                (response.getExtra() != null ? response.getExtra().get("context_id") : "")
                + ", task_id=" + taskId);

        // 通过 tasks/get 验证结果
        if (taskId != null && !taskId.isEmpty()) {
            A2AClientAgent client = (A2AClientAgent) mas.getOxyNameToOxy().get("stream_client");
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
java -cp <classpath> com.jd.oxygent.core.oxygent.samples.examples.a2a.DemoA2AStreamClient
```

## 预期输出

流式输出实时显示生成的内容片段，完成后显示最终结果：

```
[streaming] 从前...有一个...小村庄...
[final] 从前有一个小村庄，村里住着一位老奶奶...
session: context_id=abc123, task_id=task-xyz

[tasks/get]
{"id":"task-xyz","status":"completed","artifacts":[...]}
```
