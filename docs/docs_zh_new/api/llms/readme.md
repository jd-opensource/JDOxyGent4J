# LLM API
---

> OxyGent4J 中所有大语言模型集成的参考文档。

## 类层次结构

```
BaseOxy → BaseLlM
             ├── RemoteLlm（抽象基类）
             │   ├── HttpLlm       — HTTP API 调用（OpenAI/Gemini/Ollama 兼容）
             │   └── OpenAiLlm     — OkHttp + OpenAI API 调用
             └── MockLlm           — 测试用模拟 LLM
```

---

## HttpLlm

**包路径**: `com.jd.oxygent.core.oxygent.oxy.llms`

通过 Java `HttpClient` 与远程 LLM API 通信。自动检测 API 供应商并处理不同的请求/响应格式。

### 支持的供应商

- OpenAI 兼容 API（OpenAI、Azure OpenAI、DeepSeek 等）
- Google Gemini API
- Ollama 本地模型

### 参数

| 参数 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| `baseUrl` | `String` | — | LLM 服务的 API 基础 URL（必填） |
| `apiKey` | `String` | `null` | API 密钥（Ollama 可为空） |
| `modelName` | `String` | — | 模型名称（必填） |
| `timeout` | `Duration` | `30s` | 请求超时时间 |
| `llmParams` | `Map<String, Object>` | `{}` | LLM 扩展参数（temperature、max_tokens 等） |
| `headers` | `Map<String, String>` | `{}` | 自定义 HTTP 请求头 |
| `funcHeaders` | `Function<OxyRequest, Map>` | `null` | 动态请求头生成函数 |
| `streamOutputType` | `String` | `"stream"` | 流式输出消息类型标识 |
| `funcProcessLlmException` | `Function<Exception, OxyResponse>` | `null` | 自定义异常处理函数 |

### 使用示例

```java
HttpLlm httpLlm = HttpLlm.builder()
    .name("default_llm")
    .baseUrl("https://api.openai.com/v1")
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .modelName("gpt-4")
    .llmParams(Map.of("temperature", 0.01, "max_tokens", 4096))
    .build();
```

---

## OpenAiLlm

**包路径**: `com.jd.oxygent.core.oxygent.oxy.llms`

通过 OkHttp 客户端调用 OpenAI API。支持流式和非流式响应，支持 `reasoning_content`（思维链）处理。

### 参数

| 参数 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| `baseUrl` | `String` | — | OpenAI API 基础 URL（必填） |
| `apiKey` | `String` | — | API 密钥（必填） |
| `modelName` | `String` | — | 模型名称（必填） |
| `timeout` | `Integer` | `30` | 超时时间（秒），应用于 connect/read/write |
| `llmParams` | `Map<String, Object>` | `null` | LLM 扩展参数 |

### 使用示例

```java
OpenAiLlm openAiLlm = new OpenAiLlm(
    "https://api.openai.com/v1",
    System.getenv("OPENAI_API_KEY"),
    "gpt-4",
    60,
    Map.of("temperature", 0.7),
    "my_openai_llm"
);
```

---

## MockLlm

**包路径**: `com.jd.oxygent.core.oxygent.oxy.llms`

测试和开发用的模拟 LLM 实现。返回预定义的响应，无需实际模型推理。

### 参数

| 参数 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| `funcMockProcess` | `Function<OxyRequest, String>` | 默认模拟函数（返回 `"output"`） | 自定义模拟处理逻辑 |

### 使用示例

```java
MockLlm mockLlm = MockLlm.builder()
    .name("test_llm")
    .funcMockProcess(request -> {
        return "模拟响应: " + request.getQuery();
    })
    .build();
```

### 适用场景

- Agent 工作流单元测试
- 无外部 LLM 依赖的集成测试
- LLM 相关功能的开发调试
- Agent 逻辑性能测试
