# Tools API
---

> OxyGent4J 工具系统参考文档：FunctionTool、FunctionHub 和 MCP 客户端。

## 类层次结构

```
BaseOxy → BaseTool
             ├── FunctionTool       — 单个函数工具包装器
             ├── FunctionHub        — 函数工具集合注册中心
             └── BaseMCPClient      — MCP 协议客户端基类
                  ├── StdioMCPClient   — 基于 stdio 传输
                  ├── SSEMCPClient     — 基于 SSE 传输
                  └── StreamableMCPClient — 基于 Streamable HTTP
```

---

## FunctionTool

**包路径**: `com.jd.oxygent.core.oxygent.oxy.function_tools`

将 Java 函数适配为 OxyGent 工具框架的包装器。支持动态函数注册和参数 Schema 自动生成。

### 参数

| 参数 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| `name` | `String` | — | 工具名称（必填） |
| `desc` | `String` | — | 工具描述，用于 LLM 理解工具用途 |
| `funcProcess` | `ToolFunction` | — | 工具函数实现 |
| `params` | `List<ParamMeta>` | `[]` | 参数元数据列表 |

### ParamMeta 结构

| 字段 | 类型 | 描述 |
|------|------|------|
| `name` | `String` | 参数名 |
| `nameType` | `String` | 参数类型（`"string"`, `"number"`, `"boolean"`, `"OxyRequest"`） |
| `description` | `String` | 参数描述 |
| `defaultValue` | `Object` | 默认值（可选） |

### 使用示例

```java
FunctionTool tool = new FunctionTool(
    "get_weather",
    "查询指定城市的天气信息",
    (args) -> WeatherService.query((String) args[0]),
    List.of(new FunctionHub.ParamMeta("city", "string", "城市名称", null))
);
```

---

## FunctionHub

**包路径**: `com.jd.oxygent.core.oxygent.oxy.function_tools`

函数工具的集中注册和管理中心。支持动态注册多个工具，并在初始化时将所有工具注册到 MAS 系统。

### 参数

| 参数 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| `name` | `String` | — | FunctionHub 名称（必填） |
| `tools` | `Map<String, ToolMeta>` | `ConcurrentHashMap` | 已注册工具的线程安全映射 |

### 关键方法

| 方法 | 返回值 | 描述 |
|------|--------|------|
| `registerTool(name, desc, function, params)` | `void` | 注册新工具到 Hub |
| `call(toolName, args...)` | `Object` | 按名称调用已注册的工具 |
| `listTools()` | `Collection<ToolMeta>` | 返回所有已注册工具的元数据 |
| `init()` | `void` | 将所有工具注册到 MAS 系统 |

### 使用示例

```java
// 方式一：编程式注册
FunctionHub hub = new FunctionHub("math_tools");
hub.registerTool(
    "add",
    "两数相加",
    (args) -> Double.parseDouble(args[0].toString()) + Double.parseDouble(args[1].toString()),
    List.of(
        new FunctionHub.ParamMeta("a", "number", "第一个数", null),
        new FunctionHub.ParamMeta("b", "number", "第二个数", null)
    )
);

// 方式二：注解式注册（继承 FunctionHub 并使用 @Tool 注解）
public class MyTools extends FunctionHub {
    @Tool(name = "get_time", description = "获取当前时间",
          paramMetas = @ParamMetaAuto(name = "timezone", type = "string", description = "时区"))
    public String getTime(String timezone) {
        return Instant.now().atZone(ZoneId.of(timezone)).toString();
    }
}
```

---

## BaseMCPClient

**包路径**: `com.jd.oxygent.core.oxygent.oxy.mcp`

MCP（Model Context Protocol）客户端基类，提供与 MCP 服务器通信的基础功能。

### 核心特性

- MCP 服务器连接管理
- 工具自动发现与注册
- 工具调用代理（OxyGent 请求 -> MCP 协议调用）
- 资源生命周期管理

### 参数

| 参数 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| `headers` | `Map<String, String>` | `{}` | MCP 服务器请求头 |
| `isDynamicHeaders` | `boolean` | `false` | 是否使用动态请求头 |
| `isInheritHeaders` | `boolean` | `false` | 是否继承父请求的 headers |
| `isKeepAlive` | `boolean` | 配置文件值 | 是否保持长连接 |

### 子类

| 类名 | 传输方式 | 描述 |
|------|----------|------|
| `StdioMCPClient` | stdio | 通过子进程 stdin/stdout 通信 |
| `SSEMCPClient` | SSE | 通过 Server-Sent Events 通信 |
| `StreamableMCPClient` | Streamable HTTP | 通过 HTTP 流式通信 |

### 使用示例

```java
// Agent 配置中引用 MCP 客户端
ReActAgent agent = ReActAgent.builder()
    .name("code_agent")
    .tools(List.of("my_mcp_client"))  // MCP 客户端名称
    .llmModel("default_llm")
    .build();
```
