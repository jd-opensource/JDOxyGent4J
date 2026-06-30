# Launch MAS 示例

演示如何手动创建和启动 MAS（Multi-Agent System）实例，展示多种交互方式：直接调用 Agent/Tool、对话模式、CLI 模式、批量处理和 Web 服务。

## 前置条件

- JDK 17+
- 安装 `uvx`（用于 MCP 时间工具）
- 设置环境变量：`OXY_LLM_API_KEY`、`OXY_LLM_BASE_URL`、`OXY_LLM_MODEL_NAME`

## 完整代码

```java
package com.jd.oxygent.core.oxygent.samples.examples.backend;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.schemas.memory.Memory;
import com.jd.oxygent.core.oxygent.schemas.memory.Message;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import com.jd.oxygent.core.oxygent.utils.OSUtil;

import java.util.*;

public class DemoLaunchMas {

    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .build(),
                new StdioMCPClient("time_tools", "uvx",
                        Arrays.asList("mcp-server-time", "--local-timezone=Asia/Shanghai")),
                ReActAgent.builder()
                        .name("time_agent")
                        .tools(Arrays.asList("time_tools"))
                        .build()
        );
    }

    public static void main(String[] args) throws Exception {
        Mas mas = new Mas("app", getDefaultOxySpace());
        mas.init();

        // 1. 直接调用 Agent
        Map<String, Object> arguments = new HashMap<>(Map.of("query", "What time it is?"));
        mas.call("time_agent", arguments);

        // 2. 直接调用 Tool
        arguments = new HashMap<>(Map.of("timezone", "Asia/Shanghai"));
        mas.call("get_current_time", arguments);

        // 3. 直接调用 LLM
        Memory memory = new Memory();
        memory.setMessages(Arrays.asList(
                Message.systemMessage("You are a helpful assistant."),
                Message.userMessage("hello")));
        arguments = new HashMap<>(Map.of(
                "messages", memory,
                "llm_params", Map.of("temperature", 0.2)));
        mas.call("default_llm", arguments);

        // 4. 通过 Master Agent 对话
        Map<String, Object> payload = new HashMap<>(Map.of("query", "What time it is?"));
        OxyResponse oxyResponse = mas.chatWithAgent(payload);

        // 5. CLI 交互模式
        mas.startCliMode("What time it is?");

        // 6. 批量处理
        List<String> queries = Collections.nCopies(10, "What time it is?");
        List<Object> results = mas.startBatchProcessing(queries, false);

        // 7. 启动 Web 服务
        ServerApp.main(args);
    }
}
```

## 运行方式

```bash
java -cp <classpath> com.jd.oxygent.core.oxygent.samples.examples.backend.DemoLaunchMas
```

## 预期输出

程序按顺序执行各种交互方式：

```
[call time_agent] The current time is 2025-01-15T14:30:00+08:00
[call get_current_time] {"timezone":"Asia/Shanghai","datetime":"2025-01-15T14:30:00"}
[call default_llm] Hello! How can I help you today?
[chatWithAgent] The current time in Asia/Shanghai is 14:30.
[CLI mode] > What time it is? ...
[batch] Processing 10 queries...
[web] Server started on port 8080
```
