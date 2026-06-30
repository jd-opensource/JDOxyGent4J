# 快速上手

> 五分钟内构建你的第一个 Java 智能体，添加工具，编排多智能体系统。

---

## 前置条件

- **JDK 17+**
- 已添加 OxyGent4J Maven 依赖（参见 [安装指南](./install.md)）
- 一个 LLM API Key（支持任何 OpenAI 兼容的 API：DeepSeek、通义千问、智谱等）

## 1. 设置环境变量

在终端中导出 LLM 配置：

```bash
export OXY_LLM_API_KEY="your_api_key"
export OXY_LLM_BASE_URL="your_base_url"
export OXY_LLM_MODEL_NAME="your_model_name"
```

支持任何 OpenAI 兼容的 API（DeepSeek、通义千问、智谱等），只需填入对应的 base_url 和 model_name。

---

## 2. 创建第一个智能体

创建 `QuickStart.java`：

```java
import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class QuickStart {
    public static void main(String[] args) throws Exception {
        List<BaseOxy> oxySpace = Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .build(),
                ChatAgent.builder()
                        .name("assistant")
                        .isMaster(true)
                        .llmModel("default_llm")
                        .prompt("You are a helpful assistant.")
                        .build()
        );

        Mas mas = new Mas("app", oxySpace);
        mas.init();

        var response = mas.chatWithAgent(Map.of("query", "你好！你能做什么？"));
        System.out.println("Agent: " + response.getOutput());
    }
}
```

预期输出：

```
Agent: 你好！我是一个智能助手，可以回答问题、辅助写作、……
```

**要点：**

- `oxySpace` 是组件列表（`List<BaseOxy>`），包含 LLM 和 Agent。
- 组件之间通过 `name` 互相引用，如 `.llmModel("default_llm")`。
- `.isMaster(true)` 标记入口智能体，用户消息首先到达它。
- `Mas` 是运行时容器，调用 `init()` 完成初始化。
- 所有组件使用 Lombok `@SuperBuilder` 模式构建。

---

## 3. 启动 Web 服务

使用内置的 Spring Boot 服务，通过 `ServerApp` 启动 Web UI：

```java
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;

public class QuickStartWeb {
    public static void main(String[] args) throws Exception {
        ServerApp.main(args);
    }
}
```

启动后浏览器会自动打开 `http://127.0.0.1:8080`，即可在 Web UI 中与智能体对话。

MAS 支持多种运行模式：

| 模式 | 方法 | 用途 |
|------|------|------|
| Web 服务 | `ServerApp.main()` | 可视化界面 + REST API |
| 命令行 | `mas.startCliMode(firstQuery)` | 终端交互式对话 |
| 批处理 | `mas.startBatchProcessing(queries)` | 批量并发执行 |
| 编程式 | `mas.chatWithAgent(payload)` | 嵌入到应用代码中 |

---

## 4. 添加工具

通过 `FunctionHub` 注册工具，并切换到 `ReActAgent`，智能体就能推理并调用工具。

```java
import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class QuickStartTool {
    public static void main(String[] args) throws Exception {
        // 创建 FunctionHub 并注册工具
        FunctionHub calculatorHub = new FunctionHub("calculator");
        calculatorHub.registerTool(
                "add", "Add two numbers together",
                (params) -> {
                    double a = Double.parseDouble(params.get("a").toString());
                    double b = Double.parseDouble(params.get("b").toString());
                    return String.valueOf(a + b);
                },
                Arrays.asList(
                        new FunctionHub.ParamMeta("a", "number", "First number", ""),
                        new FunctionHub.ParamMeta("b", "number", "Second number", "")
                )
        );
        calculatorHub.registerTool(
                "multiply", "Multiply two numbers together",
                (params) -> {
                    double a = Double.parseDouble(params.get("a").toString());
                    double b = Double.parseDouble(params.get("b").toString());
                    return String.valueOf(a * b);
                },
                Arrays.asList(
                        new FunctionHub.ParamMeta("a", "number", "First number", ""),
                        new FunctionHub.ParamMeta("b", "number", "Second number", "")
                )
        );

        List<BaseOxy> oxySpace = Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .build(),
                calculatorHub,
                ReActAgent.builder()
                        .name("math_agent")
                        .isMaster(true)
                        .llmModel("default_llm")
                        .tools(Arrays.asList("calculator"))
                        .prompt("You are a math assistant. Use the calculator tools to answer math questions.")
                        .build()
        );

        Mas mas = new Mas("app", oxySpace);
        mas.init();

        var response = mas.chatWithAgent(Map.of("query", "12.5 加 7.3 等于多少？再把结果乘以 4。"));
        System.out.println("Agent: " + response.getOutput());
    }
}
```

`ReActAgent` 遵循推理-行动循环（Reasoning + Acting）：先思考该做什么，调用工具，观察结果，循环直到得到答案。

**要点：**
- `FunctionHub` 是工具容器，通过 `registerTool()` 注册 Java 函数。
- `ReActAgent` 会自动推理何时调用工具、调用哪个工具，并将结果整合为最终回答。
- Agent 通过 `.tools(Arrays.asList("calculator"))` 引用工具集的 `name`。

---

## 5. 多智能体协作

添加子智能体，由 master 智能体统一调度。master 根据用户意图自动分发任务。

```java
List<BaseOxy> oxySpace = Arrays.asList(
        HttpLlm.builder()
                .name("default_llm")
                .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                .build(),
        calculatorHub,
        // 子智能体：处理数学问题
        ReActAgent.builder()
                .name("math_agent")
                .desc("A math specialist. Delegates math questions to this agent.")
                .llmModel("default_llm")
                .tools(Arrays.asList("calculator"))
                .prompt("You are a math assistant.")
                .build(),
        // 主智能体：路由查询
        ReActAgent.builder()
                .name("master_agent")
                .isMaster(true)
                .llmModel("default_llm")
                .subAgents(Arrays.asList("math_agent"))
                .prompt("You are a helpful assistant. Route math questions to math_agent.")
                .build()
);

Mas mas = new Mas("app", oxySpace);
mas.init();

var response = mas.chatWithAgent(Map.of("query", "99 加 1 等于多少？"));
System.out.println("Agent: " + response.getOutput());
```

**要点：**
- 只有一个智能体设置 `.isMaster(true)`，作为用户请求的入口。
- 通过 `.subAgents(Arrays.asList(...))` 声明子智能体，按名称引用。
- `.desc(...)` 字段告诉 master 每个子智能体的能力，master 据此判断应该调度谁。

---

## 6. 使用 MCP 工具

OxyGent4J 支持通过 MCP 协议连接外部工具服务器：

```java
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;

StdioMCPClient.builder()
        .name("time_tools")
        .command("npx")
        .args(Arrays.asList("-y", "@anthropic/time-mcp-server"))
        .build()
```

将 MCP 工具添加到 `oxySpace` 列表中，并在 Agent 的 `.tools(...)` 中引用即可。

---

## 下一步

恭喜你完成了快速上手教程！接下来可以深入了解：

- [概念总览](./overview.md) -- 理解 OxyGent4J 的核心概念
- [架构设计](./architecture.md) -- 深入了解类层次和执行流程
- [设置 Config](./config.md) -- 全局配置、LLM 默认值、日志

[上一章：安装 OxyGent4J](./install.md)
[下一章：概念总览](./overview.md)
[回到首页](../readme.md)
