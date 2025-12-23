# 如何使用预设的流（Flow）？

对于开发者来说，将常用的工作流封装为预设的 [流 (Flow)] 是非常必要的。

您可以通过继承 `BaseFlow` 类来创建自己的流，并在 `_execute()` 方法中实现流的具体工作逻辑。流接受一个 `oxy.OxyRequest` 作为输入，并以 `oxy.Response` 作为输出，因此能够在 MAS 系统中像正常的 Agent 一样运行，不会发生兼容性问题。

下面以 OxyGent 预设的 `PlanAndSolve` 流为例，演示如何创建一个流。

## 数据类

### 1. Plan（计划）

- **作用**：定义未来需要执行的步骤。
- **核心字段**：`steps: List<String>`：排序后的任务步骤。

```java
public class Plan {
    /**
     * Plan to follow in future.
     */
    @JsonProperty("steps")
    @JsonPropertyDescription("different steps to follow, should be in sorted order")
    private List<String> steps;

    // constructors, getters, setters...
}
```

### 2. Response（直接响应）

- **作用**：当不需要再执行工具时，直接返回答案给用户。
- **核心字段**：`response: String`

```java
public class Response {
    /**
     * Response to user.
     */
    @JsonProperty("response")
    private String response;

    // constructors, getters, setters...
}
```

### 3. Action（动作）

- **作用**：封装下一步的动作。
- **核心字段**：`action: Union<Response, Plan>`：可以是一个新的计划，也可以是直接的响应。

```java
public class Action {
    /**
     * Action to perform.
     */
    @JsonProperty("action")
    @JsonPropertyDescription("Action to perform. If you want to respond to user, use Response. " +
                            "If you need to further use tools to get the answer, use Plan.")
    private Object action; // Can be Response or Plan

    // constructors, getters, setters...
}
```

## 主流程类

### PlanAndSolve（主流程类）：继承自 `BaseFlow`

#### 核心属性：

- **`plannerAgentName`**：负责生成计划的 agent。
- **`executorAgentName`**：执行每个步骤的 agent。
- **`enableReplanner`**：是否允许在执行中动态调整计划。
- **`pydanticParserPlanner`**：将 LLM 输出解析成 Plan。
- **`pydanticParserReplanner`**：将 LLM 输出解析成 Action。
- **`maxReplanRounds`**：最大迭代次数。

```java
public class PlanAndSolve extends BaseFlow {
    /**
     * Plan-and-Solve Prompting Workflow.
     */

    private int maxReplanRounds = 30; // Maximum retries for operations

    private String plannerAgentName = "planner_agent"; // planner agent name
    private List<String> prePlanSteps = null; // pre plan steps

    private boolean enableReplanner = false; // enable replanner

    private String executorAgentName = "executor_agent"; // executor agent name

    private String llmModel = "default_llm"; // LLM model name for fallback

    private Function<String, LLMResponse> funcParsePlannerResponse = null; // planner response parser

    private PydanticOutputParser pydanticParserPlanner = new PydanticOutputParser(Plan.class); // planner pydantic parser

    private Function<String, LLMResponse> funcParseReplannerResponse = null; // replanner response parser

    private PydanticOutputParser pydanticParserReplanner = new PydanticOutputParser(Action.class); // replanner pydantic parser

    public PlanAndSolve() {
        super();

        this.addPermittedTools(Arrays.asList(
            this.plannerAgentName,
            this.executorAgentName
        ));
    }

    @Override
    protected OxyResponse execute(OxyRequest oxyRequest) throws Exception {
        // Implementation will be shown below...
        return null;
    }
}
```

## 工作流逻辑

### 1. 规划阶段：

- 调用 `planner_agent` → 生成 `Plan.steps`

### 2. 执行阶段：

- 逐个执行 `steps`，每个步骤由 `executor_agent` 完成。

### 3. 重规划（可选）：

- 如果开启 `enable_replanner`，执行后可动态调整计划。

### 4. 结束阶段：

- 如果步骤执行完毕或 `replanner` 返回 `Response`，输出最终结果。

对应的代码逻辑如下：

```java
@Override
protected OxyResponse execute(OxyRequest oxyRequest) throws Exception {
    String planStr = "";
    String pastSteps = "";
    String originalQuery = oxyRequest.getQuery();
    List<String> planSteps = this.prePlanSteps;

    for (int currentRound = 0; currentRound <= this.maxReplanRounds; currentRound++) {
        if ((currentRound == 0) && (this.prePlanSteps == null)) {
            String query;
            if (this.pydanticParserPlanner != null) {
                query = this.pydanticParserPlanner.format(originalQuery);
            } else {
                query = originalQuery;
            }

            var oxyResponse = oxyRequest.call(
                this.plannerAgentName,
                Map.of("query", query)
            );

            Object planResponse;
            if (this.pydanticParserPlanner != null) {
                planResponse = this.pydanticParserPlanner.parse(oxyResponse.getOutput());
            } else {
                planResponse = this.funcParsePlannerResponse.apply(oxyResponse.getOutput());
            }

            planSteps = ((Plan) planResponse).getSteps();
            planStr = IntStream.range(0, planSteps.size())
                    .mapToObj(i -> String.format("%d. %s", i + 1, planSteps.get(i)))
                    .collect(Collectors.joining("\n"));
        }

        String task = planSteps.get(0);
        String taskFormatted = String.format(
            "We have finished the following steps: %s\n" +
            "The current step to execute is:%s\n" +
            "You should only execute the current step, and do not execute other steps in our plan. " +
            "Do not execute more than one step continuously or skip any step.",
            pastSteps, task
        ).trim();

        var executorResponse = oxyRequest.call(
            this.executorAgentName,
            Map.of("query", taskFormatted)
        );

        pastSteps = pastSteps + "\n" + String.format("task:%s, execute task result:%s",
                task, executorResponse.getOutput());

        if (this.enableReplanner) {
            // Replanning logic
            String replanQuery = String.format(
                "The target of user is:\n%s\n\n" +
                "The origin plan is:\n%s\n\n" +
                "We have finished the following steps:\n%s\n\n" +
                "Please update the plan considering the mentioned information. " +
                "If no more operation is supposed, Use **Response** to answer the user. " +
                "Otherwise, please update the plan. The plan should only contain the steps to be executed, " +
                "and do not include the past steps or any other information.",
                originalQuery, planStr, pastSteps
            );

            if (this.pydanticParserReplanner != null) {
                replanQuery = this.pydanticParserReplanner.format(replanQuery);
            }

            var replannerResponse = oxyRequest.call(
                this.replannerAgentName,
                Map.of("query", replanQuery)
            );

            Object planResponse;
            if (this.pydanticParserReplanner != null) {
                planResponse = this.pydanticParserReplanner.parse(replannerResponse.getOutput());
            } else {
                planResponse = this.funcParsePlannerResponse.apply(replannerResponse.getOutput());
            }

            Action action = (Action) planResponse;
            if (action.getAction() instanceof Response) {
                return new OxyResponse(
                    OxyState.COMPLETED,
                    ((Response) action.getAction()).getResponse()
                );
            } else {
                Plan newPlan = (Plan) action.getAction();
                planSteps = newPlan.getSteps();
                planStr = IntStream.range(0, planSteps.size())
                        .mapToObj(i -> String.format("%d. %s", i + 1, planSteps.get(i)))
                        .collect(Collectors.joining("\n"));
            }
        } else {
            planSteps = planSteps.subList(1, planSteps.size());

            if (planSteps.isEmpty()) {
                return new OxyResponse(
                    OxyState.COMPLETED,
                    executorResponse.getOutput()
                );
            }
        }
    }

    planStr = IntStream.range(0, planSteps.size())
            .mapToObj(i -> String.format("%d. %s", i + 1, planSteps.get(i)))
            .collect(Collectors.joining("\n"));

    String userInputWithResults = String.format(
        "Your objective was this：%s\n---\nFor the following plan：%s",
        oxyRequest.getQuery(), planStr
    );

    List<Map<String, Object>> tempMessages = Arrays.asList(
        Map.of("role", "system", "content", "Please answer user questions based on the given plan."),
        Map.of("role", "user", "content", userInputWithResults)
    );

    var oxyResponse = oxyRequest.call(
        this.llmModel,
        Map.of("messages", tempMessages)
    );

    return new OxyResponse(
        OxyState.COMPLETED,
        oxyResponse.getOutput()
    );
}
```

## 执行流

OxyGent 支持像 Agent 一样调用 Flow。您可以通过以下方式调用您的自定义流：

```java
PlanAndSolve.builder()
    // 对于自定义 flow，按照您的方法调用
    .name("master_agent")
    .isDiscardReactMemory(true)
    .llmModel("default_llm")
    .isMaster(true)
    .plannerAgentName("planner_agent")
    .executorAgentName("executor_agent")
    .enableReplanner(false)
    .timeout(100)
    .build()
```

## 高级PlanAndSolve实现

OxyGent还提供了一个功能更完整的PlanAndSolve实现，包括意图理解和多Agent协作（参考 [`DemoPlanAndSolve.java`](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/flows/DemoPlanAndSolve.java)）：

```java
static String INTENTION_PROMPT = """
    You are an expert in intention understanding, skilled at understanding the intentions of conversations.
    The following is a daily chat scenario. Please describe the merchant's current question intention
    with clear and concise language based on the historical conversation. Specific requirements are as follows:
    1. Based on the historical conversation, think step by step about the current question,
       analyze the core semantics of the question, infer the core intention of the question,
       and then describe the thinking process with concise text;
    2. Based on the thinking process and conversation information, describe the intention using
       declarative sentences. Only output the intention, and prohibit outputting irrelevant
       expressions like "the current intention is";
    3. Intention understanding should be faithful to the semantics of the current question
       and historical conversation.
    """;

@OxySpaceBean(value = "planAndSolveNewJavaOxySpace", defaultStart = true,
              query = "What time is it now? Please save it to the file log.txt under the local_file folder.")
public static List<BaseOxy> getDefaultOxySpace() {
    return Arrays.asList(
        HttpLlm.builder()
            .name("default_llm")
            .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
            .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
            .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
            .llmParams(Map.of("temperature", 0.01))
            .build(),

        // 意图理解Agent
        ChatAgent.builder()
            .name("intent_agent")
            .prompt(INTENTION_PROMPT)
            .llmModel("default_llm")
            .build(),

        // 多种MCP工具
        new StdioMCPClient("time_tools", "uvx",
            Arrays.asList("mcp-server-time", "--local-timezone=Asia/Shanghai")),
        new StdioMCPClient("file_tools", "npx",
            Arrays.asList("-y", "@modelcontextprotocol/server-filesystem", "./local_file")),
        new StdioMCPClient("math_tools", "uv",
            Arrays.asList("--directory", "path/to/mcp_servers", "run", "math_tools.py")),

        // 高级计划Agent
        ChatAgent.builder()
            .name("planner_agent")
            .desc("An agent capable of making plans")
            .llmModel("default_llm")
            .prompt("""
                For a given goal, create a simple and step-by-step executable plan.
                The plan should be concise, with each step being an independent and complete functional module.
                The plan should consist of independent tasks that, if executed correctly, will lead to the correct answer.
                Ensure that each step is actionable and includes all necessary information for execution.
                The result of the final step should be the final answer.
                """)
            .build(),

        // 高级执行Agent
        ReActAgent.builder()
            .name("executor_agent")
            .desc("An agent capable of executing tools")
            .subAgents(Arrays.asList("time_agent", "time_agent_b", "time_agent_c", "file_agent", "math_agent"))
            .tools(Arrays.asList("joke_tools"))
            .llmModel("default_llm")
            .timeout(100)
            .prompt("""
                You are a helpful assistant who can use the following tools:
                ${tools_description}

                You only need to complete the **current step** in the plan—do not do anything extra.
                Respond strictly according to the requirements of the current step.
                If a tool is required, select one from the tools listed above.
                If multiple tool calls are needed, call only **one** tool at a time.
                """)
            .build(),

        // 主PlanAndSolve流程
        PlanAndSolve.builder()
            .name("master_agent")
            .isDetailedObservation(true)
            .llmModel("default_llm")
            .isMaster(true)
            .plannerAgentName("planner_agent")
            .executorAgentName("executor_agent")
            .enableReplanner(false)  // 禁用重新规划
            .timeout(100)
            .build(),

        // 多个时间Agent实例 (演示负载均衡)
        ReActAgent.builder()
            .name("time_agent")
            .desc("A tool for querying the time")
            .tools(Arrays.asList("time_tools"))
            .llmModel("default_llm")
            .timeout(100)
            .build(),

        ReActAgent.builder()
            .name("time_agent_b")
            .desc("A tool for querying the time")
            .tools(Arrays.asList("time_tools"))
            .llmModel("default_llm")
            .timeout(100)
            .build(),

        // 文件操作Agent
        ReActAgent.builder()
            .name("file_agent")
            .desc("A tool for operating the file system")
            .tools(Arrays.asList("file_tools"))
            .llmModel("default_llm")
            .build(),

        // 数学计算工作流Agent
        WorkflowAgent.builder()
            .name("math_agent")
            .desc("A tool for querying the value of pi")
            .subAgents(Arrays.asList("time_agent"))
            .tools(Arrays.asList("math_tools"))
            .llmModel("default_llm")
            .isRetainMasterShortMemory(true)
            .funcWorkflow(x -> {
                // 复杂的Pi计算工作流逻辑
                String masterQuery = x.getQuery(true);

                // 调用时间Agent获取当前时间
                OxyResponse timeResponse = x.call(new HashMap<>(Map.of(
                    "callee", "time_agent",
                    "arguments", new HashMap<>(Map.of("query", "What time is it now?"))
                )));

                // 使用正则表达式解析数字
                Pattern pattern = Pattern.compile("\\d+");
                Matcher matcher = pattern.matcher(timeResponse.getOutput().toString());
                String n = null;
                while (matcher.find()) {
                    n = matcher.group();
                }

                if (n != null) {
                    OxyResponse piResponse = x.call(new HashMap<>(Map.of(
                        "callee", "calc_pi",
                        "arguments", new HashMap<>(Map.of("prec", n))
                    )));
                    return String.format("Save %s positions: %s", n, piResponse.getOutput());
                } else {
                    return "Save 2 positions: 3.14, or you could ask me to save how many positions you want.";
                }
            })
            .build()
    );
}
```

### 高级特性对比

| 特性          | 基础PlanAndSolve | 高级PlanAndSolve    |
| ----------- | -------------- | ----------------- |
| **意图理解**    | 不支持            | ✅ 专门的意图理解Agent    |
| **工具种类**    | 基础工具           | ✅ 多种MCP工具集成       |
| **Agent数量** | 2-3个           | ✅ 8+个专业Agent      |
| **负载均衡**    | 不支持            | ✅ 多时间Agent实例      |
| **复杂工作流**   | 简单步骤           | ✅ 嵌套WorkflowAgent |
| **文件操作**    | 不支持            | ✅ 完整文件系统操作        |

## 如何运行示例

你可以通过以下方式运行预设流程示例：

**参考现有示例**:

- [DemoPlanAndSolve.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/flows/DemoPlanAndSolve.java) - 基础Plan-and-Solve流程实现
- [DemoReflexionFlow.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/flows/DemoReflexionFlow.java) - 反思流程实现

[上一章：创建工作流](./09-03-workflow.md)
[下一章：获取记忆和重新生成](./09-01-continue_exec.md)
[回到首页](./readme.md)