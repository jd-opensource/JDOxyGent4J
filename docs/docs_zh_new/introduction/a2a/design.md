# OxyGent4J A2A 设计说明与能力总览

本文档面向开源使用者，说明 OxyGent4J A2A 从 0 到 1 的设计理念、核心能力、协议映射与最小升级方式。

当前代码主实现：
- A2A Server 网关：`com.jd.oxygent.core.oxygent.transport.a2a.A2AServerGateway`
- A2A Client Agent：`com.jd.oxygent.core.oxygent.oxy.agents.A2AClientAgent`

## 1. 设计理念

OxyGent4J A2A 的核心目标是：**最小改动接入 A2A，最大化复用 MAS 原有能力**。

1. 对 Server 侧：
- 不把 A2A 当成新的业务 Agent，而是当成协议网关（Gateway）
- 将 A2A 请求转换为 MAS 内部调用，结果再转换回 A2A 响应

2. 对 Client 侧：
- 提供基于 `RemoteAgent` 的 `A2AClientAgent`
- 通过 Agent Card 自动发现服务能力，统一 send/stream/poll 使用体验

3. 对框架用户：
- 既支持 OxyGent4J 内部互调，也支持与外部 A2A 框架互通
- 保持旧路径兼容（如 `/messages/send`），降低升级成本

## 2. 架构概览

### 2.1 Server（A2AServerGateway）

`A2AServerGateway` 负责：
- Card 暴露：`/.well-known/agent.json`
- 协议入口：JSON-RPC 与兼容 POST 入口
- 请求转换：A2A -> MAS payload
- 结果转换：MAS output/SSE -> A2A Message/Task 结构

它本质上是"协议适配层"，业务执行仍在 MAS 的目标 Agent 中完成。

```java
public class A2AServerGateway {
    private Mas mas;
    private String targetAgentName = "master_agent";
    private final String a2aBasePath;
    private final A2AInMemoryStore store = new A2AInMemoryStore();

    public A2AServerGateway(String a2aBasePath, String agentVersion) {
        this.a2aBasePath = a2aBasePath != null ? a2aBasePath : "/a2a";
    }

    public void setMas(Mas mas) { ... }  // 绑定 MAS 实例
}
```

### 2.2 Client（A2AClientAgent）

`A2AClientAgent` 负责：
- Card 发现（`serverUrl` 或 `agentCardUrl`）
- 非流式：`message/send`
- 流式：`message/stream`
- 任务态：`tasks/get` 轮询、`cancel`、`resubscribe`
- 会话态：`contextId/taskId/referenceTaskIds` 传递与复用

```java
A2AClientAgent.builder()
    .name("a2a_client")
    .serverUrl("http://127.0.0.1:8090/a2a")
    .streaming(true)
    .enableTaskPolling(false)
    .build();
```

## 3. 核心能力

### 3.1 协议能力

已支持：
1. `message/send`
2. `message/stream`
3. `tasks/get`
4. `tasks/cancel`
5. `tasks/resubscribe`
6. Agent Card 发现与解析

兼容能力：
1. JSON-RPC 方式（标准 A2A 调用）
2. 兼容旧式路由（如 `/messages/send`）
3. 统一 POST 入口按 `method/action` 分发

### 3.2 流式与非流式

1. Server 侧同时支持 `send` 与 `stream`
2. `stream` 通过 MAS 的 SSE 管道桥接到 A2A 事件流
3. 调用方在 Client 侧按参数决定流式或非流式，无需改业务代码

### 3.3 多 Task 与上下文管理

1. `task` 是独立执行单元
2. `context_id` 表示会话上下文
3. 后续 task 通过 `reference_task_ids` 关联前序 task
4. Server 会将 `referenceTaskIds[-1]` 映射到 MAS 的 `from_trace_id`

## 4. A2A 与 MAS 的关键映射

| A2A 概念 | MAS 概念 |
|----------|----------|
| `contextId` | `group_id` |
| `taskId` | 任务执行标识（生命周期管理） |
| `referenceTaskIds[-1]` | `from_trace_id` |
| streaming 事件 | SSE 增量消息 |

## 5. SDK 依赖

基于官方 A2A Java SDK（与 Python `a2a-sdk==0.3.x` 协议版本对齐）：

```xml
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-sdk-client</artifactId>
    <version>0.3.3.Final</version>
</dependency>
```

### Python 与 Java SDK 对应关系

| Python (`a2a-sdk==0.3.26`) | Java (`io.github.a2asdk:0.3.3.Final`) |
|---|---|
| `from a2a.types import TaskState` | `import io.a2a.spec.TaskState` |
| `from a2a.client import A2AClient` | `import io.a2a.client.Client` |
| `create_text_message_object(text)` | `A2A.createUserTextMessage(text, taskId, contextId)` |
| `TaskState.completed` | `TaskState.COMPLETED` |
| `task.status.state` | `task.getStatus().state()` |
| `get_message_text(msg)` | 遍历 `msg.getParts()` 中的 `TextPart.getText()` |

## 6. 最小升级方式（对现有 OxyGent4J 项目）

只需在启动时开启 A2A：

```java
Config.getServer().setEnableA2aServer(true);
Config.getServer().setA2aBasePath("/a2a");
ServerApp.main(new String[]{"-p", "8090"});
```

即可自动装配 A2A Router，对外提供：
- `GET /.well-known/agent.json`
- A2A POST 接口（含 stream/non-stream/task 路径）

## 7. 开源可维护性设计

Server 已按传输层拆分到 `transport/a2a` 包：
- `A2AServerGateway.java`：网关主流程与路由
- `A2ACard.java`：Card/skills 构建
- `A2AMapper.java`：字段提取与请求映射
- `A2AProtocol.java`：A2A 响应结构构造
- `A2AInMemoryStore.java`：task/context 运行态存储

这一拆分让协议层演进、问题定位和测试隔离更清晰。

## 8. 总结

OxyGent4J A2A 已具备以下实用能力：
1. Client + Server 双端能力
2. 流式与非流式双路径
3. 多 task 场景下的 context 关联
4. 旧路径兼容与最小升级接入
5. 多框架互通基础（Python OxyGent / AgentScope / LangChain / Google ADK）

对于现有 OxyGent4J 用户，A2A 接入已经可以通过"少量配置变更"快速落地。

---

[上一章：A2A 快速上手](./demo-guide.md)
[回到首页](../readme.md)
