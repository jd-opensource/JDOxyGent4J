# OxyGent4J 文档

**OxyGent4J** 是一个开源的 Java 多智能体系统框架。它将 LLM、智能体（Agent）、工具（Tool）和流程（Flow）统一为模块化组件，通过名称引用组装在一起，帮助你快速构建、运行和迭代基于智能体的应用。

---

## 为什么选择 OxyGent4J？

- **内置智能体循环** — 自动处理工具调用、响应解析和多轮推理
- **Java 原生** — 以 Java 对象 + Builder 模式定义智能体、工具和流程，无需 DSL 或配置文件
- **10+ 智能体类型** — ChatAgent、ReActAgent、WorkflowAgent、ParallelAgent、PlanAndSolveAgent、RAGAgent、ShellUseAgent、SkillAgent 等
- **MCP 协议支持** — 连接任意 MCP 工具服务器（Stdio、SSE、Streamable），同时支持原生 Java 工具
- **多智能体编排** — 层级调度、并行执行、混合智能体、分布式系统
- **A2A 互操作** — 内置 Agent-to-Agent 协议（官方 Java SDK），支持跨框架通信
- **Web UI + API** — 内嵌 Tomcat / Spring Boot + SSE 流式传输 + 内置聊天界面 + 可视化调试
- **生产级工具** — 链路追踪、对话历史、动态提示词管理、评分反馈

---

## Hello World

```java
import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;

import java.util.Arrays;
import java.util.Map;

public class HelloWorld {
    public static void main(String[] args) throws Exception {
        var oxySpace = Arrays.asList(
            HttpLlm.builder()
                .name("default_llm")
                .apiKey(System.getenv("OXY_LLM_API_KEY"))
                .baseUrl(System.getenv("OXY_LLM_BASE_URL"))
                .modelName(System.getenv("OXY_LLM_MODEL_NAME"))
                .build(),
            ChatAgent.builder()
                .name("assistant")
                .isMaster(true)
                .llmModel("default_llm")
                .build()
        );

        var mas = new Mas("app", oxySpace);
        mas.setDefaultOxySpace(oxySpace);
        mas.init();

        var response = mas.chatWithAgent(Map.of("query", "你好！"));
        System.out.println(response.getOutputAsString());
    }
}
```

---

## 选择你的路径

| 你的目标 | 从这里开始 |
|---------|-----------|
| 5 分钟构建第一个智能体 | [快速上手](./introduction/getting-started/quickstart.md) |
| 了解核心概念与术语 | [概念总览](./introduction/getting-started/overview.md) |
| 了解架构和类层次结构 | [架构总览](./introduction/getting-started/architecture.md) |
| 给智能体添加工具 | [注册工具](./introduction/tools/register-tool.md) |
| 使用 MCP 协议工具 | [MCP 开源工具](./introduction/tools/opensource-mcp-tools.md) |
| 构建多智能体系统 | [多智能体系统](./introduction/multi-agent/multi-agent-system.md) |
| 并行运行智能体 | [并行调用](./introduction/multi-agent/parallel.md) |
| 部署为 Web 服务 | [Web API](./introduction/backend/web-api.md) |
| 跨进程连接智能体 | [分布式系统](./introduction/multi-agent/distributed.md) |
| 与其他框架互操作 | [A2A 协议](./introduction/a2a/demo-guide.md) |

---

## 文档分区

| 分区 | 说明 |
|------|------|
| [教程](./introduction/readme.md) | 从安装到高级功能的逐步指南 |
| [API 参考](./api/readme.md) | 详细的类与方法文档 |
| [示例](./examples/readme.md) | 可运行的示例代码，按功能分类 |
