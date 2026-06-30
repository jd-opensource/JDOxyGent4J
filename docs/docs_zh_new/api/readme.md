# OxyGent4J API 参考文档

> 所有 OxyGent4J 组件的详细类文档。每个页面包含参数、方法、继承层次和使用示例。

## Agents
---
+ [ChatAgent](./agent/chat-agent.md) — 单轮/多轮对话智能体
+ [ReActAgent](./agent/react-agent.md) — 推理+行动循环，支持工具调用
+ [A2AClientAgent](./agent/a2a-client-agent.md) — A2A 协议客户端智能体
+ [PlanAndSolveAgent](./flows/readme.md#planandsolveagent) — 先规划后执行
+ [WorkflowAgent](./flows/readme.md#workflowagent) — 自定义步骤式工作流
+ [ParallelAgent](./agent/readme.md#parallelagent) — 并行子任务执行
+ [RemoteAgent](./agent/readme.md#remoteagent) — 连接远程智能体服务

## 工具
---
+ [FunctionTool](./tools/readme.md#functiontool) — 单个 Java 函数工具
+ [FunctionHub](./tools/readme.md#functionhub) — Java 函数工具集合注册中心
+ [BaseMCPClient](./tools/readme.md#basemcpclient) — MCP 协议客户端基类

## 流程
---
+ [PlanAndSolve](./flows/readme.md#planandsolveagent) — 分解 -> 规划 -> 求解
+ [WorkflowAgent](./flows/readme.md#workflowagent) — 顺序/条件步骤执行

## LLM
---
+ [HttpLlm](./llms/readme.md#httpllm) — 通过 HTTP API 调用模型（OpenAI/Gemini/Ollama 兼容）
+ [OpenAiLlm](./llms/readme.md#openaillm) — 通过 OkHttp + OpenAI API 调用模型
+ [MockLlm](./llms/readme.md#mockllm) — 测试用模拟模型

## 数据模型
---
+ [OxyRequest](./schemas/readme.md#oxyrequest) — 核心请求类型
+ [OxyResponse](./schemas/readme.md#oxyresponse) — 核心响应类型
+ [OxyState](./schemas/readme.md#oxystate) — 执行状态枚举
