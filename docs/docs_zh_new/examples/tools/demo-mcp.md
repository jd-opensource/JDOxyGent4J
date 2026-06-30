# MCP Tool 示例

演示如何通过 Model Context Protocol（MCP）接入外部工具服务。本例使用 `mcp-server-time` 实现时间查询功能。

## 前置条件

- JDK 17+
- 安装 `uvx`（Python 包管理工具）
- 设置环境变量：`OXY_LLM_API_KEY`、`OXY_LLM_BASE_URL`、`OXY_LLM_MODEL_NAME`

## 完整代码

```java
package com.jd.oxygent.core.oxygent.samples.examples.tools;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import com.jd.oxygent.core.oxygent.utils.OSUtil;

import java.util.Arrays;
import java.util.List;

public class DemoMCP {

    @OxySpaceBean(value = "MCPToolJavaOxySpace", defaultStart = true, query = "What time is it")
    public static List<BaseOxy> getDefaultOxySpace() {
        var apiKey = EnvUtils.getEnv("OXY_LLM_API_KEY");
        var baseUrl = EnvUtils.getEnv("OXY_LLM_BASE_URL");
        var modelName = EnvUtils.getEnv("OXY_LLM_MODEL_NAME");

        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .build(),
                OSUtil.isWindows() ?
                        new StdioMCPClient("time", "cmd.exe", Arrays.asList("/c", "uvx",
                                "mcp-server-time", "--local-timezone=Asia/Shanghai"))
                        :
                        new StdioMCPClient("time", "uvx", Arrays.asList(
                                "mcp-server-time", "--local-timezone=Asia/Shanghai")),
                ReActAgent.builder()
                        .name("time_agent")
                        .desc("Tool agent capable of querying time")
                        .additionalPrompt("Do not send any information other than time information.")
                        .tools(Arrays.asList("time"))
                        .trustMode(false)
                        .build(),
                ReActAgent.builder()
                        .isMaster(true)
                        .name("master_agent")
                        .subAgents(Arrays.asList("time_agent"))
                        .build()
        );
    }

    public static void main(String[] args) throws Exception {
        var currentClassName = Thread.currentThread().getStackTrace()[1].getClassName();
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(currentClassName);
        ServerApp.main(args);
    }
}
```

## 运行方式

```bash
java -cp <classpath> com.jd.oxygent.core.oxygent.samples.examples.tools.DemoMCP
```

## 预期输出

发送 `What time is it`，master_agent 委托 time_agent 通过 MCP 调用时间工具，返回当前时间：

```
The current time in Asia/Shanghai is 2025-01-15T14:30:00+08:00
```
