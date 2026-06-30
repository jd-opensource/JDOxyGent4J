# Trust Mode 示例

演示 Agent 在信任模式（Trust Mode）与普通模式下执行工具调用时的行为差异。信任模式下 Agent 跳过额外的安全校验，直接执行工具；普通模式遵循标准校验流程。

## 前置条件

- JDK 17+
- 安装 `uvx`（用于 MCP 时间工具）
- 设置环境变量：`OXY_LLM_API_KEY`、`OXY_LLM_BASE_URL`、`OXY_LLM_MODEL_NAME`

## 完整代码

```java
package com.jd.oxygent.core.oxygent.samples.examples.advanced;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.MasFactoryRegistry;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.OSUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                ReActAgent.builder()
                        .name("normal_agent")
                        .tools(List.of("time_tools"))
                        .llmModel("default_llm")
                        .trustMode(false)  // 普通模式
                        .build(),
                ReActAgent.builder()
                        .name("trust_agent")
                        .tools(List.of("time_tools"))
                        .llmModel("default_llm")
                        .trustMode(true)   // 信任模式
                        .build()
        );
    }

    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(
                Thread.currentThread().getStackTrace()[1].getClassName());
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

## 运行方式

```bash
java -cp <classpath> com.jd.oxygent.core.oxygent.samples.examples.advanced.DemoTrustMode
```

## 预期输出

两种模式均返回时间结果，但执行路径不同：

```
normal mode output: The current time in Asia/Shanghai is 2025-01-15T14:30:00+08:00
trust mode output: 2025-01-15T14:30:00+08:00
```

信任模式下 Agent 直接将工具返回结果作为输出，普通模式下 Agent 会对工具结果进行额外的格式化处理。
