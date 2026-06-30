# 如何注册一个智能体?

在OxyGent4J中，基础的智能体由[智能体（Agent）](./agent-types.md)和内部封装的[大语言模型（LLM）](./select-llm.md)组成。

对于新用户，您可以使用 `HttpLlm.builder()` 方法通过您的 `apiKey` 注册LLM：

```java
HttpLlm.builder()
    .name("default_llm")
    .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
    .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
    .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
    .llmParams(Map.of("temperature", 0.01))
    .semaphoreCount(4)
    .timeout(300)
    .build()
```

> 其中 `semaphoreCount` 参数控制并发量，详细说明请参见 [并行](../multi-agent/parallel.md) 部分。

接下来，您可以使用 `ChatAgent.builder()` 或者 `ReActAgent.builder()` 封装您的第一个agent：

```java
ChatAgent.builder()
    .isMaster(true)
    .name("master_agent")
    .llmModel("default_llm")
    .prompt("你是一个文档分析专家，请为用户提供简要的文档摘要。")
    .build()
```

如果需要带工具调用能力的智能体，请使用 `ReActAgent`：

```java
ReActAgent.builder()
    .isMaster(true)
    .name("master_agent")
    .llmModel("default_llm")
    .tools(Arrays.asList("time_tools"))
    .build()
```

为了使 LLM 和智能体生效，它们需要被添加到 `oxySpace` 列表中。

> **关键概念**
> - **`oxySpace`** 是一个 `List<BaseOxy>`，包含了系统中所有的 LLM、Agent、Tool 组件。组件之间通过 `name` 字符串互相引用。
> - **`Mas`** 是运行时容器。`new Mas(appName, oxySpace)` 会注册所有组件并建立引用关系。
> - **`isMaster(true)`** 标记入口智能体——用户的消息首先到达它，由它决定如何处理或分发给子智能体。

## 完整的可运行样例

以下是可运行的完整代码示例：

```java
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DemoSingleAgent {

    public static List<BaseOxy> createOxySpace() {
        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .llmParams(Map.of("temperature", 0.01))
                        .semaphoreCount(4)
                        .timeout(300)
                        .build(),
                ChatAgent.builder()
                        .isMaster(true)
                        .name("master_agent")
                        .llmModel("default_llm")
                        .prompt("你是一个文档分析专家，用户会向你提供文档，请为用户提供简要的文档摘要。")
                        .build()
        );
    }

    public static void main(String[] args) throws Exception {
        var oxySpace = createOxySpace();
        var mas = new Mas("SingleAgentDemo", oxySpace);
        mas.setDefaultOxySpace(oxySpace);
        mas.init();

        var payload = new HashMap<String, Object>();
        payload.put("query", "Hello!");
        mas.chatWithAgent(payload);
    }
}
```

## 自定义输入/输出处理

OxyGent4J 支持通过 `funcProcessInput` 和 `funcProcessOutput` 自定义处理逻辑：

```java
ChatAgent.builder()
        .isMaster(true)
        .name("master_agent")
        .llmModel("default_llm")
        .prompt("You are a helpful assistant.")
        .funcProcessInput(x -> {
            String query = x.getQuery();
            x.setQuery(query + " Please answer in detail.", false);
            return x;
        })
        .funcProcessOutput(x -> {
            x.setOutput("Answer: " + x.getOutput());
            return x;
        })
        .build()
```

[上一章：快速上手](../getting-started/quickstart.md)
[下一章：选择智能体种类](./agent-types.md)
[回到首页](../readme.md)
