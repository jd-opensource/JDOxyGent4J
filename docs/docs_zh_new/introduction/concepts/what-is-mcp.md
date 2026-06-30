# 什么是 MCP？

MCP（Model Context Protocol，模型上下文协议）是由 Anthropic 提出的开放协议，旨在为大语言模型提供一种标准化的方式来连接外部工具和数据源。

---

## 核心思想

在 MCP 出现之前，每个 AI 框架都有自己的工具接入方式，工具开发者需要为不同框架重复适配。MCP 定义了统一的客户端-服务端通信协议，让工具只需实现一次，就能被所有支持 MCP 的框架调用。

```
┌─────────────┐    MCP 协议   ┌─────────────┐
│   AI 框架    │ ◄──────────► │  MCP 服务器  │
│ （MCP 客户端）│              │ （工具实现）  │
└─────────────┘              └─────────────┘
```

### 工作流程

1. **连接**：MCP 客户端启动并连接到 MCP 服务器。
2. **发现**：客户端请求服务器的工具列表，获取每个工具的名称、描述和参数模式。
3. **调用**：当 LLM 决定使用某个工具时，客户端将调用请求发送给服务器。
4. **返回**：服务器执行工具并返回结果给客户端。

### 传输方式

MCP 支持多种传输协议：

| 传输方式 | 说明 | 适用场景 |
|----------|------|----------|
| Stdio | 标准输入/输出 | 本地工具，客户端启动服务器进程 |
| SSE | Server-Sent Events | 远程工具，通过 HTTP 长连接 |
| Streamable HTTP | 可流式 HTTP | 远程工具，更现代的 HTTP 传输 |

---

## 为什么使用 MCP？

1. **跨语言**：工具服务器可以用 Python、TypeScript、Go、Rust 或任何语言编写。协议与语言无关。
2. **生态系统**：社区发布了大量可复用的 MCP 工具服务器（文件系统、数据库、API、浏览器等）。无需编写任何工具代码即可使用。
3. **进程隔离**：工具在独立进程中运行。有问题或缓慢的工具不会导致智能体崩溃。
4. **可组合性**：一个智能体可以同时连接多个 MCP 服务器，每个提供不同的工具。

---

## OxyGent4J 的 MCP 支持

OxyGent4J 内置了三种 MCP 客户端，对应三种传输方式，基于 Spring AI MCP SDK 实现：

| 客户端类 | 传输方式 | 适用场景 |
|----------|----------|----------|
| `StdioMCPClient` | Stdio | 本地工具服务器，作为子进程启动 |
| `SSEMCPClient` | SSE | 远程工具服务器，通过 HTTP（旧版） |
| `StreamableMCPClient` | Streamable HTTP | 远程工具服务器，支持流式传输 |

### StdioMCPClient

通过启动子进程连接本地 MCP 服务器：

```java
StdioMCPClient.builder()
    .name("file_tools")
    .command("npx")
    .args(Arrays.asList("-y", "@modelcontextprotocol/server-filesystem", "./data"))
    .build();
```

底层使用 `io.modelcontextprotocol.client.McpClient` 和 `StdioClientTransport` 建立连接，通过标准输入/输出进行 JSON-RPC 通信。

### SSEMCPClient

通过 SSE 协议连接远程 MCP 服务器：

```java
SSEMCPClient.builder()
    .name("remote_tools")
    .url("http://mcp-server:8080/sse")
    .build();
```

底层使用 `HttpClientSseClientTransport` 建立 SSE 长连接，适用于需要实时数据推送的场景。

### StreamableMCPClient

通过 Streamable HTTP 协议连接远程 MCP 服务器：

```java
StreamableMCPClient.builder()
    .name("remote_tools")
    .url("http://mcp-server:8080/mcp")
    .headers(Map.of("Authorization", "Bearer token"))
    .build();
```

底层使用 `HttpClientStreamableHttpTransport`，支持双向流式数据传输和自定义 HTTP 头。

### 使用方式

MCP 客户端注册到 `oxySpace` 后，MAS 初始化时会自动连接服务器、发现工具并注册。智能体通过 `tools` 参数引用即可使用：

```java
List<BaseOxy> oxySpace = Arrays.asList(
    HttpLlm.builder().name("llm").apiKey(key).baseUrl(url).modelName(model).build(),
    StdioMCPClient.builder().name("file_tools").command("npx")
        .args(Arrays.asList("-y", "@modelcontextprotocol/server-filesystem", "./data"))
        .build(),
    ReActAgent.builder()
        .name("agent")
        .isMaster(true)
        .llmModel("llm")
        .tools(Arrays.asList("file_tools"))
        .build()
);
```

社区已有大量开源 MCP 服务器可直接使用，涵盖文件系统、数据库、浏览器、搜索引擎等常见场景。

---

## 延伸阅读

- [使用 MCP 开源工具](../tools/opensource-mcp-tools.md) — 接入社区工具服务器
- [使用 MCP 自定义工具](../tools/custom-mcp-tools.md) — 构建和连接自己的 MCP 服务器
- [注册本地工具](../tools/register-tool.md) — 替代方案：Java 原生 FunctionHub 工具

---

[上一章：什么是 ReAct？](./what-is-react.md)
[下一章：什么是 A2A？](./what-is-a2a.md)
[回到首页](../readme.md)
