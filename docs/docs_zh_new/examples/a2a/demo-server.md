# A2A Server 示例

演示如何将 OxyGent MAS 作为 A2A（Agent-to-Agent）兼容服务端暴露，支持标准的 Agent 发现和消息通信协议。

## 前置条件

- JDK 17+
- 设置环境变量：`OXY_LLM_API_KEY`、`OXY_LLM_BASE_URL`、`OXY_LLM_MODEL_NAME`

## 完整代码

```java
package com.jd.oxygent.core.oxygent.samples.examples.a2a;

import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DemoA2AServer {

    private static final int PORT = 8090;
    private static final String A2A_BASE_PATH = "/a2a";

    @OxySpaceBean(value = "demoA2AServerOxySpace", defaultStart = true, query = "A2A MAS server is running.")
    public static List<BaseOxy> getDefaultOxySpace() {
        var apiKey = EnvUtils.getEnv("OXY_LLM_API_KEY");
        var baseUrl = EnvUtils.getEnv("OXY_LLM_BASE_URL");
        var modelName = EnvUtils.getEnv("OXY_LLM_MODEL_NAME");

        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .llmParams(Map.of("temperature", 0.01))
                        .build(),
                ChatAgent.builder()
                        .name("master_agent")
                        .isMaster(true)
                        .desc("Local chat agent as MAS target agent for A2A")
                        .llmModel("default_llm")
                        .build()
        );
    }

    public static void main(String[] args) throws Exception {
        Config.getServer().setPort(PORT);
        Config.getServer().setEnableA2aServer(true);
        Config.getServer().setA2aBasePath(A2A_BASE_PATH);

        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(
                Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(new String[]{"-p", String.valueOf(PORT)});
    }
}
```

## 运行方式

```bash
java -cp <classpath> com.jd.oxygent.core.oxygent.samples.examples.a2a.DemoA2AServer
```

## A2A 端点

服务启动后暴露以下端点：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `http://localhost:8090/a2a/.well-known/agent.json` | Agent Card 发现 |
| POST | `http://localhost:8090/a2a` | 统一 JSON-RPC 入口 |
| POST | `http://localhost:8090/a2a/messages/send` | 发送消息 |
| POST | `http://localhost:8090/a2a/tasks/get` | 获取任务状态 |
| POST | `http://localhost:8090/a2a/tasks/cancel` | 取消任务 |

## 预期输出

```
Starting A2A server on port 8090 with base path /a2a
... Server started successfully ...
```
