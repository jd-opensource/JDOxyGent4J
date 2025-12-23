# 如何管理智能体使用工具的行为？

在默认情况下，agent对工具的调用由内部的LLM负责，但是OxyGent提供了一系列参数帮助您管理agent调用工具的方案，以下是您可能用到的参数：

## 基础工具管理

### 允许/禁止智能体使用工具

```java
ReActAgent.builder()
    .name("time_agent")
    .desc("A tool that can get current time")
    .tools(Arrays.asList("time_tools")) // 允许使用
    .exceptTools(Arrays.asList("math_tools")) //禁止使用
    .build();
```

### 管理智能体检索工具

```java
ReActAgent.builder()
    .name("time_agent")
    .desc("A tool that can get current time")
    .tools(Arrays.asList("time_tools"))
    .isSourcingTools(true) // 是否检索工具
    .isRetainSubagentInToolset(true) // 是否将subagent放入工具集
    .topKTools(10) //检索返回的工具数量
    .isRetrieveEvenIfToolsScarce(true) // 是否在工具数量不足时保持检索
    .build();
```

## 完整的可运行样例

以下是工具管理的完整代码示例（参考多个示例文件）：

```java
// 基础工具配置 (来自 DemoInReadme.java)
ReActAgent.builder()
    .name("time_agent")
    .desc("能够查询时间的工具代理")
    .tools(Arrays.asList("time_tools"))  // 允许使用时间工具
    .build()

ReActAgent.builder()
    .name("file_agent")
    .desc("能够操作文件系统的工具代理")
    .tools(Arrays.asList("file_tools"))  // 允许使用文件工具
    .build()

ReActAgent.builder()
    .name("math_agent")
    .desc("能够执行数学计算的工具代理")
    .tools(Arrays.asList("math_tools"))  // 允许使用数学工具
    .build()

// 信任模式配置 (来自 DemoTrustMode.java)
ReActAgent.builder()
    .name("untrusted_agent")
    .desc("A tool that requires manual approval for actions")
    .llmModel("default_llm")
    .trustMode(false)  // 需要人工审批工具调用
    .build()

ReActAgent.builder()
    .name("trusted_agent")
    .desc("A tool that can automatically execute actions")
    .llmModel("default_llm")
    .trustMode(true)   // 自动执行工具调用
    .build()

// 层次化工具管理 (来自 DemoHierarchicalAgents.java)
ReActAgent.builder()
    .name("time_agent")
    .desc("专门的时间查询代理")
    .additionalPrompt("专注于时间相关任务")
    .tools(Arrays.asList("time"))
    .trustMode(false)  // 严格控制工具使用
    .build()

ReActAgent.builder()
    .name("file_tools_agent")
    .desc("专门的文件操作代理")
    .additionalPrompt("专注于文件操作")
    .tools(Arrays.asList("file_tools"))
    .trustMode(false)  // 严格控制文件操作
    .build()
```

## 高级工具管理功能

### 工具权限控制
```java
// 多工具配置 (来自 DemoPlanAndSolve.java)
ReActAgent.builder()
    .name("planner_agent")
    .subAgents(Arrays.asList("time_agent", "file_agent", "math_agent"))
    .tools(Arrays.asList("joke_tools"))  // 主控agent额外工具
    .llmModel("default_llm")
    .timeout(100)
    .build()

// 工作流工具管理 (来自 DemoPlanAndSolveNew.java)
WorkflowAgent.builder()
    .name("math_agent")
    .subAgents(Arrays.asList("time_agent"))
    .tools(Arrays.asList("math_tools"))  // 工作流内工具访问
    .funcWorkflow(DemoPlanAndSolve::workflow)
    .llmModel("default_llm")
    .build()
```

### MCP工具授权管理
```java
// MCP工具授权示例 (来自 DemoMCPToolAuthorization.java)
ReActAgent.builder()
    .name("mcp_time_agent")
    .desc("Time agent with MCP tool authorization")
    .tools(Arrays.asList("time"))  // MCP时间工具
    .trustMode(false)  // 需要授权确认
    .build()
```

## 如何运行工具管理示例

你可以通过以下方式运行工具管理示例：



**参考现有示例**:
- [DemoInReadme.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/DemoInReadme.java) - 基础工具配置
- [DemoTrustMode.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/advanced/DemoTrustMode.java) - 信任模式工具管理
- [DemoHierarchicalAgents.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoHierarchicalAgents.java) - 层次化工具管理
- [DemoMCPToolAuthorization.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/tools/DemoMCPToolAuthorization.java) - MCP工具授权管理

## 工具管理最佳实践

### 1. 安全原则
```java
// 好的示例：敏感工具使用严格控制
ReActAgent.builder()
    .name("file_agent")
    .tools(Arrays.asList("file_tools"))
    .trustMode(false)  // 文件操作需要确认
    .build()

// 避免：敏感工具无限制使用
.trustMode(true) // 对文件操作过于宽松
```

### 2. 功能分离
```java
// 好的示例：按功能域分离工具
ReActAgent.builder()
    .name("time_specialist")
    .tools(Arrays.asList("time_tools"))  // 只访问时间相关工具
    .additionalPrompt("Focus only on time-related tasks.")
    .build()

// 避免：工具权限过于宽泛
.tools(Arrays.asList("time_tools", "file_tools", "math_tools")) // 过多工具权限
```

### 3. 层次化管理
```java
// 好的示例：通过主控agent协调工具使用
ReActAgent.builder()
    .isMaster(true)
    .name("coordinator")
    .subAgents(Arrays.asList("time_agent", "file_agent"))  // 通过子agent使用工具
    .build()
```

## 常见问题解答

**Q: trustMode的true和false有什么区别？**
A: trustMode(false) 需要人工确认工具调用，trustMode(true) 自动执行。敏感操作建议使用false。

**Q: 如何限制智能体只能使用特定工具？**
A: 使用 `tools(Arrays.asList("allowed_tool"))` 明确指定允许的工具，结合 `exceptTools()` 排除不需要的工具。

**Q: 子智能体的工具权限如何管理？**
A: 子智能体有独立的工具权限配置，主控智能体可以通过 `isRetainSubagentInToolset(true)` 将子智能体作为工具使用。

[上一章：注册一个工具](./02-01-register_single_tool.md)
[下一章：使用MCP开源工具](./02-03-use_opensource_tools.md)
[回到首页](./readme.md)