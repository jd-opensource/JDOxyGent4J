# OxyGent4J 概念总览

OxyGent4J 是一个开源的 Java 多智能体系统 (MAS) 框架。它将 LLM、智能体 (Agent)、工具 (Tool)、流程 (Flow) 统一为模块化的 "Oxy" 组件，通过名称引用的方式组装在一起，帮助您快速构建、运行和迭代多智能体系统。

---

## 核心概念

### OxySpace：组件列表

`oxySpace` 是一个 `List<BaseOxy>`，包含了系统中所有组件——LLM、Agent、Tool。每个组件都有一个 `name`，组件之间通过名称互相引用，而不是通过 Java 对象引用。

```java
List<BaseOxy> oxySpace = Arrays.asList(
    HttpLlm.builder()
            .name("default_llm")
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(modelName)
            .build(),
    StdioMCPClient.builder()
            .name("time_tools")
            .command("npx")
            .args(Arrays.asList("-y", "@anthropic/time-mcp-server"))
            .build(),
    ReActAgent.builder()
            .name("master_agent")
            .tools(Arrays.asList("time_tools"))       // 通过名称引用工具
            .llmModel("default_llm")                   // 通过名称引用 LLM
            .isMaster(true)
            .build()
);
```

### MAS：运行时容器

`Mas` (Multi-Agent System) 是 OxyGent4J 的运行时容器。它接收 `oxySpace`，将所有组件注册到内部，建立组件之间的引用关系，并提供多种运行方式：

```java
Mas mas = new Mas("app", oxySpace);
mas.init();

// Web 服务模式（通过 Spring Boot）
ServerApp.main(args);

// 命令行交互模式
mas.startCliMode("你好！");

// 编程式调用
OxyResponse response = mas.chatWithAgent(Map.of("query", "你好"));
System.out.println(response.getOutput());

// 批处理模式
List<Object> results = mas.startBatchProcessing(queries, false);
```

### isMaster：入口智能体

设置 `.isMaster(true)` 的智能体是用户查询的入口。用户的消息首先到达 master 智能体，由它决定是自己回答还是调用其他子智能体或工具。一个 MAS 中只能有一个 master 智能体。

### 组件类型

```
BaseOxy (基类)
├── BaseLlm (大语言模型)
│   ├── HttpLlm          — 通过 HTTP API 调用云端模型
│   ├── OpenAiLlm        — 使用 OpenAI SDK 调用兼容模型
│   ├── RemoteLlm        — 远程 LLM 基类
│   └── MockLlm          — 测试用模拟模型
├── BaseFlow (流程基类)
│   └── BaseAgent (智能体基类)
│       ├── LocalAgent         — 本地智能体基类
│       │   ├── ChatAgent      — 基础对话（单轮 LLM 调用）
│       │   ├── ReActAgent     — 推理-行动循环（支持工具调用）
│       │   ├── WorkflowAgent  — 自定义工作流
│       │   ├── ParallelAgent  — 并行执行多个子任务
│       │   ├── PlanAndSolveAgent — 先规划后执行
│       │   ├── RAGAgent       — 检索增强生成
│       │   ├── ShellUseAgent  — SSH 远程命令执行
│       │   └── SkillAgent     — 动态加载技能
│       └── RemoteAgent        — 远程智能体基类
│           ├── SSEAgent       — SSE 跨进程连接
│           └── A2AClientAgent — A2A 协议客户端
└── BaseTool (工具基类)
    ├── FunctionHub       — Java 函数工具集
    ├── BaseMCPClient     — MCP 协议客户端基类
    │   ├── StdioMCPClient    — MCP 标准输入/输出工具
    │   ├── SSEMCPClient      — MCP SSE 工具
    │   └── StreamableMCPClient — MCP Streamable 工具
    └── PresetTools       — 内置预设工具（时间、文件、数学等）
```

---

## 术语表

| 术语 | 说明 |
|------|------|
| oxySpace | 包含所有组件 (LLM、Agent、Tool) 的 `List<BaseOxy>` |
| Mas | Multi-Agent System，运行时容器，管理所有组件的生命周期 |
| isMaster | 标记入口智能体，接收用户查询的第一个智能体 |
| OxyRequest | 请求对象，在组件之间传递，包含查询内容、上下文、共享数据等 |
| OxyResponse | 响应对象，包含输出结果和状态 |
| traceId | 追踪标识，标记一次完整的对话或任务链 |
| subAgents | 子智能体列表，master 智能体可以调度的下级智能体 |
| tools | 智能体可以使用的工具名称列表 |
| llmModel | 智能体内部使用的 LLM 名称 |
| FunctionHub | 将 Java 函数注册为工具的容器 |
| StdioMCPClient | 通过标准输入/输出协议连接外部工具服务器 |
| PresetTools | OxyGent4J 内置的工具集（文件、数学、时间、Shell 等） |
| @SuperBuilder | Lombok 注解，提供 Builder 模式构建组件 |

---

## 最小运行示例

```java
import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MinimalExample {
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
                        .build()
        );

        Mas mas = new Mas("app", oxySpace);
        mas.init();
        mas.startCliMode("你好！");
    }
}
```

---

## 与 Python 版本的对应关系

| Python OxyGent | Java OxyGent4J | 说明 |
|----------------|----------------|------|
| `oxy_space = [...]` | `List<BaseOxy> oxySpace = Arrays.asList(...)` | 组件列表 |
| `oxy.HttpLLM(...)` | `HttpLlm.builder()...build()` | Builder 模式 |
| `async with MAS(...) as mas:` | `new Mas(...); mas.init();` | 同步初始化 |
| `await mas.chat_with_agent(...)` | `mas.chatWithAgent(...)` | 同步调用 |
| `@hub.tool()` 装饰器 | `hub.registerTool(...)` | 注册工具函数 |

---

## 接下来

- [安装 OxyGent4J](./install.md) -- 安装框架
- [快速上手](./quickstart.md) -- 5 分钟构建第一个智能体
- [架构设计](./architecture.md) -- 深入了解类层次和执行流程

---

[上一章：安装 OxyGent4J](./install.md)
[下一章：快速上手](./quickstart.md)
[回到首页](../readme.md)
