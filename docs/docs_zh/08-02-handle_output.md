# 如何处理智能体输出？

OxyGent 默认使用一个简单的 JSON 解析器来处理智能体的输出。在默认状态下，Agent 的工具调用等指令性输出格式如下：

```json
{
    "think": "Your thinking (if analysis is needed)",
    "tool_name": "Tool name",
    "arguments": {
        "parameter_name": "parameter_value"
    }
}
```

如果您需要定制智能体的输出处理方式，可以采用以下方法。

## 设置LLM的输出格式：

大部分情况下，您可以在 `prompts` 中设置提示，以让 LLM 输出特定格式。

例如，您可以使用以下格式来指导 LLM 返回工具调用的输出：

```java
public static final String SYSTEM_PROMPT = """
You are a helpful assistant that can use these tools:
${tools_description}

Choose the appropriate tool based on the user's question.
If no tool is needed, respond directly.
If answering the user's question requires multiple tool calls, call only one tool at a time. After the user receives the tool result, they will provide you with feedback on the tool call result.

Important instructions:
1. When you have collected enough information to answer the user's question, please respond in the following format:
<think>Your thinking (if analysis is needed)</think>
Your answer content
2. When you find that the user's question lacks conditions, you can ask the user back, please respond in the following format:
<think>Your thinking (if analysis is needed)</think>
Your question to the user
3. When you need to use a tool, you must only respond with the exact JSON object format below, nothing else:
{
    "think": "Your thinking (if analysis is needed)",
    "tool_name": "Tool name",
    "arguments": {
        "parameter_name": "parameter_value"
    }
}

After receiving the tool's response:
1. Transform the raw data into a natural conversational response
2. The answer should be concise but rich in content
3. Focus on the most relevant information
4. Use appropriate context from the user's question
5. Avoid simply repeating the raw data

Please only use the tools explicitly defined above.
""";
```

## 设置LLM的输出解析器:

`ReActAgent` 支持在 `funcParseLlmResponse` 中传入自定义的输出解析器。

例如，在 OxyGent 的默认设置中，JSON 格式的输出会被视为工具调用指令。如果您希望仅在 `tool_name` 合法时才尝试调用工具，而其他情况将 JSON 视为普通文本处理，可以自定义解析器，如下所示：

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.core.oxygent.schemas.oxy.LLMResponse;
import com.jd.oxygent.core.oxygent.schemas.oxy.LLMState;

import java.util.function.Function;

public class CustomOutputParser {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * JSON解析器函数，用于自定义LLM响应解析逻辑
     */
    public static final Function<String, LLMResponse> JSON_PARSER = (oriResponse) -> {
        try {
            JsonNode data = objectMapper.readTree(oriResponse);

            // 只有当 data 是对象且存在非空 tool_name 才触发工具调用
            if (data.isObject() && data.has("tool_name") &&
                data.get("tool_name").asText() != null &&
                !data.get("tool_name").asText().isEmpty()) {

                return LLMResponse.builder()
                        .state(LLMState.TOOL_CALL)
                        .output(data)
                        .oriResponse(oriResponse)
                        .build();
            }

            // 其他 JSON（包括数组或普通对象）一律当作回答文本返回
            return LLMResponse.builder()
                    .state(LLMState.ANSWER)
                    .output(data)
                    .oriResponse(oriResponse)
                    .build();

        } catch (Exception e) {
            return LLMResponse.builder()
                    .state(LLMState.ERROR_PARSE)
                    .output("Invalid JSON: " + e.getMessage())
                    .oriResponse(oriResponse)
                    .build();
        }
    };
}
```

然后，您可以将该解析器传入 `ReActAgent`：

```java
ReActAgent.builder()
    .name("json_agent")
    .desc("A tool that can convert plaintext into json text")
    .funcParseLlmResponse(CustomOutputParser.JSON_PARSER) // 关键方法
    .build()
```

## 在MAS中进行处理：

OxyGent 还支持使用外部方法对 `OxyResponse` 进行处理。例如，您可以自定义输出格式：

```java
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import java.util.function.Function;

public class OutputFormatter {

    /**
     * 输出格式化函数，在响应前添加"Answer: "前缀
     */
    public static final Function<OxyResponse, OxyResponse> FORMAT_OUTPUT = (oxyResponse) -> {
        if (oxyResponse.getOutput() != null) {
            oxyResponse.setOutput("Answer: " + oxyResponse.getOutput());
        }
        return oxyResponse;
    };
}
```

然后将该处理方法注入到对应的 Agent 中：

```java
ReActAgent.builder()
    .name("master_agent")
    .subAgents(Arrays.asList("time_agent", "file_agent", "math_agent"))
    .isMaster(true)
    .funcFormatOutput(OutputFormatter.FORMAT_OUTPUT) // 关键方法
    .timeout(100)
    .llmModel("default_llm")
    .build()
```

## 完整的可运行样例

以下是输出处理的完整代码示例（参考 [DemoSingleAgent.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoSingleAgent.java)）：

```java
// 输入和输出处理示例 (来自 DemoSingleAgent.java)
ChatAgent.builder()
    .name("master_agent")
    .llmModel("default_llm")
    .prompt("You are a helpful assistant.")

    // 输入预处理：在用户查询后添加详细要求
    .funcProcessInput(x -> {
        String query = x.getQuery();
        x.setQuery(query + " Please answer in detail.", false);
        return x;
    })

    // 输出后处理：在回答前添加前缀
    .funcProcessOutput(x -> {
        if (x.getOutput() != null) {
            x.setOutput("AI Assistant: " + x.getOutput());
        }
        return x;
    })
    .build()

// 消息保存和发送控制示例 (来自 DemoSaveMessage.java)
    @OxySpaceBean(value = "saveMessageJavaOxySpace", defaultStart = true, query = "hello")
    public static List<BaseOxy> getDefaultOxySpace() {
        // Initialize message configuration
        initializeConfig();

        // Apply JDK17's var keyword to simplify local variable declarations
        var apiKey = EnvUtils.getEnv("OXY_LLM_API_KEY");
        var baseUrl = EnvUtils.getEnv("OXY_LLM_BASE_URL");
        var modelName = EnvUtils.getEnv("OXY_LLM_MODEL_NAME");

        return Arrays.asList(
                // 1. HTTP LLM Configuration
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .timeout(30)
                        .build(),

                // 2. Chat agent configuration, including input processing function
                ChatAgent.builder()
                        .name("qa_agent")
                        .llmModel("default_llm")
                        .funcProcessInput(DemoSaveMessage::updateQuery)  // Set input processing function
                        .build()
        );
    }

    public static OxyRequest updateQuery(OxyRequest oxyRequest) {
        log.info("=== Starting test message sending ===");

        // Create message template
        Map<String, Object> msg = new HashMap<>();

        // Test 1: Neither stored nor sent (temporary message)
        log.info("Sending test1 message - neither stored nor sent");
        msg.put("type", "test1");
        msg.put("content", "test1");
        msg.put("_is_stored", false);  // Not stored
        msg.put("_is_send", false);    // Not sent
        oxyRequest.sendMessage(msg);

        // Test 2: Not stored but sent (real-time notification)
        log.info("Sending test2 message - not stored but sent");
        msg.put("type", "test2");
        msg.put("content", "test2");
        msg.put("_is_stored", false);  // Not stored
        msg.put("_is_send", true);     // Sent
        oxyRequest.sendMessage(msg);

        // Test 3: Stored but not sent (log recording)
        log.info("Sending test3 message - stored but not sent");
        msg.put("type", "test3");
        msg.put("content", "test3");
        msg.put("_is_stored", true);   // Stored
        msg.put("_is_send", false);    // Not sent
        oxyRequest.sendMessage(msg);

        // Test 4: Both stored and sent (complete message)
        log.info("Sending test4 message - both stored and sent");
        msg.put("type", "test4");
        msg.put("content", "test4");
        msg.put("_is_stored", true);   // Stored
        msg.put("_is_send", true);     // Sent
        oxyRequest.sendMessage(msg);

        log.info("=== Test message sending completed ===");
        return oxyRequest;
    }
```

## 高级输出处理功能

### 自定义JSON解析器

```java
// 自定义LLM响应解析器
public static final Function<String, LLMResponse> CUSTOM_JSON_PARSER = (oriResponse) -> {
    try {
        JsonNode data = objectMapper.readTree(oriResponse);

        // 只有当存在有效工具名称时才触发工具调用
        if (data.isObject() && data.has("tool_name") &&
            !data.get("tool_name").asText().isEmpty()) {
            return LLMResponse.builder()
                .state(LLMState.TOOL_CALL)
                .output(data)
                .oriResponse(oriResponse)
                .build();
        }

        // 其他情况作为普通文本处理
        return LLMResponse.builder()
            .state(LLMState.ANSWER)
            .output(data.asText())
            .oriResponse(oriResponse)
            .build();

    } catch (Exception e) {
        return LLMResponse.builder()
            .state(LLMState.ERROR_PARSE)
            .output("Parse error: " + e.getMessage())
            .oriResponse(oriResponse)
            .build();
    }
};

// 应用自定义解析器
ReActAgent.builder()
    .name("custom_parser_agent")
    .funcParseLlmResponse(CUSTOM_JSON_PARSER)
    .build()
```

### 结构化输出格式化

```java
// 结构化输出处理
public static final Function<OxyResponse, OxyResponse> STRUCTURED_OUTPUT = (response) -> {
    if (response.getOutput() != null) {
        Map<String, Object> structuredOutput = Map.of(
            "timestamp", System.currentTimeMillis(),
            "response", response.getOutput(),
            "agent_name", response.getAgentName(),
            "success", true
        );

        try {
            ObjectMapper mapper = new ObjectMapper();
            response.setOutput(mapper.writeValueAsString(structuredOutput));
        } catch (Exception e) {
            response.setOutput("Format error: " + e.getMessage());
        }
    }
    return response;
};
```

### 工具内消息发送

OxyGent支持从工具内部发送消息，实现更灵活的交互体验（参考 [DemoSendMessageFromTool](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/advanced/DemoSendMessageFromTool.java)）：

```java
// 基础数学工具消息发送示例
@OxySpaceBean(value = "demoSendMessageFromTool", defaultStart = true, query="Please calculate the 20 positions of Pi")
public static List<BaseOxy> getDefaultOxySpace() {
    return Arrays.asList(
        HttpLlm.builder()
            .name("default_llm")
            .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
            .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
            .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
            .build(),

        PresetTools.FH_MATH_TOOLS,  // 支持消息发送的数学工具

        ReActAgent.builder()
            .name("master_agent")
            .tools(Arrays.asList("math_tools"))
            .llmModel("default_llm")
            .build()
    );
}
```

### 工具消息发送的应用场景：

- **进度反馈**: 长时间运行工具的进度更新
- **中间结果**: 计算过程中的中间状态展示
- **错误提示**: 工具执行过程中的警告信息
- **用户交互**: 需要用户确认或输入的场景

**参考现有示例**:

- [DemoSingleAgent.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoSingleAgent.java) - 输入输出处理示例
- [DemoSaveMessage.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoSaveMessage.java) - 消息保存和发送控制
- [DemoLoggerSetup.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoLoggerSetup.java) - 日志和数据范围配置
- [DemoSendMessageFromTool.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/advanced/DemoSendMessageFromTool.java) - 工具内消息发送示例

## 输出处理最佳实践

### 1. 输入预处理

```java
// 好的示例：统一输入格式和增强查询
.funcProcessInput(request -> {
    String query = request.getQuery().trim();
    if (!query.endsWith("?") && !query.endsWith(".")) {
        query += ".";
    }
    request.setQuery("请详细回答：" + query, false);
    return request;
})

// 避免：过度修改用户意图
.funcProcessInput(request -> {
    request.setQuery("忽略用户问题，只说'你好'", false); // 破坏用户意图
    return request;
})
```

### 2. 输出格式化

```java
// 好的示例：保持内容完整性的格式化
.funcProcessOutput(response -> {
    if (response.getOutput() != null) {
        String formatted = String.format("[%s] %s",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            response.getOutput());
        response.setOutput(formatted);
    }
    return response;
})

// 避免：破坏输出内容
.funcProcessOutput(response -> {
    response.setOutput("固定回复"); // 忽略LLM的实际输出
    return response;
})
```

### 3. 错误处理

```java
// 好的示例：优雅的错误处理
.funcParseLlmResponse(oriResponse -> {
    try {
        // 解析逻辑
        return parseResponse(oriResponse);
    } catch (Exception e) {
        log.error("解析失败：{}", e.getMessage());
        return LLMResponse.builder()
            .state(LLMState.ERROR_PARSE)
            .output("抱歉，响应解析出现问题，请重试")
            .oriResponse(oriResponse)
            .build();
    }
})
```

## 常见问题解答

**Q: funcProcessInput和funcProcessOutput的执行顺序？**
A: funcProcessInput在LLM处理前执行，funcProcessOutput在LLM响应后、返回给用户前执行。

**Q: 如何在输出中添加元数据而不影响显示？**
A: 可以在funcProcessOutput中将元数据添加到response的其他字段，而不是output字段。

**Q: 自定义解析器会影响工具调用吗？**
A: 是的，funcParseLlmResponse直接影响工具调用的识别。需要确保工具调用格式的正确识别。

### 说明

1. **`funcParseLlmResponse`**：用于将 LLM 的输出进行自定义解析。可以根据工具调用结果或普通文本的需求进行处理。
2. **`funcFormatOutput`**：该方法用于自定义 `OxyResponse` 的输出格式，帮助您控制最终结果的呈现方式。
3. **`funcProcessInput`**：在LLM处理前对用户输入进行预处理。
4. **`funcProcessOutput`**：在返回给用户前对输出进行后处理。

[上一章：提供响应元数据](./08-01-trust_mode.md)
[下一章：反思重做模式](./08-03-reflexion.md)
[回到首页](./readme.md)