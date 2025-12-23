# 如何自定义处理传递给子智能体的提示词？

在较为复杂的 MAS 系统中，您可能需要更新提示词，以防关键信息在智能体（Agent）之间传递时丢失。

OxyGent 支持通过外部方法处理提示词。例如，如果您在提示词中包含了文件内容，并希望确保每个 Agent 都能读取完整的提示词，可以使用 `update_query` 方法在查询中传递提示词。
## 示例：更新提示词
```java
public OxyRequest updateQuery(OxyRequest oxyRequest) {
    String userQuery = oxyRequest.getQuery(true);
    String currentQuery = oxyRequest.getQuery();
    oxyRequest.setQuery(
        String.format("user query is %s\ncurrent query is %s", userQuery, currentQuery)
    );
    return oxyRequest;
}
```

在上述代码中，我们通过 `update_query` 方法合并了 `user_query` 和 `current_query`，并将其设置为新的查询内容。

### 将更新方法应用于智能体

然后，您需要将 `updateQuery` 方法传递给 Agent 的输入处理函数 `funcProcessInput` 中，使得每个 Agent 都能使用自定义的处理逻辑：

```java
ReActAgent.builder()
    .name("file_agent")
    .desc("A tool that can operate the file system")
    .tools(Arrays.asList("file_tools"))
    .funcProcessInput(this::updateQuery) // 假设您希望file_agent读到原始文件
    .build(),

ReActAgent.builder()
    .name("time_agent")
    .desc("A tool that can get current time")
    .tools(Arrays.asList("time_tools")) // 您可以控制每个agent的处理方法
    .build()
// ...
```
## 完整的可运行样例

以下是可运行的完整代码示例：

```java
package com.jd.oxygent.examples.prompts;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.ParallelAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.tools.PresetTools;
import com.jd.oxygent.core.oxygent.schemas.OxyRequest;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

/**
 * 演示如何自定义处理传递给子智能体的提示词
 * 展示了提示词更新和处理方法的应用
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DemoUpdatePromptsExample {

    /**
     * 更新查询提示词的处理方法
     *
     * @param oxyRequest 原始请求对象
     * @return 更新后的请求对象
     */
    public OxyRequest updateQuery(OxyRequest oxyRequest) {
        String userQuery = oxyRequest.getQuery(true);
        String currentQuery = oxyRequest.getQuery();
        oxyRequest.setQuery(
            String.format("user query is %s\ncurrent query is %s", userQuery, currentQuery)
        );
        return oxyRequest;
    }

    /**
     * 获取包含提示词处理的OxySpace配置
     *
     * @return 包含LLM、工具和代理的BaseOxy列表
     */
    @OxySpaceBean(value = "updatePromptsOxySpace", defaultStart = true, query = "Hello!")
    public static List<BaseOxy> getDefaultOxySpace() {
        var example = new DemoUpdatePromptsExample();

        return Arrays.asList(
                // HTTP LLM配置
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .llmParams(Map.of("temperature", 0.01))
                        .semaphoreCount(4)
                        .timeout(240)
                        .build(),

                // 时间工具MCP客户端
                new StdioMCPClient("time_tools", "uvx",
                        Arrays.asList("mcp-server-time", "--local-timezone=Asia/Shanghai")),

                // 预设文件工具
                PresetTools.FILE_TOOLS,

                // 文件代理（使用提示词处理）
                ReActAgent.builder()
                        .name("file_agent")
                        .desc("A tool that can operate the file system")
                        .tools(Arrays.asList("file_tools"))
                        .funcProcessInput(example::updateQuery)
                        .build(),

                // 时间代理（不使用提示词处理）
                ReActAgent.builder()
                        .name("time_agent")
                        .desc("A tool that can get current time")
                        .tools(Arrays.asList("time_tools"))
                        .build(),

                // 文本总结代理（使用提示词处理）
                ChatAgent.builder()
                        .name("text_summarizer")
                        .desc("A tool that can summarize markdown text")
                        .prompt("You are a text summarizer that creates concise summaries of markdown content.")
                        .funcProcessInput(example::updateQuery)
                        .build(),

                // 数据分析代理（使用提示词处理）
                ChatAgent.builder()
                        .name("data_analyser")
                        .desc("A tool that can summarize echart data")
                        .prompt("You are a data analyst that can analyze chart and visualization data.")
                        .funcProcessInput(example::updateQuery)
                        .build(),

                // 文档检查代理（使用提示词处理）
                ChatAgent.builder()
                        .name("document_checker")
                        .desc("A tool that can find problems in document")
                        .prompt("You are a document checker that finds problems in documents.")
                        .funcProcessInput(example::updateQuery)
                        .build(),

                // 并行分析代理（使用提示词处理）
                ParallelAgent.builder()
                        .name("analyzer")
                        .desc("A tool that analyze markdown document")
                        .permittedToolNameList(Arrays.asList("text_summarizer", "data_analyser", "document_checker"))
                        .funcProcessInput(example::updateQuery)
                        .build(),

                // 主代理
                ReActAgent.builder()
                        .name("master_agent")
                        .isMaster(true)
                        .subAgents(Arrays.asList("file_agent", "time_agent", "analyzer"))
                        .build()
        );
    }

    /**
     * 应用程序主入口点
     * 启动Spring Boot应用并初始化提示词处理集成
     *
     * @param args 命令行参数
     * @throws Exception 当应用启动失败时抛出异常
     */
    public static void main(String[] args) throws Exception {
        var currentClassName = Thread.currentThread().getStackTrace()[1].getClassName();
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(currentClassName);
        ServerApp.main(args);
    }
}
```


**参考现有示例**:
- [DemoSingleAgent.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoSingleAgent.java) - 输入处理和提示词修改示例
- [DemoSaveMessage.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoSaveMessage.java) - 消息传递和处理
- 上述完整代码示例 - 动态提示词更新的完整实现

## 动态提示词管理最佳实践

### 1. 上下文保持
```java
// 好的示例：保持用户原始意图的同时增强上下文
public OxyRequest enhanceContext(OxyRequest request) {
    String userQuery = request.getQuery(true);  // 获取原始用户查询
    String currentQuery = request.getQuery();   // 获取当前查询

    // 增强上下文但保持原始意图
    String enhancedQuery = String.format(
        "原始用户问题：%s\n\n当前处理阶段查询：%s\n\n请基于完整上下文回答",
        userQuery, currentQuery
    );

    request.setQuery(enhancedQuery);
    return request;
}
```

### 2. 信息过滤
```java
// 好的示例：过滤和清理提示词内容
public OxyRequest filterSensitiveInfo(OxyRequest request) {
    String query = request.getQuery();

    // 移除敏感信息或不相关内容
    query = query.replaceAll("\\b\\d{4}-\\d{4}-\\d{4}-\\d{4}\\b", "[CARD_NUMBER]");
    query = query.replaceAll("password\\s*[:=]\\s*\\S+", "password: [HIDDEN]");

    request.setQuery(query);
    return request;
}
```

## 常见问题解答

**Q: 动态提示词更新会影响性能吗？**
A: 轻量级的文本处理影响很小，但复杂的上下文构建需要考虑性能开销。建议只在必要的Agent上应用。

**Q: 如何避免提示词无限增长？**
A: 可以设置最大长度限制，或只传递关键信息摘要，避免完整历史传递。

**Q: 多个Agent之间如何共享上下文？**
A: 通过主控Agent协调，或使用全局状态管理，但要注意避免信息混乱。

[上一章：反思重做模式](./08-03-reflexion.md)
[下一章：获取记忆和重新生成](./09-01-continue_exec.md)
[回到首页](./readme.md)