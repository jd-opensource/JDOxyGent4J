# 如何使用多模态？

## 简介

OxyGent4J 支持图片和视频的多模态输入。通过多模态功能，您可以将图像等附件作为输入，结合文本进行处理，实现图片描述、分析、判别等任务。

## 配置多模态模型

首先，您需要声明多模态模型，设置 `isMultimodalSupported(true)` 以启用多模态支持：

```java
HttpLlm.builder()
    .name("default_vlm")
    .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
    .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
    .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
    .llmParams(Map.of("temperature", 0.6, "max_tokens", 2048))
    .maxImagePixels(10000000)         // 最大像素数
    .isMultimodalSupported(true)      // 开启多模态支持
    .isConvertUrlToBase64(true)       // URL 自动转换为 base64
    .base64ImagePrefix("data:image/jpeg")
    .timeout(30)
    .build()
```

## 关键参数

| 参数 | 类型 | 说明 |
|------|------|------|
| `isMultimodalSupported` | `boolean` | 是否启用多模态支持 |
| `isConvertUrlToBase64` | `boolean` | 是否将图片 URL 转为 base64 |
| `maxImagePixels` | `int` | 最大图像像素数限制 |
| `base64ImagePrefix` | `String` | base64 编码图片的前缀 |

## 传入附件

通过 payload 的 `attachments` 参数传入附件：

```java
Map<String, Object> payload = new HashMap<>();
payload.put("query", "What is it in the picture?");
payload.put("attachments", List.of("http://image.jd.com/123.jpg"));

OxyResponse response = mas.chatWithAgent(payload);
System.out.println("LLM: " + response.getOutput());
```

## 完整示例：生成+判别工作流

以下示例构建了一个 generate-discriminate 工作流：先让一个 Agent 描述图片内容，再让另一个 Agent 判断描述是否准确。

```java
public class DemoMultimodal {

    @OxySpaceBean(value = "demoMultimodal", defaultStart = true,
                  query = "What is it in the picture?")
    public static List<BaseOxy> getDefaultOxySpace() {
        Config.getAgent().setLlmModel("default_vlm");
        return Arrays.asList(
            HttpLlm.builder()
                .name("default_vlm")
                .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                .llmParams(Map.of("temperature", 0.6, "max_tokens", 2048))
                .maxImagePixels(10000000)
                .isMultimodalSupported(true)
                .isConvertUrlToBase64(true)
                .base64ImagePrefix("data:image/jpeg")
                .timeout(30)
                .build(),

            ChatAgent.builder()
                .name("generate_agent")
                .prompt("You are a helpful assistant. Please describe the image in detail.")
                .build(),

            ChatAgent.builder()
                .name("discriminate_agent")
                .prompt("Determine whether the text describes the image. Output 'True' or 'False'.")
                .build(),

            WorkflowAgent.builder()
                .isMaster(true)
                .name("master_agent")
                .permittedToolNameList(new ArrayList<>(
                    List.of("generate_agent", "discriminate_agent")))
                .funcWorkflow((oxyRequest) -> {
                    // Step 1: 描述图片
                    OxyResponse genResp = oxyRequest.call(
                        Map.of("callee", "generate_agent",
                               "arguments", new HashMap<>(Map.of(
                                   "query", oxyRequest.getQuery(),
                                   "attachments", oxyRequest.getArguments().get("attachments"),
                                   "llm_params", Map.of("temperature", 0.6)))));

                    // Step 2: 判别描述准确性
                    OxyResponse disResp = oxyRequest.call(
                        Map.of("callee", "discriminate_agent",
                               "arguments", new HashMap<>(Map.of(
                                   "query", genResp.getOutput().toString(),
                                   "attachments", oxyRequest.getArguments().get("attachments")))));

                    return String.format(
                        "generate_agent: %s\ndiscriminate_agent: %s",
                        genResp.getOutput(), disResp.getOutput());
                })
                .build()
        );
    }

    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(
            Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);
    }
}
```

## 说明

1. `isMultimodalSupported(true)` 在 `HttpLlm` 上启用后，该模型可处理图像附件。
2. `attachments` 支持图片 URL 列表或 Base64 编码字符串。
3. Agent 上也可设置 `isMultimodalSupported(true)` 来标识该 Agent 支持多模态。

---

[上一章：继续执行](./continue-exec.md)
[下一章：检索增强生成(RAG)](./rag.md)
[回到首页](../readme.md)
