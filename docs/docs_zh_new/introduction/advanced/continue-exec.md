# 如何修改记忆节点并继续执行？

## 简介

OxyGent4J 支持读取记忆及从指定节点重新执行的功能。您可以在 `chatWithAgent` 方法中指定要重启的节点 ID，修改该节点的输出内容并从该节点开始重新运行系统。这对于调试和修正中间结果非常有用。

## 基本用法

### 第一次调用

```java
Mas mas = MasFactoryRegistry.getFactory().createMas();
Map<String, Object> payload = new HashMap<>();
payload.put("query", "Get what time it is Asia/Shanghai");

OxyResponse firstResult = mas.chatWithAgent(payload);
System.out.printf("LLM-first: %s%n", firstResult.getOutput());
// 记录 trace_id 和 node_id 用于后续重新执行
```

### 从指定节点重新执行

```java
Map<String, Object> payload = new HashMap<>();
payload.put("query", "Get what time it is Asia/Shanghai");
payload.put("restart_node_id", "c6l6AFs9Ti6sIBvYMYVqpA");  // 中间节点 ID
payload.put("restart_node_output", "{\n" +
    "\"timezone\": \"Asia/Shanghai\",\n" +
    "\"datetime\": \"2024-10-14T06:18:00+08:00\",\n" +
    "\"day_of_week\": \"Tuesday\",\n" +
    "\"is_dst\": false\n" +
    "}");

OxyResponse secondResult = mas.chatWithAgent(payload);
System.out.printf("LLM-second: %s%n", secondResult.getOutput());
```

## 参数说明

| 参数 | 类型 | 是否必需 | 说明 |
|------|------|----------|------|
| `query` | `String` | 是 | 用户查询内容 |
| `restart_node_id` | `String` | 是 | 需要重新开始的中间节点 ID |
| `restart_node_output` | `String` | 是 | 替换该节点的输出内容 |
| `from_trace_id` | `String` | 否 | 来源 trace_id，用于上下文关联 |
| `reference_trace_id` | `String` | 否 | 参考 trace 编号 |

## Memory 与 maxMessages 配置

OxyGent4J 使用 `Memory` 类管理对话历史记录，支持自动截断以控制上下文长度：

```java
// 默认 maxMessages = 100
Memory memory = new Memory();

// 自定义最大消息数
Memory memory = new Memory(50);

// 运行时修改
memory.setMaxMessages(200);
```

通过 `Config.getAgent().getShortMemorySize()` 可获取全局配置的短期记忆大小（默认为 10 轮对话）。

## 完整示例

```java
public class DemoContinueExec {

    @OxySpaceBean(value = "demoContinueExec", defaultStart = true,
                  query = "Get what time it is Asia/Shanghai")
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
            HttpLlm.builder()
                .name("default_llm")
                .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                .build(),

            new StdioMCPClient("time_tools", "uvx",
                List.of("mcp-server-time", "--local-timezone=Asia/Shanghai")),

            ReActAgent.builder()
                .name("time_agent")
                .desc("A tool for time query.")
                .tools(List.of("time_tools"))
                .llmModel("default_llm")
                .build()
        );
    }

    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(
            Thread.currentThread().getStackTrace()[1].getClassName());
        Mas mas = MasFactoryRegistry.getFactory().createMas();

        // 第一次执行
        Map<String, Object> payload1 = new HashMap<>();
        payload1.put("query", "Get what time it is Asia/Shanghai");
        OxyResponse firstResult = mas.chatWithAgent(payload1);
        System.out.printf("LLM-first: %s%n", firstResult.getOutput());

        // 从指定节点重新执行（修改时间输出）
        Map<String, Object> payload2 = new HashMap<>();
        payload2.put("query", "Get what time it is Asia/Shanghai");
        payload2.put("restart_node_id", "c6l6AFs9Ti6sIBvYMYVqpA");
        payload2.put("restart_node_output", "{\n" +
            "\"timezone\": \"Asia/Shanghai\",\n" +
            "\"datetime\": \"2024-10-14T06:18:00+08:00\",\n" +
            "\"is_dst\": false\n" +
            "}");
        OxyResponse secondResult = mas.chatWithAgent(payload2);
        System.out.printf("LLM-second: %s%n", secondResult.getOutput());
    }
}
```

## 应用场景

- **调试中间步骤**：修改某个工具调用的返回值，观察后续流程变化
- **错误恢复**：某个节点执行失败后，手动提供正确结果并继续
- **A/B 测试**：对同一节点注入不同数据，比较最终输出差异

---

[上一章：工作流](./workflow.md)
[下一章：多模态](./multimodal.md)
[回到首页](../readme.md)
