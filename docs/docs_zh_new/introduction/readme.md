# 教程

从安装到高级功能的逐步指南。

---

## 入门

| 文档 | 说明 |
|------|------|
| [安装指南](./getting-started/install.md) | Maven/Gradle 依赖配置 |
| [快速上手](./getting-started/quickstart.md) | 5 分钟构建第一个智能体 |
| [概念总览](./getting-started/overview.md) | OxyGent4J 核心概念 |
| [架构总览](./getting-started/architecture.md) | 系统架构与类层次 |
| [配置指南](./getting-started/config.md) | config.json / application.yml 配置 |

## 核心概念

| 文档 | 说明 |
|------|------|
| [什么是 ReAct](./concepts/what-is-react.md) | ReAct 推理+行动模式 |
| [什么是 MCP](./concepts/what-is-mcp.md) | Model Context Protocol |
| [什么是 A2A](./concepts/what-is-a2a.md) | Agent-to-Agent 协议 |

## 智能体

| 文档 | 说明 |
|------|------|
| [创建智能体](./agents/create-agent.md) | 定义你的第一个 Agent |
| [与智能体对话](./agents/chat-with-agent.md) | 调用智能体并获取回复 |
| [选择 LLM](./agents/select-llm.md) | 配置大语言模型 |
| [选择提示词](./agents/select-prompt.md) | 系统提示词配置 |
| [智能体类型](./agents/agent-types.md) | 所有内置智能体类型一览 |
| [动态提示词](./agents/live-prompts.md) | 运行时热更新提示词 |

## 工具

| 文档 | 说明 |
|------|------|
| [注册工具](./tools/register-tool.md) | 注册自定义工具 |
| [管理工具](./tools/manage-tools.md) | 工具绑定与权限 |
| [MCP 开源工具](./tools/opensource-mcp-tools.md) | 使用社区 MCP 工具 |
| [自定义 MCP 工具](./tools/custom-mcp-tools.md) | 开发 MCP 工具服务 |

## 多智能体

| 文档 | 说明 |
|------|------|
| [多智能体系统](./multi-agent/multi-agent-system.md) | 层级编排与路由 |
| [分布式系统](./multi-agent/distributed.md) | 跨进程 SSE 连接 |
| [并行调用](./multi-agent/parallel.md) | ParallelAgent 并发执行 |

## 高级功能

| 文档 | 说明 |
|------|------|
| [工作流](./advanced/workflow.md) | WorkflowAgent 有向图编排 |
| [RAG](./advanced/rag.md) | 检索增强生成 |
| [多模态](./advanced/multimodal.md) | 图片/文件处理 |
| [信任模式](./advanced/trust-mode.md) | 工具自动执行 |
| [连续执行](./advanced/continue-exec.md) | 多步推理链 |

## 后端服务

| 文档 | 说明 |
|------|------|
| [Web API](./backend/web-api.md) | HTTP/SSE 接口 |
| [数据库配置](./backend/database.md) | ES/Redis/向量库 |
| [调试](./backend/debugging.md) | 链路追踪与日志 |

## A2A 协议

| 文档 | 说明 |
|------|------|
| [设计说明](./a2a/design.md) | A2A 架构与协议 |
| [使用指南](./a2a/demo-guide.md) | Server/Client 快速接入 |
