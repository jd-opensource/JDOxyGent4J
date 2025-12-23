# 如何选择智能体？

OxyGent提供了很多种预设智能体，这些智能体足以帮助您完成基础的MAS构建，以下是简要介绍：

## ChatAgent

`ChatAgent`是最初级的聊天agent，功能和内部的LLM大致相同。您可以使用`ChatAgent`进行文本相关的工作。

```java
ChatAgent.builder()
    .name("planner_agent")
    .desc("An agent capable of making plans")
    .llmModel("default_llm")
    .prompt("""
        For a given goal, create a simple and step-by-step executable plan. \
        The plan should be concise, with each step being an independent and complete functional module—not an atomic function—to avoid over-fragmentation. \
        The plan should consist of independent tasks that, if executed correctly, will lead to the correct answer. \
        Ensure that each step is actionable and includes all necessary information for execution. \
        The result of the final step should be the final answer. Make sure each step contains all the information required for its execution. \
        Do not add any redundant steps, and do not skip any necessary steps.
        """.strip())
    .build();
```

## WorkflowAgent

在Chat的基础上增加[工作流](./09-03-workflow.md)，可以自定义内部流程走向的Agent。

```java
WorkflowAgent.builder()
    .name("search_agent")
    .desc("一个可以查询数据的工具")
    .subAgents(Arrays.asList("ner_agent", "nen_agent"))
    .funcWorkflow(dataWorkflow)
    .llmModel("default_llm")
    .build();
```

## ReActAgent

一种支持[规划、执行、观察、纠错重试](https://www.promptingguide.ai/zh/techniques/react)的agent，适合进行复杂的工作, 常常作为master_agent。

```java
ReActAgent.builder()
    .name("master_agent")
    .subAgents(Arrays.asList("knowledge_agent", "find_agent", "search_agent"))
    .isMaster(true)
    .llmModel("default_llm")
    .build();
```

ReActAgent包含一些独特的可调节参数，包括：

+ `maxReactRounds: int `：最大react轮数
+ `trustMode: bool`：是否[提供响应元数据](./08-01-trust_mode.md)
+ `funcParseLlmResponse: Function<String, LLMResponse>` ：[处理LLM输出](./08-02-handle_output.md)

## SSEAgent

支持[分布式](./11-01-distributed.md)的agent。

```java
SSEAgent.builder()
    .name("math_agent")
    .desc("一个可以查询圆周率的工具")
    .serverUrl("http://127.0.0.1:8081")
    .build();
```

## ParallelAgent

支持[并行](./07-01-parallel.md)的agent。

```java
ParallelAgent.builder()
    .name("analyzer")
    .desc("A tool that analyze markdown document")
    .permittedToolNameList(Arrays.asList("text_summarizer", "data_analyser", "document_checker"))
    .build();
```

**参考现有示例**:
- [DemoSingleAgent.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoSingleAgent.java) - 基础 ChatAgent 示例
- [DemoReactAgent.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoReactAgent.java) - ReActAgent 示例
- [DemoWorkflowAgent.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoWorkflowAgent.java) - WorkflowAgent 示例
- [DemoHierarchicalAgents.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoHierarchicalAgents.java) - ParallelAgent 示例

[上一章：预设提示词](./01-04-select_prompt.md)
[下一章：设置缓存消息方式](./01-06-select_cache.md)
[回到首页](./readme.md)