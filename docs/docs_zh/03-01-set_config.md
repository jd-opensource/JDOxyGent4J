# 如何进行设置？

在 OxyGent 中，您可以使用配置系统来管理您的自定义内容。在Java版本中，这些配置通过application.yml文件和环境变量进行管理。

## 1. 设置 LLM 模型

如果您的多个 Agent 都使用相同的 LLM，您可以在配置文件中设置默认的LLM模型：

```java
// 在application.yml中设置
oxygent:
  agent:
    default-llm-model: "default_llm"
```

或者在代码中动态设置：

```java
// 在构建Agent时指定LLM模型
ReActAgent.builder()
    .name("master_agent")
    .llmModel("default_llm")
    .build();
```

## 2. 设置模型参数

您可以在创建LLM实例时设置模型的参数。例如，设置温度、最大 token 数量和 top-p：

```java
HttpLlm.builder()
    .name("default_llm")
    .llmParams(Map.of(
        "temperature", 0.2,
        "max_tokens", 2048,
        "top_p", 0.9
    ))
    .build();
```

## 3. 设置日志格式

您可以通过配置文件设置日志记录的详细信息：

```java
// 在application.yml中设置日志配置
logging:
  level:
    com.jd.oxygent: DEBUG
  file:
    path: ./cache_dir/demo.log
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

oxygent:
  logging:
    color:
      tool-call: "MAGENTA"
      observation: "GREEN"
    detailed:
      tool-call: true
      observation: true
```

## 4. 设置智能体输入格式

您可以通过Schema定义来设置智能体的输入格式：

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// 定义输入Schema
var inputSchema = Map.of(
    "properties", Map.of(
        "query", Map.of("description", "Query question"),
        "path", Map.of("description", "File path to save the result")
    ),
    "required", Arrays.asList("query")
);

// 在Agent配置中使用
ReActAgent.builder()
    .name("master_agent")
    .inputSchema(inputSchema)
    .build();
```

## 5. 设置结果输出格式

您可以在配置文件中设置结果的输出格式：

```java
// 在application.yml中设置
oxygent:
  message:
    send-tool-call: false
    send-observation: false
    send-think: false
    send-answer: true
```

## 完整的可运行样例

以下是可运行的完整代码示例（参考 [`DemoConfig.java`](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoConfig.java)）：

```java
import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.*;
import com.jd.oxygent.core.oxygent.tools.PresetTools;

import java.util.Arrays;
import java.util.Map;

public class ConfigExample {
    public static void main(String[] args) {
        var oxySpace = Arrays.asList(
            HttpLlm.builder()
                .name("default_llm")
                .apiKey(System.getenv("OXY_LLM_API_KEY"))
                .baseUrl(System.getenv("OXY_LLM_BASE_URL"))
                .modelName(System.getenv("OXY_LLM_MODEL_NAME"))
                .llmParams(Map.of("temperature", 0.01))
                .semaphoreCount(4)
                .timeout(240)
                .build(),

            PresetTools.FILE_TOOLS,

            ReActAgent.builder()
                .name("master_agent")
                .isMaster(true)
                .llmModel("default_llm")
                .tools(Arrays.asList("file_tools"))
                .build()
        );

        var mas = new Mas("config_app", oxySpace);

        try {
            mas.startWebService("Hello!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```



**参考现有示例**: [`DemoConfig.java`](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoConfig.java)

[上一章：管理工具调用](./02-02-manage_tools.md)
[下一章：设置数据库](./03-02-set_database.md)
[回到首页](../../README_zh.md)