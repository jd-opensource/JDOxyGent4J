# 如何选择智能体?

OxyGent4J提供了多种预设智能体，这些智能体足以帮助您完成基础的MAS构建，以下是简要介绍：

## `ChatAgent`

`ChatAgent` 是最基础的会话智能体，功能和内部的LLM大致相同。它支持多轮对话记忆管理、自定义提示词配置以及向LLM传递自定义参数，适合作为简单对话场景的首选或复杂系统中的基础组件。

**适用场景：** 问答系统、客服机器人、个人助手、内容生成（文案/摘要）、原型开发与概念验证。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | String | 必填 | 智能体名称 |
| `desc` | String | `""` | 智能体描述 |
| `llmModel` | String | 必填 | 使用的语言模型标识符 |
| `prompt` | String | `"You are a helpful assistant."` | 系统提示词 |
| `shortMemorySize` | int | 继承自 LocalAgent | 短期记忆大小 |

```java
ChatAgent.builder()
    .name("planner_agent")
    .desc("An agent capable of making plans")
    .llmModel("default_llm")
    .prompt("For a given goal, create a simple and step-by-step executable plan.")
    .build()
```

## `ReActAgent`

一种支持规划、执行、观察、纠错重试的agent，适合进行复杂的工作，常常作为master_agent。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | String | 必填 | 智能体名称 |
| `desc` | String | `""` | 智能体描述 |
| `llmModel` | String | `"default_llm"` | LLM 模型名称 |
| `maxReactRounds` | int | `16` | 最大推理-行动轮数 |
| `trustMode` | boolean | `false` | 是否启用 Trust Mode |
| `isDiscardReactMemory` | boolean | `true` | 每次新查询是否清空推理记忆 |
| `funcParseLlmResponse` | BiFunction | `null` | 自定义 LLM 输出解析函数 |
| `funcReflexion` | BiFunction | `null` | 自定义反思函数 |

```java
ReActAgent.builder()
    .isMaster(true)
    .name("master_agent")
    .subAgents(Arrays.asList("knowledge_agent", "file_agent"))
    .llmModel("default_llm")
    .maxReactRounds(10)
    .build()
```

## `WorkflowAgent`

在Chat的基础上增加工作流，可以自定义内部流程走向的Agent。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | String | 必填 | 智能体名称 |
| `desc` | String | `""` | 智能体描述 |
| `funcWorkflow` | Function | `null` | 自定义工作流函数 |
| `subAgents` | List\<String\> | `[]` | 可调用的子智能体名称 |

```java
WorkflowAgent.builder()
    .isMaster(true)
    .name("search_agent")
    .desc("一个可以查询数据的工具")
    .subAgents(Arrays.asList("ner_agent", "nen_agent"))
    .funcWorkflow(oxyRequest -> { /* 自定义逻辑 */ return result; })
    .llmModel("default_llm")
    .build()
```

## `ParallelAgent`

支持并行执行多个子智能体/工具的agent。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | String | 必填 | 智能体名称 |
| `desc` | String | `""` | 智能体描述 |
| `permittedToolNameList` | List\<String\> | `[]` | 并行执行的智能体/工具名称列表 |

```java
ParallelAgent.builder()
    .name("analyzer")
    .desc("A tool that analyze markdown document")
    .permittedToolNameList(Arrays.asList("text_summarizer", "data_analyser"))
    .build()
```

## `PlanAndSolveAgent`

一种将规划与执行分离的两阶段Agent，协调一个规划器和一个执行器。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | String | 必填 | 智能体名称 |
| `planner` | String | `"planner_agent"` | 规划器智能体名称 |
| `solver` | String | `"executor_agent"` | 执行器智能体名称 |

```java
PlanAndSolveAgent.builder()
    .name("master_agent")
    .isMaster(true)
    .planner("planner_agent")
    .solver("solver_agent")
    .build()
```

## `RAGAgent`

支持检索增强生成（Retrieval-Augmented Generation）的Agent。通过自定义的知识检索函数检索外部知识，并将检索结果注入到 Agent 的提示词中。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | String | 必填 | 智能体名称 |
| `desc` | String | 必填 | 智能体描述 |
| `funcRetrieveKnowledge` | Function | 必填 | 知识检索函数 |
| `knowledgePlaceholder` | String | `"knowledge"` | Prompt 中的知识占位符名称 |

```java
RAGAgent.builder()
    .name("rag_agent")
    .desc("A knowledge-augmented agent")
    .funcRetrieveKnowledge(oxyRequest -> "检索到的知识内容")
    .prompt("Based on the following knowledge: ${knowledge}\nPlease answer.")
    .llmModel("default_llm")
    .build()
```

## `ShellUseAgent`

通过 SSH 连接到远程主机并自主执行 Shell 命令来完成任务的Agent。

```java
ShellUseAgent.builder()
    .name("shell_agent")
    .desc("An agent that can execute shell commands")
    .tools(Arrays.asList("ssh_tools"))
    .maxReactRounds(64)
    .build()
```

## `SkillAgent`

能够动态加载技能定义的Agent，技能是可复用的结构化任务模板。

```java
SkillAgent.builder()
    .name("skill_agent")
    .desc("An agent that can discover and execute skills")
    .tools(Arrays.asList("file_tools", "shell_tools"))
    .llmModel("default_llm")
    .build()
```

## `SSEAgent`

支持分布式部署的远程Agent，通过 Server-Sent Events 连接远端运行的 OxyGent 服务。

```java
SSEAgent.builder()
    .name("math_agent")
    .desc("一个可以查询圆周率的工具")
    .serverUrl("http://127.0.0.1:8081")
    .isOxyAgent(true)
    .build()
```

## `A2AClientAgent`

支持 Agent-to-Agent (A2A) 协议的客户端Agent，可以与其他支持 A2A 协议的服务进行通信。

```java
A2AClientAgent.builder()
    .name("a2a_agent")
    .desc("An A2A protocol client agent")
    .serverUrl("http://127.0.0.1:9000")
    .build()
```

[上一章：注册一个智能体](./create-agent.md)
[下一章：选择大语言模型](./select-llm.md)
[回到首页](../readme.md)
