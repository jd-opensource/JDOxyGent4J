# 如何获取智能体原始输出？

## 简介

OxyGent4J 提供了丰富的参数供您自定义智能体的工作模式。如果您希望获取智能体的原始输出（工具执行的直接结果），只需在构建 Agent 时将 `trustMode` 设置为 `true`。

启用信任模式后，智能体会直接返回工具的执行结果，而不再对其进行额外的 LLM 处理或解析。

## 基本用法

```java
ReActAgent.builder()
    .name("trust_agent")
    .desc("a time query agent with trust mode enabled")
    .tools(List.of("time_tools"))
    .llmModel("default_llm")
    .trustMode(true)   // 启用信任模式
    .build()
```

## 信任模式 vs 普通模式

| 模式 | 行为 | 适用场景 |
|------|------|----------|
| `trustMode(false)` | 工具结果经 LLM 二次总结后返回 | 需要自然语言回答 |
| `trustMode(true)` | 直接返回工具执行的原始结果 | 需要精确数据、API 响应 |

例如，启用信任模式时，返回的原始输出如下：

```
trust mode output: Tool [get_current_time] execution result: {
  "timezone": "Asia/Shanghai",
  "datetime": "2025-07-24T20:26:19+08:00",
  "is_dst": false
}
```

## 完整示例

```java
import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.*;

public class DemoTrustMode {

    @OxySpaceBean(value = "demoTrustMode", defaultStart = true, query = "What is the current time")
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
            HttpLlm.builder()
                .name("default_llm")
                .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                .build(),

            new StdioMCPClient("time_tools", "uvx",
                Arrays.asList("mcp-server-time", "--local-timezone=Asia/Shanghai")),

            // 普通模式 Agent
            ReActAgent.builder()
                .name("normal_agent")
                .tools(List.of("time_tools"))
                .llmModel("default_llm")
                .trustMode(false)
                .build(),

            // 信任模式 Agent
            ReActAgent.builder()
                .name("trust_agent")
                .tools(List.of("time_tools"))
                .llmModel("default_llm")
                .trustMode(true)
                .build()
        );
    }

    public static void main(String[] args) throws Exception {
        Mas mas = MasFactoryRegistry.getFactory().createMas();

        Object normalResult = mas.call("normal_agent",
            new HashMap<>(Map.of("query", "What is the current time")));
        Object trustResult = mas.call("trust_agent",
            new HashMap<>(Map.of("query", "What is the current time")));

        System.out.printf("normal mode output: %s%n", normalResult);
        System.out.printf("trust mode output: %s%n", trustResult);
    }
}
```

## 错误处理

启用 `trustMode` 后，对于框架可以捕获的异常会进行错误重试。如果重试失败，`ReActAgent` 会将错误上报，而不是尝试用 LLM 进行解释。

## 配置参数

在 `ReActAgent.builder()` 中与信任模式相关的参数：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `trustMode` | `boolean` | `false` | 是否启用信任模式 |
| `toolCallPrefixIncluded` | `boolean` | `false` | 是否包含工具调用前缀信息 |

---

[上一章：多智能体系统](../multi-agent/multi-agent-system.md)
[下一章：工作流](./workflow.md)
[回到首页](../readme.md)
