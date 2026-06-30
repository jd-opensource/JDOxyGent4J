# A2A (Agent-to-Agent) 协议支持

OxyGent4J 支持 [A2A 协议](https://a2a-protocol.org/)（Agent-to-Agent Protocol），这是由 Google 贡献、Linux Foundation 托管的开放协议，用于实现不同 Agent 框架之间的互通。

## 功能概览

| 能力 | 说明 |
|------|------|
| A2A Server | 将 OxyGent MAS 暴露为 A2A 服务，供外部 A2A 客户端调用 |
| A2A Client | 调用外部 A2A 服务（包括其他框架如 LangChain、AgentScope 等） |
| 流式/非流式 | 同时支持 `message/send`（同步）和 `message/stream`（SSE 流式） |
| 多轮对话 | 通过 `contextId` + `referenceTaskIds` 实现 A2A 上下文传递 |
| Agent Card | 自动生成 `/.well-known/agent.json` 服务发现卡片 |
| 任务管理 | 支持 `tasks/get`、`tasks/cancel` 任务生命周期管理 |

## SDK 依赖

基于官方 A2A Java SDK（与 Python `a2a-sdk==0.3.x` 协议版本对齐）：

```xml
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-sdk-client</artifactId>
    <version>0.3.3.Final</version>
</dependency>
```

## 快速开始

### 1. 启动 A2A Server

```java
public class DemoA2AServer {

    @OxySpaceBean(value = "demoA2AServerOxySpace", defaultStart = true)
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
            HttpLlm.builder()
                .name("default_llm")
                .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                .build(),
            ChatAgent.builder()
                .name("master_agent")
                .isMaster(true)
                .desc("A2A chat agent")
                .llmModel("default_llm")
                .build()
        );
    }

    public static void main(String[] args) throws Exception {
        Config.getServer().setPort(8090);
        Config.getServer().setEnableA2aServer(true);
        Config.getServer().setA2aBasePath("/a2a");

        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(
            Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(new String[]{"-p", "8090"});
    }
}
```

启动后自动提供以下端点：
- `GET  http://localhost:8090/a2a/.well-known/agent.json` — Agent Card 发现
- `POST http://localhost:8090/a2a` — 统一 JSON-RPC 端点
- `POST http://localhost:8090/a2a/messages/send` — 发送消息
- `POST http://localhost:8090/a2a/tasks/get` — 查询任务
- `POST http://localhost:8090/a2a/tasks/cancel` — 取消任务

### 2. A2A Client 调用

```java
public class DemoA2AClient {

    public static void main(String[] args) throws Exception {
        var oxySpace = Arrays.asList(
            A2AClientAgent.builder()
                .name("a2a_client")
                .serverUrl("http://127.0.0.1:8090/a2a")
                .streaming(false)
                .enableTaskPolling(false)
                .build()
        );

        var mas = new Mas("demo-a2a-client", oxySpace);
        mas.setDefaultOxySpace(oxySpace);
        mas.init();

        OxyRequest request = OxyRequest.builder()
            .callee("a2a_client")
            .arguments(Map.of("query", "1+1等于几"))
            .build();
        request.setMas(mas);

        OxyResponse response = mas.getOxyNameToOxy().get("a2a_client").execute(request);
        System.out.println(response.getOutput());
        // 输出: 2
    }
}
```

### 3. 流式客户端

```java
A2AClientAgent.builder()
    .name("stream_client")
    .serverUrl("http://127.0.0.1:8090/a2a")
    .streaming(true)    // 启用流式
    .build()
```

流式模式下，响应内容会逐步打印到 stdout，完成后返回完整结果。

### 4. 多轮对话

```java
// 第一轮
OxyResponse first = callOnce(mas, "哪个数字最大？1，5，7");
String contextId = (String) first.getExtra().get("context_id");
String taskId = (String) first.getExtra().get("task_id");

// 第二轮 — 传入 contextId 和 referenceTaskIds 保持上下文
Map<String, Object> args = Map.of(
    "query", "哪个数字最小？",
    "context_id", contextId,
    "reference_task_ids", List.of(taskId)
);
OxyResponse second = callOnce(mas, args);
```

## Spring Boot 方式

在 `application.yml` 中开启：

```yaml
oxygent:
  server:
    enable-a2a-server: true
    a2a-base-path: /a2a
```

Spring Boot 启动后自动注册 `A2AController`，提供相同的 A2A 端点。

## A2AClientAgent 配置项

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `serverUrl` | (必填) | A2A 服务端 URL |
| `streaming` | `false` | 是否使用流式模式 |
| `enableTaskPolling` | `false` | 发送后是否轮询 task 状态 |
| `taskPollIntervalSeconds` | `3.0` | 轮询间隔（秒） |
| `taskPollMaxWaitSeconds` | `60.0` | 轮询最大等待时间（秒） |
| `cardPath` | `.well-known/agent.json` | Agent Card 相对路径 |
| `metadata` | `{}` | 附加到请求的元数据 |
| `headers` | `{}` | 自定义 HTTP 头 |

## 架构说明

```
┌──────────────────────────────────────────────────────┐
│  A2A Client Side                                      │
│                                                       │
│  A2AClientAgent (使用 A2A Java SDK)                    │
│    ├── A2A.getAgentCard() → AgentCard                 │
│    ├── Client.sendMessage() → ClientEvent 回调         │
│    ├── Client.getTask() → Task                        │
│    └── Client.cancelTask() → Task                     │
└───────────────────────┬──────────────────────────────┘
                        │ HTTP/SSE (JSON-RPC)
┌───────────────────────▼──────────────────────────────┐
│  A2A Server Side                                      │
│                                                       │
│  A2AServerGateway / A2AController                     │
│    ├── /.well-known/agent.json → AgentCard            │
│    ├── message/send → MAS.chatWithAgent → Task        │
│    ├── message/stream → SSE 流式输出                   │
│    └── tasks/get | tasks/cancel → TaskState 管理      │
│                                                       │
│  A2AInMemoryStore (Task/TaskState/Artifact from SDK)  │
└──────────────────────────────────────────────────────┘
```

## 互通性与示例代码

OxyGent4J 的 A2A 实现与 Python OxyGent、LangChain、AgentScope、Google ADK 等框架互通。示例代码位于 `oxygent-core/.../samples/examples/a2a/` 目录下。

---

[上一章：什么是 A2A？](../concepts/what-is-a2a.md)
[下一章：A2A 设计与能力](./design.md)
[回到首页](../readme.md)
