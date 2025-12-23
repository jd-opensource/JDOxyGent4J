# 如何使用多模态？

OxyGent 当前版本支持图片和视频的多模态输入。通过多模态，您可以将图像和视频等附件作为输入，结合文本进行处理，从而实现更丰富的交互。

## 配置多模态模型

首先，您需要声明您的多模态模型，特别是需要设置 `isMultimodalSupported` 为 `true`，以启用多模态支持：

```java
HttpLlm.builder()
    .name("default_vlm")
    .apiKey(System.getenv("DEFAULT_VLM_API_KEY"))
    .baseUrl(System.getenv("DEFAULT_VLM_BASE_URL"))
    .modelName(System.getenv("DEFAULT_VLM_MODEL_NAME"))
    .llmParams(Map.of("temperature", 0.6, "max_tokens", 2048))
    .maxImagePixels(10000000)  // 设置最大像素大小
    .isMultimodalSupported(true)  // 开启多模态支持
    .isConvertUrlToBase64(true)  // 如果需要，将 URL 转换为 base64 格式
    .timeout(30)  // Java版本使用timeout替代semaphore
    .build()
```

## 传入附件

一旦启用多模态支持，您可以通过 `attachments` 参数（或可视化界面）传入附件，OxyGent 会自动处理这些附件并将其与查询一起传递：

```java
// Java版本通过OxySpace配置和SpringBoot应用自动处理附件
// 在Web界面中可以直接上传附件，或通过API传入attachments参数
Map<String, Object> payload = Map.of(
    "query", "What is it in the picture?",  // 提问
    "attachments", Arrays.asList("http://image.jd.com/123.jpg")  // 传入图片附件
);
// 通过Web API或应用直接处理多模态请求
```

在这个例子中，`attachments` 包含了图片的 URL，OxyGent 会自动从 URL 中获取图片并进行处理。

## 完整的可运行示例

以下是一个完整的可运行示例（参考 [DemoMultimodal.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/advanced/DemoMultimodal.java)），展示了如何配置和使用多模态输入：

```java
package com.jd.oxygent.core.oxygent.samples.examples.advanced;

import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.WorkflowAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;

import java.util.*;

/**
 * 多模态处理示例
 * 展示如何配置和使用多模态LLM进行图像处理
 */
public class DemoMultimodal {

    @OxySpaceBean(value = "demoMultimodal", defaultStart = true, query = "What is it in the picture?")
    public static List<BaseOxy> getDefaultOxySpace() {
        // 设置 LLM 模型
        Config.getAgent().setLlmModel("default_vlm");

        return Arrays.asList(
            // 多模态LLM配置
            HttpLlm.builder()
                .name("default_vlm")
                .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                .llmParams(Map.of("temperature", 0.6, "max_tokens", 2048))
                .maxImagePixels(10000000)  // 设置最大像素数
                .isMultimodalSupported(true)  // 开启多模态支持
                .isConvertUrlToBase64(true)  // 将图片 URL 转换为 base64 格式
                .timeout(30)
                .build(),

            // 图像描述代理
            ChatAgent.builder()
                .name("generate_agent")
                .prompt("You are a helpful assistant. Please describe the content of the image in detail.")
                .build(),

            // 图像描述判断代理
            ChatAgent.builder()
                .name("discriminate_agent")
                .prompt("Please determine whether the following text is a description of the content of the image. If it is, please output 'True', otherwise output 'False'.")
                .build(),

            // 主工作流代理
            WorkflowAgent.builder()
                .isMaster(true)
                .name("master_agent")
                .permittedToolNameList(new ArrayList<>(List.of("generate_agent", "discriminate_agent")))
                .funcWorkflow((oxyRequest) -> {
                    // 调用 generate_agent 处理图片描述
                    OxyResponse generateAgentOxyResponse = oxyRequest.call(
                        Map.of("callee", "generate_agent",
                               "arguments", new HashMap<>(Map.of(
                                   "query", oxyRequest.getQuery(),
                                   "attachments", oxyRequest.getArguments().get("attachments"),
                                   "llm_params", Map.of("temperature", 0.6)
                               ))));

                    // 调用 discriminate_agent 判断图片描述是否准确
                    OxyResponse discriminateAgentOxyResponse = oxyRequest.call(
                        Map.of("callee", "discriminate_agent",
                               "arguments", new HashMap<>(Map.of(
                                   "query", generateAgentOxyResponse.getOutput().toString(),
                                   "attachments", oxyRequest.getArguments().get("attachments")
                               ))));

                    return String.format("generate_agent output: %s \n discriminate_agent output: %s",
                                       generateAgentOxyResponse.getOutput(),
                                       discriminateAgentOxyResponse.getOutput());
                })
                .build()
        );
    }

    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);
    }
}
```

## 简化的多模态配置

OxyGent也提供了简化的多模态配置方式，适用于基础的图像处理需求（参考 [`DemoMultimodalNew.java`](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/advanced/DemoMultimodalNew.java)）：

```java
@OxySpaceBean(value = "demoMultimodalNew", defaultStart = true, query="这是什么？")
public static List<BaseOxy> getDefaultOxySpace() {
    return Arrays.asList(
        // 简化的视觉语言模型配置
        HttpLlm.builder()
            .name("default_vlm")
            .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
            .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
            .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
            .isMultimodalSupported(true)  // 启用多模态支持
            .isConvertUrlToBase64(true)   // 自动转换URL为base64
            .build(),

        // 简单的视觉代理
        ChatAgent.builder()
            .name("vision_agent")
            .llmModel("default_vlm")
            .build()
    );
}
```

### 多模态配置对比

| 特性 | 完整工作流配置 | 简化配置 |
|------|-------------|----------|
| **复杂度** | 支持多步骤图像处理 | ✅ 配置简单，一步到位 |
| **功能** | 图像生成+判断+验证 | ✅ 基础图像识别 |
| **适用场景** | 复杂业务逻辑 | ✅ 快速原型和基础需求 |
| **配置难度** | 需要理解工作流 | ✅ 最小化配置 |



**参考现有示例**:
- [DemoMultimodal.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/advanced/DemoMultimodal.java) - 完整的多模态工作流示例
- [DemoMultimodalNew.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/advanced/DemoMultimodalNew.java) - 简化的多模态配置示例
- [DemoMultimodalTransfer.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/advanced/DemoMultimodalTransfer.java) - 多模态传输和LLM配置示例

### 说明

1. **`isMultimodalSupported(true)`**：启用多模态支持，允许您将图像、视频等附件作为输入。
2. **`attachments`**：用于传入图像或其他附件。您可以提供 URL 或 Base64 编码的文件。
3. **Java版本特性**：
   - 使用 `WorkflowAgent` 替代Python版本的 `Workflow`
   - 通过SpringBoot Web应用自动处理附件上传和处理
   - 支持通过Web界面直接上传图片文件


[上一章：创建分布式系统](./11_dstributed.md)
[下一章：导入附件](./10_1_attachments.md)
[回到首页](./readme.md)

