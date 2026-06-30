# 分布式多 Agent 示例

演示如何部署分布式多 Agent 系统，由 Master Agent 通过 SSE 协议协调远端的 Math Agent 和 Time Agent 协作完成任务。

## 前置条件

- JDK 17+
- 安装 `uvx`（用于 MCP 时间工具）和 `npx`（用于文件系统工具）
- 设置环境变量：`OXY_LLM_API_KEY`、`OXY_LLM_BASE_URL`、`OXY_LLM_MODEL_NAME`

## 架构说明

```
[Master Agent :8090] --SSE--> [Math Agent :8091] --SSE--> [Time Agent :8092]
       |                              |
       +-- file_tools (MCP)           +-- math_tools (MCP)
```

## AppMasterAgent（端口 8090）

```java
package com.jd.oxygent.core.oxygent.samples.examples.distributed;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.SSEAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import com.jd.oxygent.core.oxygent.utils.OSUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AppMasterAgent {

    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .llmParams(Map.of("temperature", 0.01))
                        .semaphoreCount(4)
                        .build(),
                new StdioMCPClient("file_tools", "npx",
                        Arrays.asList("-y", "@modelcontextprotocol/server-filesystem", "./local_file")),
                ReActAgent.builder()
                        .name("file_agent")
                        .desc("A tool for querying local files")
                        .tools(Arrays.asList("file_tools"))
                        .build(),
                ReActAgent.builder()
                        .isMaster(true)
                        .name("master_agent")
                        .subAgents(Arrays.asList("file_agent", "math_agent"))
                        .build(),
                SSEAgent.builder()
                        .name("math_agent")
                        .desc("A tool for mathematical calculations")
                        .serverUrl("http://127.0.0.1:8081")
                        .isOxyAgent(true)
                        .isShareCallStack(false)
                        .build()
        );
    }

    public static void main(String[] args) throws Exception {
        ServerApp.main(new String[]{"-p", "8090"});
    }
}
```

## AppMathAgent（端口 8091）

```java
// WorkflowAgent：调用 time_agent 获取当前时间秒数，再调用 calc_pi 计算对应精度的 Pi
WorkflowAgent.builder()
        .isMaster(true)
        .name("master_agent")
        .subAgents(Arrays.asList("time_agent"))
        .tools(Arrays.asList("math_tools"))
        .funcWorkflow(x -> {
            OxyResponse timeResp = x.call(Map.of("callee", "time_agent",
                    "arguments", Map.of("query", "What time is it now?")));
            // 从时间中提取数字作为精度
            String n = extractLastNumber(timeResp.getOutput());
            OxyResponse piResp = x.call(Map.of("callee", "calc_pi",
                    "arguments", Map.of("prec", n)));
            return String.format("Save %s positions: %s", n, piResp.getOutput());
        })
        .build()
```

## AppTimeAgent（端口 8092）

```java
// ReActAgent 配合 mcp-server-time 提供时间查询服务
ReActAgent.builder()
        .name("time_agent")
        .desc("A tool for time query")
        .tools(Arrays.asList("time_tools"))
        .build()
```

## 运行方式

依次启动三个服务（需要三个终端）：

```bash
# 终端 1 - Time Agent
java -cp <classpath> com.jd.oxygent.core.oxygent.samples.examples.distributed.AppTimeAgent

# 终端 2 - Math Agent
java -cp <classpath> com.jd.oxygent.core.oxygent.samples.examples.distributed.AppMathAgent

# 终端 3 - Master Agent
java -cp <classpath> com.jd.oxygent.core.oxygent.samples.examples.distributed.AppMasterAgent
```

## 预期输出

发送 `The first 30 positions of pi` 到 Master Agent（:8090），Master 将请求路由至 Math Agent，Math Agent 调用 Time Agent 和 calc_pi 工具：

```
Save 30 positions: 3.141592653589793238462643383279
```
