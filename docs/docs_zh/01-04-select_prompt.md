# 如何向智能体传递prompt

## 使用自定义prompt

在OxyGent中，您可以通过预设prompt告知agent一些信息。例如：

```java
var textSummarizerPrompt = """
    你是一个文件分析专家，用户会向你提供文档，你需要分析文件中的文字内容，并提供摘要
    """;

var dataAnalyserPrompt = """
    你是一个数据分析专家，需要分析文档中的表格、图表、echart代码等数据，并提供文字版的分析结果。
    """;

var documentCheckerPrompt = """
    你需要查看用户提供的文档，并尝试提出文档内容中存在的问题，例如前后矛盾、错误叙述等，帮助用户进行改进。
    """;
```

之后，您可以在执行脚本中使用`prompt`参数调用prompt：

```java
ChatAgent.builder()
    .name("text_summarizer")
    .desc("A tool that can summarize markdown text")
    .prompt(textSummarizerPrompt)
    .build();

ChatAgent.builder()
    .name("data_analyser")
    .desc("A tool that can summarize echart data")
    .prompt(dataAnalyserPrompt)
    .build();

ChatAgent.builder()
    .name("document_checker")
    .desc("A tool that can find problems in document")
    .prompt(documentCheckerPrompt)
    .build();
```

## 使用系统预设prompt

您也可以使用以下方式调用我们的**默认prompts**：

```java
import com.jd.oxygent.core.oxygent.prompts.Prompts;

// 使用系统预设的 prompt
var intentionPrompt = Prompts.INTENTION_PROMPT;
var systemPrompt = Prompts.SYSTEM_PROMPT;
var systemPromptRetrieval = Prompts.SYSTEM_PROMPT_RETRIEVAL;
var multimodalPrompt = Prompts.MULTIMODAL_PROMPT;
```

> 我们的默认 [Prompts](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/config/Prompts.java)中包含了工具调用格式等关键信息。

> 因此在使用自定义 Prompt 之前，建议您先参考我们提供的默认 [Prompts](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/config/Prompts.java)，以便更好地理解如何解析大模型的输出以及如何进行工具调用或回答处理。

> 我们也提供了传入您自定义解析函数的属性，以便更加灵活地处理输出。具体请您参考[处理智能体输出](./08-02-handle_output.md)。

如果您不对prompts进行任何指定，我们的智能体将默认使用系统prompts。您可以对系统prompts进行追加：

```java
ReActAgent.builder()
    .name("time_agent")
    .desc("A tool for time query.")
    .additionalPrompt("Do not send other information except time.")
    .tools(Arrays.asList("time_tools"))
    .build();
```

## 完整的可运行样例

以下是提示词配置的完整代码示例（参考多个示例文件）：

```java
// 基础提示词配置 (来自 DemoSingleAgent.java)
ChatAgent.builder()
    .name("master_agent")
    .llmModel("default_llm")
    .prompt("You are a helpful assistant.")
    .funcProcessInput(x -> {
        String query = x.getQuery();
        x.setQuery(query + " Please answer in detail.", false);
        return x;
    })
    .build()

// 专业任务提示词配置 (来自 DemoMultimodal.java)
ChatAgent.builder()
    .name("image_analyzer")
    .prompt("You are a helpful assistant. Please describe the content of the image in detail.")
    .build()

ChatAgent.builder()
    .name("content_discriminator")
    .prompt("Please determine whether the following text is a description of the content of the image. If it is, please output 'True', otherwise output 'False'.")
    .build()

// 工具特化提示词配置 (来自层次化智能体示例)
ReActAgent.builder()
    .name("time_agent")
    .desc("Specialized agent for time queries")
    .additionalPrompt("Focus only on time-related tasks.")
    .tools(Arrays.asList("time"))
    .build()
```

## 如何运行提示词配置示例

你可以通过以下方式运行提示词配置示例：

**参考现有示例**:
- [DemoSingleAgent.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoSingleAgent.java) - 基础提示词配置
- [DemoMultimodal.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/advanced/DemoMultimodal.java) - 多模态任务提示词
- [DemoHierarchicalAgents.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoHierarchicalAgents.java) - 多层级Agent提示词配置

## 提示词最佳实践

### 1. 明确角色定位
```java
// 好的示例：明确定义智能体的角色和能力范围
.prompt("You are a data analysis expert specialized in processing CSV files and generating statistical reports.")

// 避免：过于模糊的定义
.prompt("You are helpful.")
```

### 2. 任务具体化
```java
// 好的示例：具体描述期望的行为
.additionalPrompt("Focus only on time-related tasks. Return results in ISO 8601 format.")

// 避免：缺乏具体指导
.additionalPrompt("Do something with time.")
```

### 3. 输出格式规范
```java
// 好的示例：明确输出格式要求
.prompt("Analyze the image and provide results in JSON format: {\"objects\": [], \"text\": \"\", \"analysis\": \"\"}")
```

## 常见问题解答

**Q: 如何动态更新提示词？**

A: 可以通过 `funcProcessInput` 方法在运行时修改用户输入，或使用配置文件动态加载提示词模板。

**Q: 自定义提示词会覆盖系统提示词吗？**

A: 是的，自定义 `prompt` 会完全替换系统提示词。使用 `additionalPrompt` 可以在系统提示词基础上添加额外指导。

**Q: 如何处理多语言提示词？**

A: OxyGent支持任何语言的提示词，LLM会根据提示词语言自动适配响应语言。

[上一章：选择智能体使用的LLM](./01-03-select_llm.md)
[下一章：选择智能体种类](./01-05-select_agent.md)
[回到首页](./readme.md)