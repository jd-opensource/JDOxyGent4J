# 如何建立简单的多智能体系统?

如果您认为单个智能体无法满足业务需求，使用多智能体系统可以有效地解决这个问题。

在下面的简单示例中，我们将功能相关的工具使用子智能体（subAgent）进行管理。我们推荐新用户使用 `ReActAgent` 来调用这些工具：

```java
ReActAgent.builder()
    .name("file_agent")
    .desc("A tool that can operate the file system")
    .tools(Arrays.asList("file_tools"))
    .build(),
ReActAgent.builder()
    .name("time_agent")
    .desc("A tool that can get current time")
    .tools(Arrays.asList("time_tools"))
    .build(),
ReActAgent.builder()
    .name("math_agent")
    .desc("A tool that can do math calculations")
    .tools(Arrays.asList("math_tools"))
    .build()
```

接下来，您需要注册一个 **master_agent**，它负责在 MAS 中总调度其他智能体。将其他子智能体声明为 **master_agent** 的 `subAgents`：

```java
ReActAgent.builder()
    .isMaster(true)
    .name("master_agent")
    .subAgents(Arrays.asList("file_agent", "time_agent", "math_agent"))
    .build()
```

OxyGent4J 的智能体系统结构非常灵活，这意味着您可以注册多层子智能体（subAgent），而无需手动管理它们之间的协作关系。Master Agent 会根据每个子智能体的 `desc` 描述自动路由请求。

## 层级式智能体树

OxyGent4J 支持构建层级式的智能体树：

```
master_agent (ReActAgent, isMaster=true)
├── file_agent (ReActAgent)
│   └── tools: [file_tools]
├── time_agent (ReActAgent)
│   └── tools: [time_tools]
└── math_agent (ReActAgent)
    └── tools: [math_tools]
```

每个层级的 Agent 只需关注自己的职责，上层 Agent 通过 `subAgents` 来协调下层的工作。

## 完整的可运行样例

以下是可运行的完整代码示例：

```java
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DemoHierarchicalAgents {

    public static List<BaseOxy> createOxySpace() {
        return Arrays.asList(
            HttpLlm.builder()
                    .name("default_llm")
                    .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                    .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                    .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                    .llmParams(Map.of("temperature", 0.01))
                    .semaphoreCount(4)
                    .build(),
            new StdioMCPClient("time", "uvx", Arrays.asList(
                    "mcp-server-time", "--local-timezone=Asia/Shanghai"
            )),
            new StdioMCPClient("file_tools", "npx", Arrays.asList(
                    "-y", "@modelcontextprotocol/server-filesystem", "./local_file"
            )),
            ReActAgent.builder()
                    .name("time_agent")
                    .desc("A tool that can query the time")
                    .tools(Arrays.asList("time"))
                    .build(),
            ReActAgent.builder()
                    .name("file_tools_agent")
                    .desc("A tool for file operation")
                    .tools(Arrays.asList("file_tools"))
                    .build(),
            ReActAgent.builder()
                    .isMaster(true)
                    .name("master_agent")
                    .subAgents(Arrays.asList("time_agent", "file_tools_agent"))
                    .build()
        );
    }

    public static void main(String[] args) throws Exception {
        var oxySpace = createOxySpace();
        var mas = new Mas("HierarchicalDemo", oxySpace);
        mas.setDefaultOxySpace(oxySpace);
        mas.init();

        var payload = new HashMap<String, Object>();
        payload.put("query", "Get the current time and save it to log.txt");
        mas.chatWithAgent(payload);
    }
}
```

## 路由机制

Master Agent 收到用户请求后，会根据以下信息决定路由到哪个子智能体：

1. **子智能体的 `desc`**：描述信息是路由的核心依据
2. **用户的查询内容**：LLM 分析用户意图后选择合适的子智能体
3. **Agent 的 `tools` 列表**：Agent 可用的工具也会影响路由决策

因此，为每个子智能体编写清晰、准确的 `desc` 描述至关重要。

## 附加提示词

您可以通过 `additionalPrompt` 为子智能体添加额外的行为指导：

```java
ReActAgent.builder()
    .name("time_agent")
    .desc("A tool that can query the time")
    .additionalPrompt("Do not send other information except time.")
    .tools(Arrays.asList("time"))
    .build()
```

[上一章：注册工具](../tools/register-tool.md)
[下一章：分布式运行智能体](./distributed.md)
[回到首页](../readme.md)
