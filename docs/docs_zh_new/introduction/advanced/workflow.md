# 如何使用工作流？

## 简介

OxyGent4J 支持以外部工作流控制智能体的工作次序。通过 `WorkflowAgent`，您可以定义自定义的执行逻辑，精细控制多个智能体之间的调用顺序和数据流转。

## 简单实例

使用 `WorkflowAgent.builder()` 创建一个工作流智能体，通过 `funcWorkflow` 指定执行函数：

```java
WorkflowAgent.builder()
    .name("order_workflow")
    .funcWorkflow((oxyRequest) -> {
        // Step 1: 验证订单
        String orderId = (String) oxyRequest.getArguments().get("orderId");
        if (!validateOrder(orderId)) {
            throw new IllegalArgumentException("订单验证失败");
        }
        // Step 2: 处理支付
        OxyResponse payResp = oxyRequest.call(
            Map.of("callee", "payment_agent",
                   "arguments", Map.of("query", "处理订单 " + orderId)));
        return "订单处理完成: " + payResp.getOutput();
    })
    .build();
```

## 构建 Workflow

Workflow 是一种精细控制的方法，下面逐步演示如何构建一个可运行的工作流。

### 假设的工作需求

> 为用户输入的文档写一段总结，并将带时间的总结存储在文件里。

可以将工作流拆分为：

1. 获取时间（不需要原始输入）
2. 分析文档（需要用户原始输入）
3. 写入文件（需要前两步的输出）

### 将步骤转化为代码

每个步骤通过 `oxyRequest.call()` 调用子智能体，传入 `callee`（目标名称）和 `arguments`（参数）：

```java
// Step 1: 获取时间
OxyResponse timeResp = oxyRequest.call(
    Map.of("callee", "time_agent",
           "arguments", new HashMap<>(Map.of("query", "现在的北京时间是？"))));
String currentTime = timeResp.getOutput().toString();

// Step 2: 分析文档
OxyResponse analysisResp = oxyRequest.call(
    Map.of("callee", "analyzer",
           "arguments", new HashMap<>(Map.of("query", "请分析文档：" + oxyRequest.getQuery()))));

// Step 3: 写入文件
String finalContent = String.format("时间：%s\n分析：%s", currentTime, analysisResp.getOutput());
oxyRequest.call(Map.of("callee", "file_agent",
    "arguments", new HashMap<>(Map.of("query", "写入 output.txt：\n" + finalContent))));
```

## 完整示例

```java
@OxySpaceBean(value = "demoWorkflow", defaultStart = true, query = "分析这篇文档")
public static List<BaseOxy> getDefaultOxySpace() {
    return Arrays.asList(
        HttpLlm.builder()
            .name("default_llm")
            .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
            .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
            .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
            .build(),

        new StdioMCPClient("time_tools", "uvx",
            List.of("mcp-server-time", "--local-timezone=Asia/Shanghai")),

        ChatAgent.builder()
            .name("analyzer")
            .prompt("You are a document analyzer. Summarize the given text.")
            .build(),

        ReActAgent.builder()
            .name("time_agent")
            .desc("获取当前时间")
            .tools(List.of("time_tools"))
            .llmModel("default_llm")
            .build(),

        WorkflowAgent.builder()
            .isMaster(true)
            .name("workflow_agent")
            .permittedToolNameList(new ArrayList<>(List.of("time_agent", "analyzer")))
            .funcWorkflow((oxyRequest) -> {
                // Step 1: 获取时间
                OxyResponse timeResp = oxyRequest.call(
                    Map.of("callee", "time_agent",
                           "arguments", new HashMap<>(Map.of("query", "现在的北京时间是？"))));
                String currentTime = timeResp.getOutput().toString();

                // Step 2: 分析文档
                String userQuery = oxyRequest.getQuery();
                OxyResponse analysisResp = oxyRequest.call(
                    Map.of("callee", "analyzer",
                           "arguments", new HashMap<>(Map.of("query", userQuery))));

                return String.format("时间：%s\n分析结果：%s", currentTime, analysisResp.getOutput());
            })
            .build()
    );
}
```

## WorkflowAgent 核心机制

| 属性 | 类型 | 说明 |
|------|------|------|
| `funcWorkflow` | `Function<OxyRequest, Object>` | 工作流执行函数 |
| `permittedToolNameList` | `List<String>` | 允许调用的子智能体/工具列表 |
| `isMaster` | `boolean` | 是否为入口智能体 |

工作流函数返回值将作为 `OxyResponse.output` 返回给调用方。若函数抛出异常，状态为 `FAILED`；正常返回则状态为 `COMPLETED`。

## 条件分支

在 `funcWorkflow` 中可使用标准 Java 条件逻辑实现分支：

```java
.funcWorkflow((oxyRequest) -> {
    String query = oxyRequest.getQuery();
    if (query.contains("时间")) {
        return oxyRequest.call(Map.of("callee", "time_agent",
                "arguments", new HashMap<>(Map.of("query", query))));
    } else {
        return oxyRequest.call(Map.of("callee", "analyzer",
                "arguments", new HashMap<>(Map.of("query", query))));
    }
})
```

---

[上一章：信任模式](./trust-mode.md)
[下一章：继续执行](./continue-exec.md)
[回到首页](../readme.md)
