# ChatAgent
---

## 类层次结构

```
BaseOxy → BaseFlow → BaseAgent → LocalAgent → ChatAgent
```

---

## 简介

`ChatAgent` 是专为对话场景设计的智能体，继承自 `LocalAgent`。它负责管理会话记忆、处理用户查询，并与大语言模型协调生成响应。

**包路径**: `com.jd.oxygent.core.oxygent.oxy.agents`

## 核心特性

- 多轮对话能力：维护会话上下文，支持连续对话
- 记忆管理：自动处理短期和长期对话历史
- 智能提示词：提供默认助手提示词，可自定义覆盖
- LLM 集成：无缝集成多种大语言模型

## 参数

| 参数 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| *无新增参数* | — | — | `ChatAgent` 未引入新的数据字段；它继承了 `LocalAgent` 中已定义的所有参数。 |

> 继承自 `LocalAgent` 的参数详见 [Agent 概览](./readme.md)。

## 方法

| 方法 | 返回值 | 描述 |
|------|--------|------|
| `init()` | `void` | 初始化智能体，调用父类初始化并设置默认提示词 |
| `setDefaultPrompt()` | `void` | 若未提供自定义 prompt，设置默认值 `"You are a helpful assistant."` |
| `_execute(OxyRequest)` | `OxyResponse` | 核心执行方法：构建临时记忆，追加用户查询，调用 LLM 并返回响应 |

## 执行流程

1. 构建系统消息（使用 `buildInstruction` 渲染 prompt 模板）
2. 加载短期记忆（最近对话历史）
3. 追加当前用户问题
4. 调用配置的 LLM 模型
5. 封装并返回 `OxyResponse`

## 使用示例

```java
// 通过 Builder 创建 ChatAgent
ChatAgent chatAgent = ChatAgent.builder()
    .name("smart_assistant")
    .desc("通用智能助手")
    .llmModel("default_llm")
    .prompt("你是一个专业的 AI 助手，请准确且友善地回答用户问题。")
    .shortMemorySize(10)
    .build();

// 创建请求并执行
OxyRequest request = OxyRequest.builder()
    .query("请解释什么是机器学习？")
    .build();

OxyResponse response = chatAgent.execute(request);
System.out.println(response.getOutput());
```

## 适用场景

- 智能客服系统
- 个人 AI 助手
- 教育问答系统
- 创意写作助手
- 代码助手与技术咨询
