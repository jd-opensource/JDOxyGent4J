# 如何使用自定义MCP工具？

在OxyGent中，您可以通过本地模式或SSE模式注册自定义MCP工具。

## 1.本地MCP工具

首先，创建一个 `mcp_servers` 文件夹，并在 `/mcp_servers/math_tools.py` 文件中使用 `FastMCP` 声明一个 MCP 实例：

```python
# mcp_servers/math_tools.py
import math
from decimal import Decimal, getcontext

from mcp.server.fastmcp import FastMCP
from pydantic import Field

# Initialize FastMCP server instance
mcp = FastMCP()
```

接着，您可以使用类似 `FunctionHub` 的方式注册工具：

```python
# mcp_servers/math_tools.py
@mcp.tool(description="Power calculator tool")
def power(
    n: int = Field(description="base"), m: int = Field(description="index", default=2)
) -> int:
    return math.pow(n, m)
# other tools...
```

然后，您可以在 `oxySpace` 中调用这些工具：

```java
StdioMCPClient.builder()
    .name("math_tools")
    .command("uv")
    .args(Arrays.asList("--directory", "./mcp_servers", "run", "math_tools.py"))
    .build();
```
## 完整的可运行样例

以下是完整的代码示例，展示了如何在 OxyGent 中使用多个 LLM 和一个 Agent，并调用自定义的 MCP 工具：
```java
package com.jd.oxygent.examples.tools;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.tools.PresetTools;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 演示如何使用自定义MCP工具
 * 展示了StdioMCPClient的使用方法和多个MCP工具的集成
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class CustomMCPToolExample {

    /**
     * 获取包含自定义MCP工具的OxySpace配置
     *
     * @return 包含LLM、MCP工具和代理的BaseOxy列表
     */
    @OxySpaceBean(value = "customMCPToolOxySpace", defaultStart = true, query = "Hello! 现在几点了？请帮我计算一下 2 的 8 次方，然后将结果保存到文件中。")
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
                // HTTP LLM配置
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .llmParams(Map.of("temperature", 0.01))
                        .semaphoreCount(4)
                        .timeout(240)
                        .build(),

                // 时间工具MCP客户端
                new StdioMCPClient("time_tools", "uvx",
                        Arrays.asList("mcp-server-time", "--local-timezone=Asia/Shanghai")),

                // 自定义数学工具MCP客户端
                new StdioMCPClient("math_tools", "uv",
                        Arrays.asList("--directory", "./mcp_servers", "run", "math_tools.py")),

                // 预设文件工具
                PresetTools.FILE_TOOLS,

                // 主代理，集成所有工具
                ReActAgent.builder()
                        .name("master_agent")
                        .isMaster(true)
                        .llmModel("default_llm")
                        .tools(Arrays.asList("file_tools", "time_tools", "math_tools"))
                        .build()
        );
    }

    /**
     * 应用程序主入口点
     * 启动Spring Boot应用并初始化MCP工具集成
     *
     * @param args 命令行参数
     * @throws Exception 当应用启动失败时抛出异常
     */
    public static void main(String[] args) throws Exception {
        var currentClassName = Thread.currentThread().getStackTrace()[1].getClassName();
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(currentClassName);
        ServerApp.main(args);
    }
}
```


## 2.SSE MCP工具

如果需要使用SSE MCP工具，您可以在声明`FastMCP`对象时增加端口参数：

```python
mcp = FastMCP("math_tools", port=8000)
```

然后您可以通过在`sse_url`中传入端口的方式注册工具到OxyGent：

```java
SSEMCPClient.builder()
    .name("math_tools")
    .sseUrl("http://127.0.0.1:8000/sse")
    .build();
```

[上一章：使用MCP开源工具](./02-03-use_opensource_tools.md)
[下一章：管理工具调用](./02-02-manage_tools.md)
[回到首页](./readme.md)