# Web API

OxyGent4J 提供两种方式启动 Web 服务：

1. **ServerApp**：轻量嵌入式 Tomcat，适合快速开发和独立部署。
2. **oxygent-studio（Spring Boot）**：功能完整的 Spring Boot 应用，适合生产环境和企业集成。

两种方式都提供相同的核心 API 端点。

---

## 启动方式

### ServerApp（嵌入式 Tomcat）

```java
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;

public class MyApp {
    @OxySpaceBean(value = "myOxySpace", defaultStart = true)
    public static List<BaseOxy> getOxySpace() {
        return Arrays.asList(
            HttpLlm.builder().name("llm").apiKey(key).baseUrl(url).modelName(model).build(),
            ReActAgent.builder().name("master_agent").isMaster(true).llmModel("llm").build()
        );
    }

    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(
            Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);  // 默认端口 8080
    }
}
```

### Spring Boot（oxygent-studio）

在 `application.yml` 中配置：

```yaml
oxygent:
  server:
    port: 8080
    enable-a2a-server: true
    a2a-base-path: /a2a
```

Spring Boot 启动后自动注册所有控制器，提供完整的 Web API。

---

## 状态码定义

```json
200: 成功
400: 失败
500: 服务器错误
```

## Response 结构定义

```json
{
    "code": 200,
    "message": "SUCCESS",
    "data": {}
}
```

---

## 核心接口

### 获取 Agent 架构

```
GET /get_organization

Response:
{
    "code": 200,
    "message": "SUCCESS",
    "data": {
        "id_dict": {"math_agent": 0, "time_agent": 1},
        "organization": {
            "name": "math_agent",
            "type": "agent",
            "children": [
                {
                    "name": "time_agent",
                    "type": "agent",
                    "children": [
                        {"name": "get_current_time", "type": "tool", "is_remote": true}
                    ]
                }
            ]
        }
    }
}
```

### 获取问候语

```
GET /get_welcome_message

Response:
{
    "code": 200,
    "message": "SUCCESS",
    "data": {
        "first_query": "Hi, I'm OxyGent. How can I assist you?"
    }
}
```

### 提问（SSE 流式）

```
POST /sse/chat
Content-Type: application/json

Request Body:
{
    "query": "现在几点",
    "from_trace_id": "from_trace_id"
}
```

前端监听以下四类 SSE 消息：

```json
// 工具调用
{"type": "tool_call", "content": {"caller": "math_agent", "callee": "time_agent", "arguments": {"query": "现在几点"}}}

// 观察结果
{"type": "observation", "content": {"caller": "math_agent", "callee": "time_agent", "output": "当前时间是14:37"}}

// 思考过程
{"type": "think", "content": "用户想知道当前的时间，应该调用get_current_time函数"}

// 最终回答
{"type": "answer", "content": "现在是北京时间14:37。"}
```

### 上传附件

```
POST /upload
Content-Type: multipart/form-data

Response:
{
    "code": 200,
    "message": "SUCCESS",
    "data": {"file_name": "123.jpg"}
}
```

---

## A2A 端点

当开启 A2A Server（`Config.getServer().setEnableA2aServer(true)`）后，额外提供以下端点：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/a2a/.well-known/agent.json` | Agent Card 发现 |
| POST | `/a2a` | 统一 JSON-RPC 端点 |
| POST | `/a2a/messages/send` | 发送消息（兼容旧路由） |
| POST | `/a2a/tasks/get` | 查询任务状态 |
| POST | `/a2a/tasks/cancel` | 取消任务 |

---

## 自定义端口

```java
// ServerApp 方式
ServerApp.main(new String[]{"-p", "8090"});

// 或通过 Config
Config.getServer().setPort(8090);
```

---

## 延伸阅读

- [A2A 设计与能力](../a2a/design.md) — A2A 端点详细说明
- [快速上手](../getting-started/quickstart.md) — 完整启动示例

---

[上一章：后端概述](./overview.md)
[下一章：A2A 快速上手](../a2a/demo-guide.md)
[回到首页](../readme.md)
