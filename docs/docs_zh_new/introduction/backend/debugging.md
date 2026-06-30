# 调试与日志

## 简介

OxyGent4J 提供了完善的日志和追踪机制，帮助开发者调试智能体执行流程。系统使用 `trace_id` 跟踪每次请求的完整调用链，并通过结构化日志记录每个节点的输入输出。

## 日志级别配置

通过 `Config.ServerConfig` 配置日志级别：

```java
@Data
public static class ServerConfig {
    private String logLevel = "INFO";
    // ...
}
```

在 `config.json` 中设置：

```json
{
  "default": {
    "server": {
      "log_level": "DEBUG"
    }
  }
}
```

或通过 `application.yml`：

```yaml
logging:
  level:
    com.jd.oxygent: DEBUG
```

可用的日志级别：`DEBUG` > `INFO` > `WARN` > `ERROR`

## Trace ID 追踪

OxyGent4J 为每次请求分配唯一的 `trace_id`，贯穿整个调用链：

```
INFO - i4oNVqcwQjz6KVg6 - 6m8jX6xmQF4xXzpo - user <<< master_agent <<< time_agent <<< get_current_time
```

日志格式：`时间 - 级别 - trace_id - node_id - 调用链路 : 内容`

### 关键字段说明

| 字段 | 说明 |
|------|------|
| `trace_id` | 请求唯一标识，贯穿整个会话 |
| `node_id` | 当前执行节点标识 |
| `调用链路` | 显示 `user <<< agent <<< tool` 的层级关系 |

### 获取 trace_id

```java
OxyResponse response = mas.chatWithAgent(payload);
String traceId = response.getOxyRequest().getCurrentTraceId();
System.out.println("Trace ID: " + traceId);
```

## 消息发送控制

通过 `Config.MessageConfig` 精细控制日志中包含的信息：

```java
@Data
public static class MessageConfig {
    private boolean isSendToolCall = true;        // 是否记录工具调用
    private boolean isDetailedToolCall = true;    // 是否记录工具调用详情
    private boolean isSendObservation = true;     // 是否记录观察结果
    private boolean isDetailedObservation = true; // 是否记录观察详情
    private boolean isSendThink = true;           // 是否记录思考过程
    private boolean isSendAnswer = true;          // 是否记录最终答案
    private boolean isStored = true;             // 是否持久化存储
    private boolean isShowInTerminal = false;     // 是否在终端显示
    private boolean isSendFullArguments = false;  // 是否发送完整参数
    private Integer streamBatchSize = 256;        // 流式批处理大小
}
```

在 `config.json` 中配置：

```json
{
  "default": {
    "message": {
      "is_send_tool_call": true,
      "is_detailed_tool_call": true,
      "is_send_observation": true,
      "is_show_in_terminal": true
    }
  }
}
```

## 节点可视化

OxyGent4J 内置 Web 可视化界面，启动 `ServerApp.main(args)` 后访问 `http://127.0.0.1:8080` 即可查看调用链路图、每个节点的输入输出以及 LLM 推理过程。支持从任意节点重新执行（参见 [继续执行](../advanced/continue-exec.md)）。

## ReActAgent 调试技巧

### 查看推理过程

设置 `isDiscardReactMemory(false)` 保留完整推理步骤：

```java
ReActAgent.builder()
    .name("debug_agent")
    .isDiscardReactMemory(false)  // 保留 ReAct 中间步骤
    .maxReactRounds(10)           // 最大推理轮次
    .memoryMaxTokens(24800)       // 记忆 token 上限
    .build()
```

### 常见调试场景

| 问题 | 排查方向 |
|------|----------|
| Agent 无响应 | 检查 LLM 连接配置、API Key 是否有效 |
| 工具调用失败 | 查看 node_id 对应的 observation 日志 |
| 上下文丢失 | 检查 `shortMemorySize` 和 `maxMessages` 配置 |
| 循环调用 | 检查 `maxReactRounds` 限制和工具描述 |

## OxyConfig 全局超时配置

```java
@Data
public static class OxyConfig {
    private int semaphore = 1024;      // 最大并发数
    private double timeout = 3600;     // 全局超时 (秒)
    private int retries = 2;           // 重试次数
    private double delay = 1.0;        // 重试延迟 (秒)
}
```

---

[上一章：数据库配置](./database.md)
[下一章：Web API](./web-api.md)
[回到首页](../readme.md)
