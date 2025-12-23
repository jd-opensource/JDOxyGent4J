# 如何让智能体进行反思？

## 使用ReActAgent进行反思

`ReActAgent`支持传入反思函数进行反思。在未达到最大反思次数的情况下，Agent能够根据反思结果进行重做，直到返回要求的结果。

反思函数的形式非常自由，您可以要求对于特定的疑问返回特定的回答，或是要求过滤部分回答。如果反思结果不为`null`，Agent将根据反思进行重做：

```java
import java.util.function.BiFunction;
import java.util.Arrays;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;

/**
 * 自定义反思函数，用于评估响应质量
 * @param response 智能体的响应内容
 * @param oxyRequest 当前请求上下文
 * @return 如果需要重做返回改进建议，否则返回null
 */
public static final BiFunction<String, OxyRequest, String> CUSTOM_REFLEXION = (response, oxyRequest) -> {
    // 基本检查
    if (response == null || response.trim().length() < 5) {
        return "The response is too short or empty. Please provide a more detailed and helpful answer.";
    }

    var query = oxyRequest.getQuery().toLowerCase();
    var lowerResponse = response.toLowerCase();

    // 针对问候查询的自定义业务逻辑检查
    if (query.contains("hello")) {
        // 对于问候查询，期望友好的回应
        var greetingWords = Arrays.asList("hello", "hi", "hey", "greetings", "welcome");
        if (greetingWords.stream().noneMatch(lowerResponse::contains)) {
            return "This is a greeting. Please respond in a more friendly and welcoming manner.";
        }
    }

    if (query.contains("math") || query.contains("calculate")) {
        // 对于数学查询，期望包含数字内容
        if (response.chars().noneMatch(Character::isDigit)) {
            return "This seems to be a math-related question but your answer doesn't contain any numbers. Please provide a numerical answer or calculation.";
        }
    }

    if (query.contains("explain")) {
        // 对于解释请求，期望详细的回应
        if (response.split("\\s+").length < 20) {
            return "The user asked for an explanation, but your response is too brief. Please provide a more detailed explanation.";
        }
    }

    // 检查常见的无效回应
    var unhelpfulPhrases = Arrays.asList(
        "i don't know", "i can't help", "sorry, i cannot", "i'm not sure", "not possible"
    );

    if (unhelpfulPhrases.stream().anyMatch(lowerResponse::contains)) {
        return "Your response seems unhelpful. Please try to provide a more constructive answer or suggest alternative solutions.";
    }

    return null; // 通过验证
};
```

反思函数可以嵌套，如果您希望对数学计算做更严格的反思，比如让Agent输出详细的步骤，可以采取如下方法：

```java
/**
 * 数学专用反思函数
 */
public static final BiFunction<String, OxyRequest, String> MATH_REFLEXION = (response, oxyRequest) -> {
    // 先执行基本检查
    String basicMsg = CUSTOM_REFLEXION.apply(response, oxyRequest);
    if (basicMsg != null) {
        return basicMsg;
    }

    // 数学专用检查
    var query = oxyRequest.getQuery().toLowerCase();
    var mathKeywords = Arrays.asList("calculate", "compute", "solve", "math", "equation");

    if (mathKeywords.stream().anyMatch(query::contains)) {
        // 期望逐步解决方案
        var lowerResponse = response.toLowerCase();
        if (!lowerResponse.contains("step") && !response.contains("=")) {
            return "For mathematical problems, please provide a step-by-step solution showing your work.";
        }
    }

    return null;
};
```

反思需要指定`ReActAgent`执行。值得注意的是，如果您要让Master Agent输出反思后的结果，需要为每一层添加反思。

```java
// 数学智能体配置
ReActAgent.builder()
    .name("math_agent")
    .desc("A specialized agent for mathematical problems with advanced reflexion")
    .llmModel("default_llm")
    .funcReflexion(MATH_REFLEXION) // 关键参数
    .maxReactRounds(30) // 指定最大重做次数
    .build(),

// 主代理，协调其他代理
ReActAgent.builder()
    .name("master_agent")
    .subAgents(Arrays.asList("basic_agent", "smart_agent", "math_agent"))
    .isMaster(true)
    .llmModel("default_llm")
    .funcReflexion(MATH_REFLEXION)
    .build()
```

## 使用流进行反思

我们提供了[流](./09-02-preset_flow.md)`Reflexion`用于一般任务的反思，`MathReflexion`用于计算任务的反思或验算。您可以使用以下的方法调用：

```java
import com.jd.oxygent.core.oxygent.oxy.flows.Reflexion;

// 通用反思流
Reflexion.builder()
    .name("general_reflexion")
    .workerAgent("worker_agent") // 工作智能体
    .reflexionAgent("reflexion_agent") // 反思智能体
    .evaluationTemplate("...") // 反思模板
    .maxReflexionRounds(3) // 反思轮数
    .build(),

// 数学反思流
Reflexion.MathReflexion.builder()
    .name("math_reflexion")
    .workerAgent("worker_agent") // 工作智能体
    .reflexionAgent("reflexion_agent") // 反思智能体
    .evaluationTemplate("...") // 反思模板
    .maxReflexionRounds(3) // 反思轮数
    .build()
```


## 使用工作流进行反思

在一些情况下，您可能希望使用一个智能体而不是固定的方法进行反思。此时您可以指定一个`ChatAgent`或其他类型的Agent进行反思：

```java
// Reflexion Agent - 负责评估答案质量
ChatAgent.builder()
    .name("reflexion_agent")
    .desc("Reflexion agent responsible for evaluating answer quality and providing improvement suggestions")
    .llmModel("default_llm")
    .build()
```

您可以使用一个[工作流](./09-03-workflow.md)管理反思过程。以下展示了利用查询更新进行反思的全流程：

```java
import java.util.function.Function;
import java.util.concurrent.CompletableFuture;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;

/**
 * 反思工作流核心逻辑
 * 实现外部反思过程：
 * 1. 获取用户查询
 * 2. 让worker_agent生成初始答案
 * 3. 让reflexion_agent评估答案质量
 * 4. 如果不满意，提供改进建议并重新生成
 * 5. 返回最终满意的答案
 */
public static final Function<OxyRequest, CompletableFuture<OxyResponse>> REFLEXION_WORKFLOW = (oxyRequest) -> {
    return CompletableFuture.supplyAsync(() -> {
        try {
            // Step 1: 获取原始查询
            String userQuery = oxyRequest.getQuery();
            System.out.println("=== User Query ===\n" + userQuery + "\n");

            int maxIterations = 3;
            int currentIteration = 0;

            while (currentIteration < maxIterations) {
                currentIteration++;
                System.out.println("=== Reflexion Round " + currentIteration + " ===");

                // Step 2: 执行
                var workerArgs = Map.of("query", userQuery);
                var workerResp = oxyRequest.call(Map.of(
                    "callee", "worker_agent",
                    "arguments", workerArgs
                ));
                String workerAnswer = workerResp.getOutput().toString();
                System.out.println("Worker Answer:\n" + workerAnswer + "\n");

                // Step 3: 输入要反思的内容
                String evaluationQuery = String.format("""
                    Please evaluate the quality of the following answer:

                    Original Question: %s

                    Answer: %s

                    Please return evaluation results in the following format:
                    Evaluation Result: [Satisfactory/Unsatisfactory]
                    Evaluation Reason: [Specific reason]
                    Improvement Suggestions: [If unsatisfactory, provide specific improvement suggestions]
                    """, userQuery, workerAnswer);

                var reflexionArgs = Map.of("query", evaluationQuery);
                var reflexionResp = oxyRequest.call(Map.of(
                    "callee", "reflexion_agent",
                    "arguments", reflexionArgs
                ));
                String reflexionResult = reflexionResp.getOutput().toString();
                System.out.println("Reflexion Evaluation:\n" + reflexionResult + "\n");

                // Step 4: 获取反思结果
                if (reflexionResult.contains("Satisfactory") && !reflexionResult.contains("Unsatisfactory")) {
                    System.out.println("=== Reflexion Complete, Answer Quality Satisfactory ===");
                    return OxyResponse.builder()
                        .output(String.format("Final answer optimized through %d rounds of reflexion:\n\n%s",
                                currentIteration, workerAnswer))
                        .build();
                }

                // Step 5: 使用反思结果更新查询
                String improvementSuggestion = "";
                String[] lines = reflexionResult.split("\n");
                for (String line : lines) {
                    if (line.contains("Improvement Suggestions")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) {
                            improvementSuggestion = parts[1].trim();
                            break;
                        }
                    }
                }

                if (!improvementSuggestion.isEmpty()) {
                    userQuery = String.format("%s\n\nPlease note the following improvement suggestions: %s",
                            oxyRequest.getQuery(), improvementSuggestion);
                    System.out.println("Updated query with improvement suggestions:\n" + userQuery + "\n");
                }
            }

            // 如果重做次数用尽，返回当前最好结果
            System.out.println("=== Reached maximum iterations (" + maxIterations + "), returning current best answer ===");
            var finalArgs = Map.of("query", oxyRequest.getQuery());
            var finalResp = oxyRequest.call(Map.of(
                "callee", "worker_agent",
                "arguments", finalArgs
            ));

            return OxyResponse.builder()
                .output(String.format("Answer after %d rounds of reflexion attempts:\n\n%s",
                        maxIterations, finalResp.getOutput()))
                .build();

        } catch (Exception e) {
            return OxyResponse.builder()
                .output("Reflexion workflow failed: " + e.getMessage())
                .build();
        }
    });
};
```

最后您需要使用`WorkflowAgent`管理反思过程：

```java
import com.jd.oxygent.core.oxygent.oxy.agents.WorkflowAgent;

WorkflowAgent.builder()
    .name("general_reflexion_agent")
    .desc("Workflow agent that optimizes answer quality through external reflexion")
    .subAgents(Arrays.asList("worker_agent", "reflexion_agent"))
    .funcWorkflow(REFLEXION_WORKFLOW)
    .llmModel("default_llm")
    .build()
```


**参考现有示例**: [DemoReflexionFlow.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/flows/DemoReflexionFlow.java)

[上一章：处理LLM和智能体输出](./08-02-handle_output.md)
[下一章：创建工作流](./09-03-workflow.md)
[回到首页](./readme.md)