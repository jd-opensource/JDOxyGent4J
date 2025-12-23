# 如何调用LLM模型？

OxyGent所指的LLM是传统的LLM形式，它支持输入一个字符串并输出一个字符串。您可以通过`HttpLlm`或者`OpenAiLlm`调用模型。

## 调用一般模型

```java
HttpLlm.builder()
    .name("default_llm")
    .apiKey(System.getenv("OXY_LLM_API_KEY"))
    .baseUrl(System.getenv("OXY_LLM_BASE_URL"))
    .modelName(System.getenv("OXY_LLM_MODEL_NAME"))
    .llmParams(Map.of("temperature", 0.01))
    .timeout(240) // 最大执行时间（秒）
    .build()
```

对于常见的开源模型和闭源模型，OxyGent均支持以这种方式进行调用。
> OxyGent支持直接url调用和加后缀`/chat/completions`的模型调用。

## 调用OpenAI接口模型

对于支持OpenAI接口的模型，可以使用以下方法进行调用：

```java
OpenAiLlm.builder()
    .name("default_llm")
    .apiKey(System.getenv("OXY_LLM_API_KEY"))
    .baseUrl(System.getenv("OXY_LLM_BASE_URL"))
    .modelName(System.getenv("OXY_LLM_MODEL_NAME"))
    .llmParams(Map.of("temperature", 0.01))
    .timeout(240)
    .build()
```

## 调用ollama部署模型

如果您使用ollama在本地部署了模型，请使用以下方式进行调用：

```java
HttpLlm.builder()
    .name("local_gemma")
    // 注意不要传入apiKey参数
    .baseUrl("http://localhost:11434/api/chat") // 替换为本地的url接口
    .modelName(System.getenv("DEFAULT_OLLAMA_MODEL"))
    .llmParams(Map.of("temperature", 0.2))
    .timeout(240)
    .build()
```
### url补全说明

OxyGent支持自动补全url，补全逻辑简要如下：
```java
// Java版本的URL补全逻辑在HttpLlm类中实现
if (isGemini) {
    if (!url.endsWith(":generateContent")) {
        url = url + "/models/" + modelName + ":generateContent";
    }
} else if (useOpenai) {
    if (!url.endsWith("/chat/completions")) {
        url = url + "/chat/completions";
    }
} else {
    if (!url.endsWith("/api/chat")) { // only support ollama
        url = url + "/api/chat";
    }
}
```
因此，请您注意以下内容，如果您遇到404问题，大概率是url错误导致的：
- 使用Gemini是可以直接传入模型api，例如`https://generativelanguage.googleapis.com/v1beta`
- 使用通用开源模型（DeepSeek, Qwen）时，即使api_key为EMPTY，也请您写在环境变量中并传入`HttpLlm`。
- 使用基于OpenAI协议的闭源模型（ChatGPT）时，请使用`OpenAiLlm`。
- 使用ollama模型时，不要传入`apiKey`参数。

## 常用参数设置
OxyGent支持细致设置模型参数，您可以在调用时设置LLM参数。以下是一些常用的参数列表：
- **category**: 始终为"llm"，表示这是LLM模型的配置。
- **timeout**: 最大执行时间，单位为秒。
- **llmParams**: 模型的额外参数（如温度设置等）。
- **isSendThink**: 是否向前端发送思考消息。
- **friendlyErrorText**: 错误信息的用户友好提示。
- **isMultimodalSupported**: 模式是否支持多模态输入。
- **isConvertUrlToBase64**: 是否将媒体URL转换为base64格式。
- **maxImagePixels**: 图片处理的最大像素数。
- **maxVideoSize**: 视频处理的最大字节数。

## 完整的可运行样例

以下是LLM配置的完整代码示例（参考 [`DemoInReadme.java`](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/DemoInReadme.java)）：

```java
// 基本LLM配置示例
HttpLlm.builder()
    .name("default_llm")
    .apiKey(System.getenv("OXY_LLM_API_KEY"))
    .baseUrl(System.getenv("OXY_LLM_BASE_URL"))
    .modelName(System.getenv("OXY_LLM_MODEL_NAME"))
    .llmParams(Map.of("temperature", 0.01))
    .timeout(30)
    .build()

// 多模态LLM配置示例 (支持图片、视频等)
HttpLlm.builder()
    .name("multimodal_llm")
    .apiKey(System.getenv("OXY_LLM_API_KEY"))
    .baseUrl(System.getenv("OXY_LLM_BASE_URL"))
    .modelName(System.getenv("DEFAULT_VLM_MODEL_NAME"))
    .isMultimodalSupported(true)    // 启用多模态支持
    .isConvertUrlToBase64(true)     // URL转Base64
    .maxImagePixels(10000000)       // 最大图片像素
    .maxVideoSize(50000000)         // 最大视频大小
    .build()
```

## 如何运行LLM配置示例

你可以通过以下方式运行LLM配置示例：

**参考现有示例**:
- [DemoInReadme.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/DemoInReadme.java) - 基础LLM配置
- [DemoMultimodal.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/advanced/DemoMultimodal.java) - 多模态LLM配置
- [DemoMultimodalTransfer.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/advanced/DemoMultimodalTransfer.java) - 高级多模态配置

## 常见问题解答

**Q: 如何切换不同的LLM模型？**
A: 通过修改环境变量或直接在代码中更改 `modelName` 参数。

**Q: 支持哪些LLM提供商？**
A: 支持OpenAI、Claude、Qwen、DeepSeek、Gemini等主流模型，以及Ollama本地部署模型。

**Q: 多模态LLM如何配置？**
A: 设置 `isMultimodalSupported(true)` 并根据需要调整图片和视频处理参数。

OxyGent默认为每个agent提供单独的LLM。如果您需要配置统一的LLM，请参考[设置默认LLM](./03-01-set_config.md)；如果您需要并行运行多种LLM，请参考[并行调用agent](./07-01-parallel.md)。

[上一章：和智能体交流](./01-02-chat_with_agent.md)
[下一章：预设提示词](./01-04-select_prompt.md)
[回到首页](./readme.md)