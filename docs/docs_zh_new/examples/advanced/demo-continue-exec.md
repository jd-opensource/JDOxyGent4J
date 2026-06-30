# Continue Execution 示例

演示如何从工作流的中间节点恢复执行，适用于中断恢复、人工审核后继续、或从指定中间状态重新执行的场景。

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
import com.jd.oxygent.core.oxygent.samples.server.masprovider.MasFactoryRegistry;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import com.jd.oxygent.core.oxygent.utils.OSUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // 从中间节点恢复执行
        Map<String, Object> payload = new HashMap<>();
        payload.put("query", "Get what time it is Asia/Shanghai");
        // 传入首次调用时获取的中间 node_id
        payload.put("restart_node_id", "c6l6AFs9Ti6sIBvYMYVqpA");
        // 传入该节点的输出作为恢复起点
        payload.put("restart_node_output", "{\n" +
                "\"timezone\": \"Asia/Shanghai\",\n" +
                "\"datetime\": \"2024-10-14T06:18:00+08:00\",\n" +
                "\"day_of_week\": \"Tuesday\",\n" +
                "\"is_dst\": false\n" +
                "}");

        OxyResponse result = mas.chatWithAgent(payload);
        System.out.printf("LLM-second: %s", result.getOutput());
    }
}
```

## 关键参数

| 参数 | 说明 |
|------|------|
| `restart_node_id` | 首次执行时记录的中间节点 ID |
| `restart_node_output` | 该节点的输出结果，作为恢复执行的输入 |

## 运行方式

```bash
java -cp <classpath> com.jd.oxygent.core.oxygent.samples.examples.advanced.DemoContinueExec
```

## 预期输出

Agent 跳过工具调用阶段，直接从提供的中间结果继续执行 LLM 推理：

```
LLM-second: The current time in Asia/Shanghai is 2024-10-14 06:18:00 (Tuesday, not DST).
```
