# 如何获取智能体原始输出？

OxyGent提供了非常丰富的参数供您自定义智能体的工作模式，

如果您希望获取智能体的原始输出，只需将 trustMode 设置为 true。启用信任模式后，智能体会直接返回工具的执行结果，而不是对其进行额外的处理或解析。

```java
ReActAgent.builder()
    .name("trust_agent")
    .desc("a time query agent with trust mode enabled")
    .tools(Arrays.asList("time_tools"))
    .llmModel("default_llm")
    .trustMode(true)  // enable trust mode
    .isMaster(true)
    .build()
```

例如，启用信任模式时，返回的原始输出可能如下所示：

```
trust mode output: Tool [get_current_time] execution result: {
  "timezone": "Asia/Shanghai",
  "datetime": "2025-07-24T20:26:19+08:00",
  "is_dst": false
}
```

如果开启了`trustMode`，对于框架可以捕获的异常，或进行错误重试，否则`ReActAgent`会将错误上报。

## 完整的可运行样例

以下是可运行的完整代码示例（参考 [DemoTrustMode.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/advanced/DemoTrustMode.java)）：

```java
package com.jd.oxygent.examples.advanced;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 信任模式演示类
 * 展示了信任模式和普通模式在输出处理上的差异
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class TrustModeExample {

    /**
     * 获取包含信任模式和普通模式代理的OxySpace配置
     *
     * @return 包含LLM、工具和两种模式代理的BaseOxy列表
     */
    @OxySpaceBean(value = "trustModeOxySpace", defaultStart = true, query = "What is the current time")
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
                // LLM配置
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .llmParams(Map.of("temperature", 0.01))
                        .semaphoreCount(4)
                        .build(),

                // 时间工具
                new StdioMCPClient("time_tools", "uvx",
                        Arrays.asList("mcp-server-time", "--local-timezone=Asia/Shanghai")),

                // 普通模式ReActAgent
                ReActAgent.builder()
                        .name("normal_agent")
                        .desc("a time query agent with trust mode disabled")
                        .tools(Arrays.asList("time_tools"))
                        .llmModel("default_llm")
                        .trustMode(false)  // disable trust mode
                        .build(),

                // 信任模式ReActAgent
                ReActAgent.builder()
                        .name("trust_agent")
                        .desc("a time query agent with trust mode enabled")
                        .tools(Arrays.asList("time_tools"))
                        .llmModel("default_llm")
                        .trustMode(true)  // enable trust mode
                        .isMaster(true)
                        .build()
        );
    }

    /**
     * 演示信任模式与普通模式的差异
     * 分别调用两个代理并比较输出结果
     */
    public static void demonstrateTrustModeComparison() {
        try {
            var mas = new Mas("trust_mode_app", getDefaultOxySpace());
            var query = Map.of("query", "What is the current time");

            System.out.println("=== normal mode test ===");
            var normalResult = mas.call("normal_agent", query);
            System.out.printf("normal mode output: %s%n", normalResult);

            System.out.println("\n=== trust mode test ===");
            var trustResult = mas.call("trust_agent", query);
            System.out.printf("trust mode output: %s%n", trustResult);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 应用程序主入口点
     * 启动Spring Boot应用或演示信任模式
     *
     * @param args 命令行参数
     * @throws Exception 当应用启动失败时抛出异常
     */
    public static void main(String[] args) throws Exception {
        // 如果是演示模式，运行信任模式比较
        if (args.length > 0 && "demo".equals(args[0])) {
            demonstrateTrustModeComparison();
            return;
        }

        // 否则启动Web服务
        var currentClassName = Thread.currentThread().getStackTrace()[1].getClassName();
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(currentClassName);
        ServerApp.main(args);
    }
}
```

该示例演示了：
- 普通模式 (`trustMode=false`): 智能体会处理和解析工具输出
- 信任模式 (`trustMode=true`): 直接返回工具的原始执行结果

**运行前准备**:
- 确保已安装Node.js和uvx工具
- MCP server会自动启动时间服务

[上一章：并行调用agent](./07-01-parallel.md)
[下一章：处理查询和提示词](./08-04-update_prompts.md)
[回到首页](../../README_zh.md)