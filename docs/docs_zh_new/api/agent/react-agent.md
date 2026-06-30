# ReActAgent
---

## 类层次结构

```
BaseOxy → BaseFlow → BaseAgent → LocalAgent → ReActAgent
```

---

## 简介

`ReActAgent` 实现了 ReAct（Reasoning and Acting）范式，是一种能够进行多步推理和工具调用的高级智能体。它通过迭代的"思考-行动-观察"循环解决复杂问题。

**包路径**: `com.jd.oxygent.core.oxygent.oxy.agents`

## 核心特性

- 多步推理：支持复杂逻辑推理与问题分解
- 工具调用：动态选择并调用合适的工具
- 观察学习：根据工具执行结果做出后续决策
- 记忆管理：智能的短期与长期记忆管理
- 反思机制：支持对响应质量的反思和改进
- 信任模式：支持直接返回工具结果的快速模式

## 参数

| 参数 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| `maxReactRounds` | `int` | `16` | 每个请求的最大推理-行动循环次数 |
| `isDiscardReactMemory` | `boolean` | `true` | 是否丢弃详细的 ReAct 记忆，仅保留问答对 |
| `memoryMaxTokens` | `int` | `24800` | 记忆裁剪的 Token 预算 |
| `weightShortMemory` | `int` | `5` | 短期记忆的重要性权重 |
| `weightReactMemory` | `int` | `1` | ReAct 记忆片段的重要性权重 |
| `trustMode` | `boolean` | `false` | 为 `true` 时直接返回工具结果 |
| `toolCallPrefixIncluded` | `boolean` | `false` | 工具结果中是否包含执行器名称前缀 |
| `funcMapMemoryOrder` | `Function<Integer, Integer>` | `x -> x` | 记忆位置到重要性分数的映射函数 |
| `funcParseLlmResponse` | `BiFunction<String, OxyRequest, LLMResponse>` | 内置解析器 | 自定义 LLM 响应解析函数 |
| `funcReflexion` | `BiFunction<String, OxyRequest, String>` | 默认空检查 | 对 LLM 回答进行反思的回调函数 |

> 同时继承 `LocalAgent` 的所有参数（`llmModel`、`prompt`、`tools` 等）。

## 方法

| 方法 | 返回值 | 描述 |
|------|--------|------|
| `init()` | `void` | 初始化提示词、解析器、反思函数，配置向量搜索工具 |
| `defaultReflexion(String, OxyRequest)` | `String` | 默认反思：检查响应是否为空 |
| `getHistory(OxyRequest, boolean)` | `Memory` | 检索并加权裁剪对话历史 |
| `buildMessageContext(OxyRequest, Memory)` | `Memory` | 构建包含系统提示、历史和 ReAct 记忆的完整上下文 |
| `executeToolCalls(OxyRequest, Object)` | `ObservationData` | 执行工具调用并收集结果 |
| `callLlm(OxyRequest, Memory)` | `OxyResponse` | 调用 LLM 模型的通用方法 |
| `_execute(OxyRequest)` | `OxyResponse` | 实现完整的 ReAct 循环 |

## 执行流程

```
用户查询 → [思考] → [行动(工具调用)] → [观察(工具结果)]
                ↑                                    ↓
                └────── 循环直到得到答案或达到 maxReactRounds ──┘
```

## 使用示例

```java
ReActAgent reactAgent = ReActAgent.builder()
    .name("research_assistant")
    .desc("能够使用工具进行研究和分析的智能体")
    .llmModel("default_llm")
    .tools(List.of("web_search", "calculator", "file_reader"))
    .maxReactRounds(10)
    .memoryMaxTokens(20000)
    .trustMode(false)
    .isDiscardReactMemory(true)
    .build();

// 设置自定义反思函数
reactAgent.setFuncReflexion((response, request) -> {
    if (response.length() < 10) {
        return "回答过于简短，请提供更详细的分析";
    }
    return null; // null 表示响应可接受
});

OxyRequest request = OxyRequest.builder()
    .query("分析近一周的市场趋势")
    .build();

OxyResponse response = reactAgent.execute(request);
```

## 适用场景

- 复杂问题求解：需要多步推理的任务
- 工具链编排：需要组合多个工具的任务
- 研究分析：收集和分析多源信息
- 决策支持：基于数据分析的决策
- 自动化流程：复杂的自动化工作流
