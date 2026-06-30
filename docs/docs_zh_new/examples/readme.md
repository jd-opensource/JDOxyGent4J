# OxyGent4J Examples

本目录包含 OxyGent4J 框架的完整示例代码，按功能分类组织。

## Agent 示例

| 示例 | 说明 |
|------|------|
| [Single Agent](agents/demo-single-agent.md) | 单 Agent 基本用法，演示输入输出处理函数 |
| [ReAct Agent](agents/demo-react-agent.md) | ReAct（推理与行动）Agent，带反思校验能力 |
| [Workflow Agent](agents/demo-workflow-agent.md) | 工作流 Agent，支持编排复杂任务流程 |
| [Parallel Agent](agents/demo-parallel.md) | 并行 Agent，多专家同时评估 |

## Tool 示例

| 示例 | 说明 |
|------|------|
| [MCP Tool](tools/demo-mcp.md) | 通过 MCP 协议接入外部工具（如时间查询） |
| [Function Hub](tools/demo-function-hub.md) | 自定义函数工具注册与调用 |

## A2A（Agent-to-Agent）示例

| 示例 | 说明 |
|------|------|
| [A2A Server](a2a/demo-server.md) | 启动 A2A 兼容的 Agent 服务端 |
| [A2A Client](a2a/demo-client.md) | 非流式调用 A2A 服务端 |
| [A2A Stream Client](a2a/demo-stream-client.md) | 流式调用 A2A 服务端 |

## 分布式多 Agent 示例

| 示例 | 说明 |
|------|------|
| [Distributed MAS](distributed/demo-distributed.md) | 分布式多 Agent 协作（Master + Math + Time） |

## 后端服务示例

| 示例 | 说明 |
|------|------|
| [Launch MAS](backend/demo-launch-server.md) | 手动创建并启动 MAS 实例的多种交互方式 |

## 高级用法示例

| 示例 | 说明 |
|------|------|
| [Trust Mode](advanced/demo-trust-mode.md) | 对比信任模式与普通模式的 Agent 执行差异 |
| [Continue Execution](advanced/demo-continue-exec.md) | 从中间节点恢复/继续执行工作流 |

## 环境准备

所有示例需要设置以下环境变量：

```bash
export OXY_LLM_API_KEY="your-api-key"
export OXY_LLM_BASE_URL="your-llm-base-url"
export OXY_LLM_MODEL_NAME="your-model-name"
```

## 运行方式

每个示例均可通过以下方式运行：

```bash
# 方式 1：直接运行 main 方法
mvn exec:java -Dexec.mainClass="com.jd.oxygent.core.oxygent.samples.examples.agent.DemoSingleAgent"

# 方式 2：在 IDE 中右键运行对应的 main 方法
```
