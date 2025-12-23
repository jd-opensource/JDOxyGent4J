# 如何处理附件和文件？

OxyGent 支持处理各种类型的附件，包括图片、文档、音频等文件。通过多模态LLM和附件处理功能，您可以构建能够理解和处理文件内容的智能体系统。

## 1. 基本附件处理配置

要使用附件处理功能，您需要配置支持多模态的LLM：

```java
HttpLlm.builder()
    .name("default_llm")
    .apiKey(System.getenv("OXY_LLM_API_KEY"))
    .baseUrl(System.getenv("OXY_LLM_BASE_URL"))
    .modelName(System.getenv("OXY_LLM_MODEL_NAME"))
    .isMultimodalSupported(true)  // 关键配置：启用多模态支持
    .maxImagePixels(10000000)     // 设置最大图片像素
    .isConvertUrlToBase64(true)   // 自动转换URL为Base64
    .timeout(30)
    .build()
```

## 2. 创建附件处理智能体

配置一个专门处理附件的智能体：

```java
ChatAgent.builder()
    .name("attachment_agent")
    .desc("An agent specialized in processing attachments and files")
    .llmModel("default_llm")  // 使用支持多模态的LLM
    .prompt("You are a helpful assistant that can analyze and process various types of attachments including images, documents, and other files. Please provide detailed analysis of the content.")
    .build()
```

## 3. 附件上传和处理流程

### Web界面上传

用户可以通过OxyGent的Web界面直接上传附件：

1. 访问 `http://localhost:8080`
2. 在聊天界面中点击附件上传按钮
3. 选择要上传的文件（支持图片、PDF、文档等）
4. 发送消息，智能体会自动处理附件内容

### API方式上传

也可以通过API方式上传和处理附件：

```bash
# 上传附件API
curl -X POST http://localhost:8080/upload \
  -F "file=@/path/to/your/file.jpg" \
  -H "Content-Type: multipart/form-data"

# 响应示例
{
  "code": 200,
  "message": "SUCCESS",
  "data": {
    "file_name": "uploaded_image.jpg"
  }
}
```

## 4. 支持的附件类型

OxyGent 支持多种附件类型的处理：

| 附件类型     | 支持格式               | 处理能力       | 示例用途         |
| -------- | ------------------ | ---------- | ------------ |
| **图像文件** | JPG, PNG, GIF, BMP | 图像识别、内容分析  | 图片描述、OCR文字识别 |
| **文档文件** | PDF, DOC, DOCX     | 文本提取、内容分析  | 文档摘要、信息提取    |
| **音频文件** | MP3, WAV, M4A      | 语音转文字、内容理解 | 会议记录、音频分析    |
| **视频文件** | MP4, AVI, MOV      | 视频帧分析、内容识别 | 视频摘要、场景识别    |

## 5. 完整的可运行样例

以下是附件处理的完整代码示例（参考 [`DemoAttachment.java`](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoAttachment.java)）：

```java
package com.jd.oxygent.examples.attachment;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 附件处理演示类
 * 展示了如何配置和使用OxyGent处理各种类型的附件
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DemoAttachment {

    @OxySpaceBean(value = "attachmentJavaOxySpace", defaultStart = true, query = "Introduce the content of the file")
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
                // 1. HTTP LLM Configuration - corresponds to Python version's oxy.HttpLLM, with concurrency limits
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .isMultimodalSupported(true)
                        .timeout(30)
                        .build(),

                // 2. Chat Agent - corresponds to Python version's oxy.ChatAgent, with concurrency limits
                ChatAgent.builder()
                        .name("qa_agent")
                        .llmModel("default_llm")
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

## 6. 高级附件处理功能

### 批量附件处理

处理多个附件的示例：

```java
// 在Web界面或API中，可以同时上传多个附件
// 智能体会自动处理每个附件并生成综合分析报告

Map<String, Object> batchRequest = Map.of(
    "query", "Please analyze all uploaded files and create a comprehensive report",
    "attachments", Arrays.asList(
        "document1.pdf",
        "image1.jpg",
        "audio1.mp3"
    )
);
```

### 附件内容搜索

基于附件内容进行搜索和查询：

```java
// 智能体可以在已处理的附件中搜索信息
String searchQuery = "Find all references to 'machine learning' in the uploaded documents";
```

### 附件转换和处理

智能体可以执行各种附件转换操作：

- 图片格式转换和优化
- PDF文档的文本提取
- 音频转文字转录
- 视频关键帧提取

## 7. 如何运行此示例

你可以通过以下方式运行附件处理示例：

**参考现有示例**: [`DemoAttachment.java`](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoAttachment.java)

## 8. 最佳实践

### 安全性考虑

- 限制上传文件的大小和类型
- 对用户上传的文件进行安全检查
- 避免处理可执行文件

### 性能优化

- 合理设置 `maxImagePixels` 避免处理过大图片
- 使用适当的 `timeout` 设置
- 对大文件进行分块处理

### 用户体验

- 提供文件上传进度提示
- 支持拖拽上传
- 显示文件处理状态

## 9. 常见问题

**Q: 支持哪些文件格式？**
A: 主要支持图片(JPG/PNG/GIF)、PDF文档、音频(MP3/WAV)等常见格式。

**Q: 文件大小限制是多少？**
A: 默认限制可通过 `maxImagePixels` 等参数配置，建议图片不超过10MB。

**Q: 如何处理多语言文档？**
A: 多模态LLM通常支持多语言，可以自动识别和处理不同语言的内容。

[上一章：使用多模态智能体](./10-01-multimodal.md)
[下一章：创建分布式系统](./11-01-distributed.md)
[回到首页](../../README_zh.md)