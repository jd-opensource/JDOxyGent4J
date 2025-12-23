# 如何使用工作流？

## 简单实例

OxyGent支持以外部工作流控制智能体的工作次序。您可以通过在工作流中使用 `call` 方法指定智能体的任务执行顺序。例如，在 Java 示例中，我们使用工作流确保智能体在计算 Pi 之前首先查询时间：

```java
// Java工作流实现需要继承BaseFlow类
@Component
public class CustomWorkflow extends BaseFlow {

    @Override
    protected OxyResponse execute(OxyRequest oxyRequest) throws Exception {
        var shortMemory = oxyRequest.getShortMemory();
        System.out.println("--- History record --- : " + shortMemory);

        var masterShortMemory = oxyRequest.getShortMemory(true);
        System.out.println("--- History record-User layer --- : " + masterShortMemory);
        System.out.println("user query: " + oxyRequest.getQuery(true));

        oxyRequest.sendMessage("msg");

        var timeResponse = oxyRequest.call(
                "time_agent",
                Map.of("query", "What time is it now in Asia/Shanghai?")
        );
        System.out.println("--- Current time --- : " + timeResponse.getOutput());

        var llmResponse = oxyRequest.call(
                "default_llm",
                Map.of(
                    "messages", Arrays.asList(
                        Map.of("role", "system", "content", "You are a helpful assistant."),
                        Map.of("role", "user", "content", "Hello!")
                    ),
                    "llm_params", Map.of("temperature", 0.6)
                )
        );
        System.out.println(llmResponse.getOutput());

        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(oxyRequest.getQuery());
        List<String> numbers = new ArrayList<>();
        while (matcher.find()) {
            numbers.add(matcher.group());
        }

        if (!numbers.isEmpty()) {
            var n = numbers.get(numbers.size() - 1);
            var piResponse = oxyRequest.call("calc_pi", Map.of("prec", n));
            return new OxyResponse(String.format("Save %s positions: %s", n, piResponse.getOutput()));
        } else {
            return new OxyResponse("Save 2 positions: 3.14, or you could ask me to save how many positions you want.");
        }
    }
}
```

在此工作流中，我们先查询时间，再进行文档分析，并最终保存计算结果。工作流需要一个上层的 Agent 进行执行，您可以使用 `WorkflowAgent` 来控制工作流：

```java
WorkflowAgent.builder()
    .name("math_agent")
    .desc("A tool for pi query")
    .subAgents(Arrays.asList("time_agent"))
    .tools(Arrays.asList("math_tools"))
    .funcWorkflow(new CustomWorkflow())
    .isRetainMasterShortMemory(true)
    .build()
```

完整的样例请参考[`DemoWorkflowAgent.java`](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoWorkflowAgent.java)。

## 构建 Workflow

Workflow是一种非常精细的方法，下面将以[如何自定义处理提示词？](./08-04-update_prompts.md)中的例子入手，逐步写一个可以运行的workflow。

### 假设的工作需求

假设我们的工作需求是：

> 为用户输入的文档写一段总结，并将带时间的总结存储在 `output.txt` 文件里。

可以将工作流拆分为如下步骤：

1. 获取时间（不需要原始输入）
2. 分析文档（需要用户原始输入）
3. 写入文件（需要前两步的输出）

### 将步骤转化为代码

#### 获取时间（不需要原始输入）

```java
var timeResp = oxyRequest.call(
    "time_agent", Map.of("query", "现在的北京时间是？")
);
String currentTime = timeResp.getOutput();
```

#### 分析文档（需要用户原始输入）

```java
// 使用getQuery获取用户原始输入
String userQuery = oxyRequest.getQuery(true);

var analysisResp = oxyRequest.call(
    "analyzer",
    Map.of("query", "请分析文档：" + userQuery)
);
String analysisResult = analysisResp.getOutput();
```

#### 写入文件（需要前两步的输出）

```java
String finalContent = String.format("时间：%s\n\n分析结果：%s", currentTime, analysisResult);
var fileResp = oxyRequest.call(
    "file_agent",
    Map.of("query", "请将以下内容写入 output.txt：\n" + finalContent)
);
```

### 包装一个workflow

将上述步骤按照顺序包装成一个工作流，需要传入一个 `OxyRequest` 对象作为参数：

```java
public class DocumentAnalysisWorkflow extends BaseFlow {

    @Override
    protected OxyResponse execute(OxyRequest oxyRequest) throws Exception {
        // Step 1: 获取时间
        var timeResp = oxyRequest.call(
            "time_agent", Map.of("query", "现在的北京时间是？")
        );
        String currentTime = timeResp.getOutput();
        System.out.println("== 当前时间 ==\n" + currentTime);

        // 后续的steps...
        return new OxyResponse("流程完成，output.txt 写入成功");
    }
}
```

### 指定一个调用workflow的agent

通过 `WorkflowAgent` 控制整个工作流，并指定其调用的 subagent 和所需工具：

```java
WorkflowAgent.builder()
    .name("workflow_agent")
    .desc("时间获取 + 文档分析 + 写入文件的工作流")
    .subAgents(Arrays.asList("file_agent", "time_agent", "analyzer"))
    .funcWorkflow(new DocumentAnalysisWorkflow())
    .llmModel("default_llm")
    .build(),

ReActAgent.builder()
    .name("master_agent")
    .isMaster(true)
    .subAgents(Arrays.asList("workflow_agent"))
    .build()
```

预期的输出结果是：

```markdown
时间：当前的北京时间是2025年7月25日09:27:01。

分析结果：Based on the parallel execution of the tasks, the following summary has been compiled and stored in the `output.txt` file:

---

**当前时间：2023-12-05 10:00:00**

**总结：**

...

---

以上总结已存储在`output.txt`文件中。
```

## 完整的可运行样例

以下是可运行的完整代码示例：

```java
package com.jd.oxygent.examples.workflow;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.ParallelAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.WorkflowAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.tools.PresetTools;
import com.jd.oxygent.core.oxygent.schemas.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.OxyResponse;
import com.jd.oxygent.core.oxygent.flows.BaseFlow;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

/**
 * 演示如何使用工作流(Workflow)功能
 * 展示了时间获取、文档分析和文件写入的完整工作流程
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DemoWorkflowExample {

    /**
     * 自定义工作流：时间获取 + 文档分析 + 写入文件
     */
    private static final Function<OxyRequest, Object> documentAnalysisWorkflow = oxyRequest -> {
            // Step 1: 获取时间
            var timeResp = oxyRequest.call(
                "time_agent", Map.of("query", "现在的北京时间是？")
            );
            String currentTime = timeResp.getOutput();
            System.out.println("== 当前时间 ==\n" + currentTime);

            // Step 2: 获取用户原始 markdown 文件 query
            String userQuery = oxyRequest.getQuery(true);

            // Step 3: 分析文档（保留原始 query 作为文件路径）
            var analysisResp = oxyRequest.call(
                "analyzer",
                Map.of("query", "请分析文档：" + userQuery)
            );
            String analysisResult = analysisResp.getOutput();
            System.out.println("== 分析结果 ==\n" + analysisResult);

            // Step 4: 写入文件
            String finalContent = String.format("时间：%s\n\n分析结果：%s", currentTime, analysisResult);
            var fileResp = oxyRequest.call(
                "file_agent",
                Map.of("query", "请将以下内容写入 output.txt：\n" + finalContent)
            );
            System.out.println("== 写入文件结果 ==\n" + fileResp.getOutput());
            return "流程完成，output.txt 写入成功";
    }

    /**
     * 获取包含工作流的OxySpace配置
     *
     * @return 包含LLM、工具、代理和工作流的BaseOxy列表
     */
    @OxySpaceBean(value = "workflowOxySpace", defaultStart = true, query = "Hello!")
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
                // HTTP LLM配置
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .llmParams(Map.of("temperature", 0.01))
                        .semaphoreCount(4)
                        .timeout(240)
                        .build(),

                // 时间工具MCP客户端
                new StdioMCPClient("time_tools", "uvx",
                        Arrays.asList("mcp-server-time", "--local-timezone=Asia/Shanghai")),

                // 预设文件工具
                PresetTools.FILE_TOOLS,

                // 文件代理
                ReActAgent.builder()
                        .name("file_agent")
                        .desc("A tool that can operate the file system")
                        .tools(Arrays.asList("file_tools"))
                        .build(),

                // 时间代理
                ReActAgent.builder()
                        .name("time_agent")
                        .desc("A tool that can get current time")
                        .tools(Arrays.asList("time_tools"))
                        .build(),

                // 文本总结代理
                ChatAgent.builder()
                        .name("text_summarizer")
                        .desc("A tool that can summarize markdown text")
                        .prompt("You are a text summarizer that creates concise summaries of markdown content.")
                        .build(),

                // 数据分析代理
                ChatAgent.builder()
                        .name("data_analyser")
                        .desc("A tool that can summarize echart data")
                        .prompt("You are a data analyst that can analyze chart and visualization data.")
                        .build(),

                // 文档检查代理
                ChatAgent.builder()
                        .name("document_checker")
                        .desc("文档校验器")
                        .prompt("You are a document checker that finds problems in documents.")
                        .build(),

                // 并行分析代理
                ParallelAgent.builder()
                        .name("analyzer")
                        .desc("A tool that analyze markdown document")
                        .permittedToolNameList(Arrays.asList("text_summarizer", "data_analyser", "document_checker"))
                        .build(),

                // 工作流代理
                WorkflowAgent.builder()
                        .name("workflow_agent")
                        .desc("时间获取 + 文档分析 + 写入文件的工作流")
                        .subAgents(Arrays.asList("file_agent", "time_agent", "analyzer"))
                        .funcWorkflow(documentAnalysisWorkflow )
                        .llmModel("default_llm")
                        .build(),

                // 主代理
                ReActAgent.builder()
                        .name("master_agent")
                        .isMaster(true)
                        .subAgents(Arrays.asList("workflow_agent"))
                        .build()
        );
    }

    /**
     * 应用程序主入口点
     * 启动Spring Boot应用并初始化工作流集成
     *
     * @param args 命令行参数
     * @throws Exception 当应用启动失败时抛出异常
     */
    public static void main(String[] args) throws Exception {
        var currentClassName = Thread.currentThread().getStackTrace()[1].getClassName();
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(currentClassName);
        ServerApp.main(args);
    }
}
```

## 自定义输入Schema工作流

OxyGent支持为WorkflowAgent定义自定义输入Schema，确保工作流接收到正确格式的参数（参考 [DemoCustomAgentInputSchema](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/advanced/DemoCustomAgentInputSchema.java)）：

```java
WorkflowAgent.builder()
    .name("math_agent")
    .desc("A tool for pi query")
    .inputSchema(Map.of(
        "required", List.of("query", "precision"),
        "properties", Map.of(
            "query", Map.of("description", "Query question"),
            "precision", Map.of("description", "How many decimal places are there")
        )
    ))
    .funcWorkflow((oxyRequest) -> {
        System.out.println(oxyRequest.getQuery());
        String PI = "3.141592653589793238462643383279502884197169399375105820974944592307816406286208998";
        try {
            int precision = Integer.parseInt((String)oxyRequest.getArguments().get("precision"));
            if (precision <= 0 || precision >= PI.length()) {
                return PI; // 返回完整的PI
            }
            return PI.substring(0, precision);
        } catch (NumberFormatException e) {
            return PI; // 如果参数不是数字，返回完整的PI
        }
    })
    .build()
```

### inputSchema的优势：

- **参数验证**: 确保传入参数符合预期格式
- **类型检查**: 明确参数类型和必需性
- **文档化**: 自动生成API文档
- **错误提示**: 提供清晰的参数错误信息

该示例演示了：

- 使用`WorkflowAgent`创建自定义工作流
- Lambda函数式工作流定义（`funcWorkflow`）
- 工作流中调用LLM、Agent和工具
- 内存管理和消息传递
- Pi计算的完整工作流程

**运行前准备**:

- 确保数学工具可用
- 工作流会自动处理数据流传递

## 参考现有示例:

- [DemoWorkflowAgent.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoWorkflowAgent.java) - 基础工作流使用
- [DemoCustomAgentInputSchema.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/advanced/DemoCustomAgentInputSchema.java) - 自定义输入Schema工作流

[上一章：反思重做模式](./08-03-reflexion.md)
[下一章：创建流](./09-02-preset_flow.md)
[回到首页](./readme.md)