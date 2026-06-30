# 如何注册一个工具?

在 OxyGent4J 中，有三种主要方式注册工具：

1. **FunctionHub**：通过 Java 代码动态注册本地函数工具
2. **StdioMCPClient**：连接基于标准输入/输出的 MCP Server
3. **SSEMCPClient**：连接基于 SSE 协议的远程 MCP Server
4. **@MCPTool 注解**：通过注解声明 MCP 工具（用于发布 MCP Server）

## 方式一：使用 FunctionHub 注册本地工具

### 步骤 1：创建 FunctionHub 实例

```java
import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;

FunctionHub jokeTools = new FunctionHub("joke_tools");
jokeTools.setDesc("Tool collection for telling jokes");
```

### 步骤 2：注册工具函数

使用 `registerTool` 方法将函数注册为工具：

```java
jokeTools.registerTool(
    "joke_tool",                            // 工具名称
    "A tool that can generate jokes",       // 工具描述
    (args) -> {                             // 工具执行函数
        String jokeType = (String) args.getOrDefault("joke_type", "any");
        return getRandomJoke(jokeType);
    },
    Arrays.asList(                          // 参数定义
        new FunctionHub.ParamMeta("joke_type", "String", "Type of the joke", "any")
    )
);
```

### 步骤 3：将工具添加到 Agent

将注册的工具放入 oxySpace，Agent 将根据工具的描述自动调用相应工具：

```java
List<BaseOxy> oxySpace = Arrays.asList(
    HttpLlm.builder()
            .name("default_llm")
            .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
            .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
            .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
            .build(),
    jokeTools,  // FunctionHub 直接放入 oxySpace
    ReActAgent.builder()
            .name("master_agent")
            .isMaster(true)
            .tools(Arrays.asList("joke_tool"))  // 引用工具名称
            .llmModel("default_llm")
            .build()
);
```

## 方式二：使用 StdioMCPClient 连接 MCP Server

`StdioMCPClient` 通过标准输入/输出与 MCP Server 子进程通信：

```java
// 连接 uvx 安装的 MCP Server
new StdioMCPClient("time", "uvx", Arrays.asList(
    "mcp-server-time",
    "--local-timezone=Asia/Shanghai"
))

// 连接 npx 安装的 MCP Server
new StdioMCPClient("file_tools", "npx", Arrays.asList(
    "-y", "@modelcontextprotocol/server-filesystem", "./local_file"
))

// Windows 系统语法
new StdioMCPClient("time", "cmd.exe", Arrays.asList(
    "/c", "uvx", "mcp-server-time", "--local-timezone=Asia/Shanghai"
))
```

### 完整示例：使用 MCP 工具的 Agent

```java
List<BaseOxy> oxySpace = Arrays.asList(
    HttpLlm.builder()
            .name("default_llm")
            .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
            .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
            .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
            .build(),
    new StdioMCPClient("time", "uvx", Arrays.asList(
            "mcp-server-time", "--local-timezone=Asia/Shanghai"
    )),
    ReActAgent.builder()
            .name("time_agent")
            .desc("Tool agent capable of querying time")
            .tools(Arrays.asList("time"))
            .trustMode(false)
            .build(),
    ReActAgent.builder()
            .isMaster(true)
            .name("master_agent")
            .subAgents(Arrays.asList("time_agent"))
            .build()
);
```

## 方式三：使用 SSEMCPClient 连接远程 MCP Server

`SSEMCPClient` 通过 SSE 协议连接远程 MCP Server：

```java
SSEMCPClient.builder()
    .name("remote_tools")
    .serverUrl("http://127.0.0.1:9000/sse")
    .build()
```

## 方式四：使用 @MCPTool 注解发布工具

如果您需要将 Java 方法发布为 MCP Server 的工具，可以使用 `@MCPTool` 注解：

```java
import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;

public class MathTools {

    @MCPTool(name = "calc_pi", description = "Calculate the value of pi to n decimal places")
    public String calcPi(
        @ToolParam(name = "prec", description = "Number of decimal places") int prec
    ) {
        // 计算逻辑
        return computePi(prec);
    }

    @MCPTool(name = "add", description = "Add two numbers together")
    public String add(
        @ToolParam(name = "a", description = "First number") double a,
        @ToolParam(name = "b", description = "Second number") double b
    ) {
        return String.valueOf(a + b);
    }
}
```

使用 `@EnableMcpServer` 注解启用 MCP Server 自动扫描和注册。

## FunctionTool 详解

`FunctionTool` 是 `FunctionHub` 内部用于包装单个函数的类。通常您不需要直接使用它，而是通过 `FunctionHub.registerTool()` 注册工具。

每个 `FunctionTool` 包含：
- `name`：工具名称
- `desc`：工具描述（LLM 根据描述决定何时调用）
- `funcProcess`：工具执行函数
- `params`：参数元数据列表

## 预设工具

OxyGent4J 提供了一些预设工具，可以直接在 oxySpace 中使用：

```java
import com.jd.oxygent.core.oxygent.tools.PresetTools;

// 预设工具包括：FileTool, MathTool, TimeTool, ShellTools, HttpTools 等
```

[上一章：选择智能体种类](../agents/agent-types.md)
[下一章：多智能体系统](../multi-agent/multi-agent-system.md)
[回到首页](../readme.md)
