# Agent API 概览

> OxyGent4J 智能体类层次结构及链接索引。

## 类层次结构

```
BaseOxy
└── BaseFlow
    └── BaseAgent
        ├── LocalAgent
        │   ├── ChatAgent              — 单轮/多轮对话
        │   ├── ReActAgent             — 推理+行动循环
        │   │   └── SkillAgent         — 动态技能加载
        │   ├── PlanAndSolveAgent      — 先规划后执行
        │   ├── WorkflowAgent          — 自定义工作流
        │   ├── ParallelAgent          — 并行执行
        │   └── RAGAgent               — 检索增强生成
        └── RemoteAgent
            ├── SSEAgent               — 基于 SSE 的远程智能体
            └── A2AClientAgent         — A2A 协议客户端
```

## Agent 列表

| 类名 | 包路径 | 说明 | 文档 |
|------|--------|------|------|
| ChatAgent | `com.jd.oxygent.core.oxygent.oxy.agents` | 基础对话智能体，管理会话记忆与 LLM 交互 | [chat-agent.md](./chat-agent.md) |
| ReActAgent | `com.jd.oxygent.core.oxygent.oxy.agents` | ReAct 范式，支持多轮推理与工具调用 | [react-agent.md](./react-agent.md) |
| A2AClientAgent | `com.jd.oxygent.core.oxygent.oxy.agents` | 通过 A2A 协议连接远端智能体 | [a2a-client-agent.md](./a2a-client-agent.md) |
| PlanAndSolveAgent | `com.jd.oxygent.core.oxygent.oxy.agents` | 规划-执行-总结三阶段流程 | [../flows/readme.md](../flows/readme.md#planandsolveagent) |
| WorkflowAgent | `com.jd.oxygent.core.oxygent.oxy.agents` | 通过 Function 接口定义自定义工作流 | [../flows/readme.md](../flows/readme.md#workflowagent) |
| ParallelAgent | `com.jd.oxygent.core.oxygent.oxy.agents` | 并行执行多个子任务 | — |
| RemoteAgent | `com.jd.oxygent.core.oxygent.oxy.agents` | 远程智能体抽象基类 | — |

## 通用继承参数（LocalAgent）

所有继承自 `LocalAgent` 的智能体共享以下核心参数：

| 参数 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| `name` | `String` | — | 智能体名称（必填） |
| `desc` | `String` | `""` | 智能体描述，用于被其他 Agent 发现 |
| `llmModel` | `String` | 配置文件默认值 | 所使用的 LLM 模型名称 |
| `prompt` | `String` | 配置文件默认值 | 系统提示词模板，支持 `${variable}` 替换 |
| `tools` | `List<String>` | `[]` | 可用工具名称列表 |
| `subAgents` | `List<String>` | `[]` | 可调用的子智能体名称列表 |
| `shortMemorySize` | `int` | 配置文件默认值 | 短期记忆保留轮数 |
| `isMultimodalSupported` | `boolean` | `false` | 是否支持多模态输入 |
| `timeout` | `double` | `30.0` | 执行超时时间（秒） |
| `semaphore` | `int` | `0` | 并发信号量（0 表示不限制） |
