# A2AClientAgent
---

## 类层次结构

```
BaseOxy → BaseFlow → BaseAgent → RemoteAgent → A2AClientAgent
```

---

## 简介

`A2AClientAgent` 通过 A2A（Agent-to-Agent）协议连接远端智能体服务。使用官方 A2A Java SDK (`io.github.a2asdk:a2a-java-sdk-client`) 实现 Agent Card 解析、消息发送、任务生命周期管理以及流式事件处理。

**包路径**: `com.jd.oxygent.core.oxygent.oxy.agents`

## 核心特性

- A2A 协议兼容：与 Python `a2a-sdk==0.3.x` 协议兼容
- Agent Card 自动发现：从远端服务器解析智能体能力描述
- 流式/非流式模式：支持 SSE 流式响应和普通请求-响应模式
- 任务轮询：可选的任务状态轮询机制
- 会话管理：自动维护 contextId 和 taskId

## 参数

| 参数 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| `serverUrl` | `String` | `""` | 远端 A2A 服务器 URL（继承自 RemoteAgent） |
| `streaming` | `boolean` | `false` | 是否使用流式模式发送消息 |
| `enableTaskPolling` | `boolean` | `false` | 是否启用任务状态轮询 |
| `taskPollIntervalSeconds` | `double` | `3.0` | 任务轮询间隔（秒） |
| `taskPollMaxWaitSeconds` | `double` | `60.0` | 任务轮询最大等待时间（秒） |
| `headers` | `Map<String, String>` | `{}` | 自定义 HTTP 请求头 |
| `metadata` | `Map<String, Object>` | `{}` | 消息元数据 |
| `cardPath` | `String` | `".well-known/agent.json"` | Agent Card 的路径 |
| `taskTerminalStates` | `List<String>` | `["completed","failed","canceled","rejected"]` | 任务终态列表 |

## 方法

| 方法 | 返回值 | 描述 |
|------|--------|------|
| `init()` | `void` | 解析 Agent Card 并构建 A2A 客户端 |
| `_execute(OxyRequest)` | `OxyResponse` | 发送消息到远端 A2A 服务并返回结果 |
| `getTask(String taskId)` | `Task` | 查询指定任务的状态 |
| `cancelTask(String taskId)` | `Task` | 取消指定任务 |

## 执行流程

1. `init()` 阶段：从 `serverUrl` 解析 Agent Card，获取远端智能体能力描述
2. `_execute()` 阶段：
   - 加载会话 ID（contextId / taskId）
   - 构建 A2A Message
   - 根据 `streaming` 配置选择流式或非流式发送
   - （可选）轮询任务直到终态
   - 保存会话 ID 并返回结果

## 使用示例

```java
A2AClientAgent a2aAgent = A2AClientAgent.builder()
    .name("remote_translation_agent")
    .desc("通过 A2A 协议连接的翻译智能体")
    .serverUrl("https://translate-service.example.com/")
    .streaming(true)
    .enableTaskPolling(false)
    .headers(Map.of("X-API-Key", "your-api-key"))
    .build();

// 初始化（解析 Agent Card）
a2aAgent.init();

// 发送请求
OxyRequest request = OxyRequest.builder()
    .query("Translate this to Chinese: Hello World")
    .build();

OxyResponse response = a2aAgent.execute(request);
System.out.println(response.getOutput());
// Extra 中包含 context_id 和 task_id
System.out.println(response.getExtra().get("context_id"));
```

## 多轮对话

```java
// 第一轮
OxyResponse r1 = a2aAgent.execute(request1);
// contextId 和 taskId 自动通过 sharedData 持久化

// 第二轮（自动延续之前的会话）
OxyResponse r2 = a2aAgent.execute(request2);
```

## 适用场景

- 微服务架构：连接部署为独立服务的远端智能体
- 跨平台协作：与 Python/Go 等实现的 A2A 服务通信
- 多云部署：连接不同区域或云提供商的智能体
- 第三方集成：调用第三方提供的 A2A 兼容服务
