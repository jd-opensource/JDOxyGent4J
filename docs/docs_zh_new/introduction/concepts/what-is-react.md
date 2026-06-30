# 什么是 ReAct？

ReAct（Reasoning and Acting）是一种让大语言模型交替进行推理和行动的范式，由 Yao et al. 在 2022 年的论文 *"ReAct: Synergizing Reasoning and Acting in Language Models"* 中提出。

---

## 核心思想

传统的 LLM 调用是"一问一答"：用户提问，模型直接回答。但面对需要查询外部信息或执行操作的任务时，仅靠模型自身的知识往往不够。

ReAct 的解决方案是让模型在"思考"和"行动"之间循环：

```
Thought  →  我需要查一下今天的天气
Action   →  调用 get_weather("北京")
Observation → 北京今天晴，25°C
Thought  →  用户问的是北京天气，我已经拿到结果了
Answer   →  北京今天天气晴朗，气温 25°C。
```

每一轮循环包含三个步骤：

1. **Thought（思考）**：模型分析当前状态，决定下一步该做什么。
2. **Action（行动）**：模型选择并调用一个工具，传入参数。
3. **Observation（观察）**：工具返回执行结果，模型读取后继续思考。

这个循环持续进行，直到模型认为已经获得足够信息，给出最终回答。

---

## 为什么使用 ReAct？

普通的 LLM 在一次前向传播中生成文本，无法查询实时数据、执行计算或与外部系统交互。ReAct 通过赋予 LLM 执行操作并从结果中学习的能力来解决这一问题。

与纯思维链提示（仅推理）或盲目工具调用（仅行动）相比，ReAct 将两者结合——智能体在每次行动前解释其推理过程，使行为更透明、更易调试。

---

## OxyGent4J 中的 ReActAgent

OxyGent4J 的 `ReActAgent` 是 ReAct 范式的完整 Java 实现。它自动管理推理-行动循环，开发者只需声明智能体和工具：

```java
List<BaseOxy> oxySpace = Arrays.asList(
    HttpLlm.builder()
        .name("default_llm")
        .apiKey(apiKey)
        .baseUrl(baseUrl)
        .modelName(modelName)
        .build(),
    myTools,  // FunctionHub 或 MCP 工具
    ReActAgent.builder()
        .name("master_agent")
        .isMaster(true)
        .llmModel("default_llm")
        .tools(Arrays.asList("myTools"))
        .maxReactRounds(10)  // 最大循环次数
        .build()
);
```

每一轮中，`ReActAgent` 会：
1. 将对话历史（包括之前的观察结果）发送给 LLM。
2. 解析 LLM 的响应，提取工具调用。
3. 执行请求的工具并收集观察结果。
4. 将观察结果追加到对话中，然后回到步骤 1。
5. 当 LLM 给出最终回答（没有工具调用）时，循环结束。

### 反思机制（Reflexion）

OxyGent4J 的 ReActAgent 支持自定义反思函数，对输出不满意时触发重做：

```java
BiFunction<String, OxyRequest, String> reflexion = (response, oxyRequest) -> {
    var pattern = Pattern.compile("^[-+]?(\\d+(\\.\\d*)?|\\.\\d+)$");
    if (!pattern.matcher(response.trim()).matches()) {
        return "Only answer with numbers";  // 返回非 null 触发重试
    }
    return null;  // 返回 null 表示验证通过
};

ReActAgent.builder()
    .name("master_agent")
    .llmModel("default_llm")
    .funcReflexion(reflexion)
    .maxReactRounds(2)
    .build();
```

### 关键参数

| 参数 | 说明 |
|------|------|
| `maxReactRounds` | 最大推理-行动循环次数，防止无限循环 |
| `tools` | 智能体可用的工具名称列表 |
| `subAgents` | 可调度的子智能体（子智能体也被视为一种"工具"） |
| `isDiscardReactMemory` | 是否丢弃详细的 ReAct 记忆，节省上下文窗口 |
| `funcReflexion` | 自定义反思函数，对输出不满意时触发重做 |
| `additionalPrompt` | 附加提示词，补充系统提示 |

### 与 ChatAgent 的区别

- `ChatAgent`：单轮 LLM 调用，不支持工具，适合纯对话。
- `ReActAgent`：多轮推理-行动循环，支持工具和子智能体调用，适合需要外部能力的复杂任务。

---

## 延伸阅读

- [智能体类型](../agents/agent-types.md) — 所有智能体类型的对比
- [注册本地工具](../tools/register-tool.md) — 为智能体添加工具
- [快速上手](../getting-started/quickstart.md) — 5 分钟构建一个 ReActAgent

---

[上一章：概念总览](../getting-started/overview.md)
[下一章：什么是 MCP？](./what-is-mcp.md)
[回到首页](../readme.md)
