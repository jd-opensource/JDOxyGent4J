# 如何并行运行智能体？

OxyGent 支持高兼容性的并行执行功能，允许您同时运行多个智能体并进行协作。

## 1. 并行执行多个智能体

例如，如果您需要同时对一篇文档进行数据分析、文字总结和纠错，您可以注册相应功能的智能体，并使用 `ParallelAgent` 来管理这些智能体。`ParallelAgent` 会负责并行处理并汇总各个智能体的结果。


```java
// 需要并行的agent
ChatAgent.builder()
    .name("text_summarizer")
    .desc("A tool that can summarize markdown text")
    .prompt("请对输入的markdown文本进行总结，提取关键信息和要点。")
    .build(),

ChatAgent.builder()
    .name("data_analyser")
    .desc("A tool that can summarize echart data")
    .prompt("请分析输入数据中的图表和数据，提供数据洞察和分析结果。")
    .build(),

ChatAgent.builder()
    .name("document_checker")
    .desc("A tool that can find problems in document")
    .prompt("请检查文档中的问题，包括格式错误、逻辑不一致等问题。")
    .build(),

// 管理的上层agent
ParallelAgent.builder()
    .name("analyzer")
    .desc("A tool that analyze markdown document")
    .permittedToolNameList(Arrays.asList("text_summarizer", "data_analyser", "document_checker"))
    .build()
```

`ParallelAgent` 会自动启动所有子智能体，进行并行计算，并最终返回所有任务的结果。

## 2. 同一智能体并行执行

如果您需要使同一个智能体并行运行多次，可以使用 `startBatchProcessing` 方法来批量处理请求。

### 批量处理和信号量控制

以下是批量处理和信号量控制的完整示例（参考 [DemoBatchAndSemaphore.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoBatchAndSemaphore.java)）：

```java
package com.jd.oxygent.examples.agent;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Collections;

/**
 * 并行处理演示类
 * 展示了如何使用同一智能体并行处理多个请求
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class ParallelProcessingExample {

    /**
     * 获取支持并行处理的OxySpace配置
     *
     * @return 包含高并发LLM和代理的BaseOxy列表
     */
    @OxySpaceBean(value = "parallelProcessingOxySpace", defaultStart = true, query = "Hello!")
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .llmParams(Map.of(
                                "temperature", 0.7,
                                "max_tokens", 512,
                                "chat_template_kwargs", Map.of("enable_thinking", false)
                        ))
                        .semaphoreCount(200) // 支持高并发
                        .build(),

                ReActAgent.builder()
                        .name("master_agent")
                        .isMaster(true)
                        .llmModel("default_llm")
                        .semaphoreCount(200) // 支持高并发
                        .timeout(100)
                        .build()
        );
    }

    /**
     * 演示批量并行处理
     * 批量处理10个相同的请求
     */
    public static void demonstrateBatchProcessing() {
        try {
            var mas = new Mas("parallel_processing_app", getDefaultOxySpace());

            // 创建10个相同的请求进行并行处理
            var requests = Collections.nCopies(10, Map.of("query", "Hello!"));

            // 并行处理所有请求，返回trace ID
            var results = mas.startBatchProcessing(requests, true);

            System.out.println("批量处理结果:");
            for (int i = 0; i < results.size(); i++) {
                System.out.printf("请求 %d 的结果: %s%n", i + 1, results.get(i));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 应用程序主入口点
     * 启动Spring Boot应用并演示并行处理
     *
     * @param args 命令行参数
     * @throws Exception 当应用启动失败时抛出异常
     */
    public static void main(String[] args) throws Exception {
        // 如果是演示模式，运行批量处理演示
        if (args.length > 0 && "demo".equals(args[0])) {
            demonstrateBatchProcessing();
            return;
        }

        // 否则启动Web服务
        var currentClassName = Thread.currentThread().getStackTrace()[1].getClassName();
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(currentClassName);
        ServerApp.main(args);
    }
}

```
### 说明

1. **`startBatchProcessing`**：该方法接收一个包含多个请求的列表，同步并行执行所有请求，并返回结果。如果您希望处理多次相同的请求或不同的请求，可以通过这个方法快速进行批量处理。
2. **`semaphore`**：这是用来控制并发的参数。通过设置适当的并发数，您可以灵活控制系统的资源消耗，避免过多的并行请求导致性能瓶颈。
3. **`returnTraceId=true`**：返回每个请求的 trace ID，便于追踪请求的执行过程和结果。



**参考现有示例**:
- [ParallelDemo.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/ParallelDemo.java) - 基础并行处理示例
- [DemoBatchAndSemaphore.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/backend/DemoBatchAndSemaphore.java) - 批量处理和信号量控制

[上一章：复制相同智能体](./06-02-moa.md)
[下一章：提供响应元数据](./08-01-trust_mode.md)
[回到首页](../../README_zh.md)