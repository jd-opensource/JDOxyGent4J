# 如何调用LLM模型?

OxyGent4J所指的LLM是传统的LLM形式，它支持输入一个字符串并输出一个字符串。您可以通过 `HttpLlm`、`OpenAiLlm` 或 `MockLlm` 调用模型。

## 如何选择 LLM 类型?

| 场景 | 推荐类 | 关键参数 |
|------|--------|----------|
| 云端 API（DeepSeek、通义千问、Gemini 等） | `HttpLlm` | `apiKey`, `baseUrl`, `modelName` |
| OpenAI 或 OpenAI 兼容接口 | `OpenAiLlm` | `apiKey`, `baseUrl`, `modelName` |
| Ollama 本地部署模型 | `HttpLlm` | `baseUrl`（不传 apiKey） |
| 测试/开发（无需真实 LLM） | `MockLlm` | `funcMockProcess` |

## 调用一般模型（HttpLlm）

```java
HttpLlm.builder()
    .name("default_llm")
    .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
    .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
    .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
    .llmParams(Map.of("temperature", 0.01))
    .semaphoreCount(4)    // 并发量
    .timeout(300)         // 最大执行时间（秒）
    .retries(3)           // 重试次数
    .build()
```

对于常见的开源模型和闭源模型，OxyGent4J均支持以这种方式进行调用。

> OxyGent4J支持直接url调用和加后缀`/chat/completions`的模型调用。

## 调用OpenAI接口模型（OpenAiLlm）

对于支持OpenAI接口的模型，可以使用以下方法进行调用：

```java
OpenAiLlm.builder()
    .name("default_llm")
    .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
    .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
    .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
    .llmParams(Map.of("temperature", 0.01))
    .semaphoreCount(4)
    .timeout(300)
    .build()
```

### OpenAiLlm 特性

`OpenAiLlm` 基于 OkHttp 客户端，适用于所有兼容 OpenAI 协议的 API。主要特性包括：

- **流式传输支持**：支持实时内容传递和增量消息转发
- **动态配置合并**：配置从多个来源合并，优先级为：请求参数 > 实例 LLM 参数 > 全局 LLM 配置
- **统一响应格式**：对流式和非流式响应均提供统一的 `OxyResponse` 格式处理

## 调用Ollama部署模型

如果您使用Ollama在本地部署了模型，请使用以下方式进行调用：

```java
HttpLlm.builder()
    .name("local_gemma")
    // 注意不要传入apiKey参数
    .baseUrl("http://localhost:11434/api/chat")
    .modelName("gemma:7b")
    .llmParams(Map.of("temperature", 0.2))
    .semaphoreCount(1)
    .timeout(240)
    .build()
```

## 使用 MockLlm 进行测试

`MockLlm` 不调用真实的 LLM API，而是返回预设的模拟输出。适用于开发调试和单元测试：

```java
MockLlm.builder()
    .name("mock_llm")
    .funcMockProcess(oxyRequest -> "这是一个模拟回复，用于测试。")
    .build()
```

`funcMockProcess` 接收 `OxyRequest` 对象，返回字符串作为模拟的 LLM 输出。如果不传此参数，默认返回 `"output"`。

## 常用参数设置

OxyGent4J支持细致设置模型参数，以下是 `HttpLlm` 的常用参数列表：

| 参数 | 类型 | 说明 |
|------|------|------|
| `name` | String | LLM 实例名称（必填） |
| `apiKey` | String | API 认证密钥 |
| `baseUrl` | String | API 基础 URL |
| `modelName` | String | 模型名称 |
| `llmParams` | Map\<String, Object\> | 模型的额外参数（如温度等） |
| `semaphoreCount` | int | 并发信号量，控制最大并发数 |
| `timeout` | int | 最大执行时间，单位为秒 |
| `retries` | int | 请求失败后的重试次数 |
| `httpVersion` | HttpClient.Version | HTTP 协议版本 |
| `headers` | Map\<String, String\> | 自定义 HTTP 请求头 |

### semaphoreCount 参数说明

`semaphoreCount` 用于限制对同一个 LLM 的最大并发请求数。当多个 Agent 同时向同一个 LLM 发起请求时，信号量会限制并发数量，防止 API 限流或服务过载。

```java
HttpLlm.builder()
    .name("default_llm")
    .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
    .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
    .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
    .semaphoreCount(8)  // 允许最多8个并发请求
    .build()
```

## 异常处理

`HttpLlm` 支持通过 `funcProcessLlmException` 自定义异常处理逻辑：

```java
HttpLlm.builder()
    .name("default_llm")
    .apiKey(apiKey)
    .baseUrl(baseUrl)
    .modelName(modelName)
    .funcProcessLlmException(e -> {
        log.error("LLM调用异常: {}", e.getMessage());
        return OxyResponse.builder()
                .state(OxyState.FAILED)
                .output("LLM服务暂时不可用，请稍后重试")
                .build();
    })
    .build()
```

[上一章：选择智能体种类](./agent-types.md)
[下一章：注册工具](../tools/register-tool.md)
[回到首页](../readme.md)
