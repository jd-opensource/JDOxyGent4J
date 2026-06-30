# 架构总览

> 本文介绍 OxyGent4J 的整体架构设计，包括统一执行生命周期、类继承层次、请求/响应流程、名称引用机制、MAS 生命周期和部署模式。

---

## BaseOxy：统一抽象基类

OxyGent4J 中的所有组件（LLM、Agent、Tool、Flow）都继承自同一个基类 `BaseOxy`。`BaseOxy` 使用 Lombok `@SuperBuilder` 和 `@Data` 注解，定义了一套统一的执行生命周期。

### 执行生命周期

每个 Oxy 组件被调用时，都会按以下顺序依次执行各生命周期钩子：

```
调用入口 execute(OxyRequest)
│
├─ preProcess()           前处理：校验和转换输入
├─ preLog()               前置日志：记录请求信息
├─ preSaveData()          前置持久化：保存请求到数据库
├─ formatInput()          格式化输入
├─ preSendMessage()       前置消息：向前端推送状态
│
├─ beforeExecute()        执行前钩子：最后的准备工作
├─ _execute()             核心执行（抽象方法，由子类实现）
│   └─ 支持重试：retries 次数，delay 间隔
├─ afterExecute()         执行后钩子：结果后处理
│
├─ postProcess()          后处理：转换输出
├─ postLog()              后置日志：记录响应信息
├─ postSaveData()         后置持久化：保存响应到数据库
├─ formatOutput()         格式化输出
└─ postSendMessage()      后置消息：向前端推送结果
```

每个钩子都可以通过构造参数自定义。例如 `funcProcessInput` 可在前处理阶段转换请求，`funcFormatOutput` 可在格式化输出阶段定制响应。

---

## 类继承层次

```
BaseOxy (com.jd.oxygent.core.oxygent.oxy.BaseOxy)
│   @SuperBuilder, @Data
│   统一的执行生命周期、并发控制（Semaphore）、重试、日志、持久化
│
├── BaseTool (oxy/BaseTool.java)
│   │   工具基类
│   ├── FunctionHub / FunctionTool     Java 函数工具
│   ├── BaseMCPClient                  MCP 协议客户端基类
│   │   ├── StdioMCPClient             标准输入/输出 MCP
│   │   ├── SSEMCPClient               SSE 协议 MCP
│   │   └── StreamableMCPClient        Streamable 协议 MCP
│   └── PresetTools                    内置预设工具（静态常量）
│
├── BaseLlm (oxy/llms/BaseLlM.java)
│   │   大语言模型基类
│   ├── RemoteLlm        远程 LLM 基类
│   │   ├── HttpLlm      HTTP API 调用（通用，支持 OpenAI/Ollama/Gemini）
│   │   └── OpenAiLlm    OpenAI SDK 调用
│   └── MockLlm          测试用模拟模型
│
└── BaseFlow (oxy/BaseFlow.java)
    │   流程基类：包含执行逻辑的组件
    │
    └── BaseAgent (oxy/agents/BaseAgent.java)
        │   智能体基类
        ├── LocalAgent           本地智能体基类
        │   ├── ChatAgent        基础对话（单轮 LLM 调用）
        │   ├── ReActAgent       推理-行动循环
        │   ├── ParallelAgent    并行执行
        │   ├── PlanAndSolveAgent 先规划后执行
        │   ├── WorkflowAgent    自定义工作流
        │   ├── RAGAgent         检索增强生成
        │   ├── ShellUseAgent    SSH 远程命令
        │   └── SkillAgent       动态技能加载
        └── RemoteAgent          远程智能体基类
            ├── SSEAgent         SSE 跨进程连接
            └── A2AClientAgent   A2A 协议客户端
```

---

## 请求 / 响应流程

下图展示一次用户请求从进入 MAS 到返回结果的完整流程：

```
用户
 │
 ▼
Mas.chatWithAgent(payload)
 │
 ├─ 创建 OxyRequest（traceId, query, sharedData）
 │
 ▼
Master Agent.execute(oxyRequest)
 │
 ├─ preProcess → preLog → preSaveData
 ├─ formatInput → preSendMessage
 │
 ├─ _execute()    ← ReActAgent 的推理-行动循环
 │   │
 │   ├─ LLM 推理 → "我需要调用 time_agent"
 │   │
 │   ├─ 调用子智能体 time_agent.execute(subRequest)
 │   │   ├─ time_agent._execute()
 │   │   │   ├─ LLM 推理 → "调用 get_time 工具"
 │   │   │   ├─ 工具执行 → "2024-01-01 12:00:00"
 │   │   │   └─ LLM 总结 → "当前时间是..."
 │   │   └─ 返回 OxyResponse
 │   │
 │   └─ LLM 总结 → 最终回答
 │
 ├─ postProcess → postLog → postSaveData
 ├─ formatOutput → postSendMessage
 │
 ▼
OxyResponse（output, state, extra）
 │
 ▼
用户
```

### 核心数据结构

| 结构 | 说明 |
|------|------|
| `OxyRequest` | 请求对象。携带 `traceId`（追踪标识）、`caller`/`callee`（调用方/被调用方）、`query`（查询内容）、`sharedData`（共享数据） |
| `OxyResponse` | 响应对象。携带 `output`（输出文本）、`state`（`OxyState` 枚举）、`extra`（附加数据） |
| `traceId` | 唯一追踪标识，贯穿一次完整的调用链 |

---

## 名称引用机制

OxyGent4J 采用**名称引用**而非 Java 对象引用来连接组件。所有组件在 `oxySpace` 中声明后，由 Mas 统一注册到 `oxyNameToOxy` 字典（`ConcurrentHashMap`）中。

```java
List<BaseOxy> oxySpace = Arrays.asList(
    HttpLlm.builder().name("my_llm").build(),
    calculatorHub,  // name = "calculator"
    ReActAgent.builder()
            .name("my_agent")
            .llmModel("my_llm")               // 通过名称引用 LLM
            .tools(Arrays.asList("calculator")) // 通过名称引用工具
            .subAgents(Arrays.asList("other_agent")) // 通过名称引用子智能体
            .build()
);
```

这种设计带来以下好处：

- **声明式组装**：组件定义和连接关系分离，便于配置化管理。
- **顺序无关**：`oxySpace` 中的组件不需要按依赖顺序排列。
- **延迟绑定**：实际的引用解析发生在 `Mas.init()` 阶段，而非组件构造时。

---

## MAS 生命周期

`Mas` 是 OxyGent4J 的运行时容器，负责管理所有组件的完整生命周期。

```java
Mas mas = new Mas("app", oxySpace);
mas.init();  // 完成初始化

// 使用 MAS...
mas.chatWithAgent(payload);
```

### 初始化阶段（`init()`）

1. **注册组件**：将 `oxySpace` 中的所有组件注册到 `oxyNameToOxy` 字典。
2. **初始化数据库**：连接 Elasticsearch、Redis（如果配置了），否则使用本地降级方案。
3. **初始化所有 Oxy**：按类型顺序初始化 LLM -> Tool -> Flow/Agent。
4. **确定入口**：找到 `isMaster=true` 的智能体作为 `masterAgentName`。
5. **构建组织树**：建立智能体之间的层级关系（`agentOrganization`）。

### 运行阶段

Mas 提供多种运行模式。所有模式最终都通过 `chatWithAgent()` 将请求路由到 master 智能体。

### Spring Boot 集成

OxyGent4J 原生支持 Spring Boot，`Mas` 可通过 `@Autowired` 注入数据库客户端和配置属性：

```java
@Autowired
private BaseEs esClient;       // Elasticsearch 客户端

@Autowired
private BaseCache redisClient; // Redis 客户端
```

---

## 部署模式

### Web 服务模式

启动 Spring Boot 内置服务器，提供 REST API 和 Web UI。

```java
// 通过 ServerApp 启动
ServerApp.main(args);
```

- 默认地址：`http://127.0.0.1:8080`
- `POST /chat` -- 同步对话
- `POST /sse/chat` -- SSE 流式对话
- `POST /async/chat` -- 异步对话
- `GET /get_organization` -- 获取智能体组织树

### CLI 命令行模式

```java
mas.startCliMode("你好！");
```

### 批处理模式

```java
List<String> queries = Arrays.asList("查询1", "查询2", "查询3");
List<Object> results = mas.startBatchProcessing(queries, false);
```

### 编程式调用

```java
OxyResponse response = mas.chatWithAgent(Map.of("query", "你好"));
System.out.println(response.getOutput());
```

---

## 存储层

OxyGent4J 使用可选的存储层实现数据持久化：

| 服务 | 用途 | 降级方案 |
|------|------|----------|
| Elasticsearch | 追踪记录、消息、Prompt、评分 | `LocalEs`（基于文件） |
| Redis | 后端与前端之间的 SSE 消息队列 | `LocalCache`（内存队列） |
| Vearch | 向量数据库，用于工具检索 | 可选，非必需 |

所有存储均为可选。未配置外部数据库时，OxyGent4J 自动使用本地降级方案。

---

## 总结

| 概念 | 说明 |
|------|------|
| BaseOxy | 统一抽象基类，定义执行生命周期，使用 @SuperBuilder |
| 名称引用 | 组件通过 `name` 字符串互相引用 |
| oxySpace | 组件声明列表 (`List<BaseOxy>`) |
| Mas | 运行时容器，管理注册、初始化、路由 |
| OxyRequest / OxyResponse | 请求/响应数据结构 |
| traceId | 调用链追踪标识 |

---

[上一章：概念总览](./overview.md)
[下一章：设置 Config](./config.md)
[回到首页](../readme.md)
