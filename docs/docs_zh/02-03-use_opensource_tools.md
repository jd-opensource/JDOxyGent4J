# 如何使用开源MCP工具？

在使用MCP的过程中，您可能希望使用外部工具。OxyGent支持如同本地工具一样集成外部的开源工具，您可以使用基于MCP协议的`StdioMCPClient`引入外部工具。

## 基础MCP工具集成

例如，如果您希望使用工具获取时间，您可以使用`mcp-server-time`工具：

```java
StdioMCPClient.builder()
    .name("time_tools")
    .command("uvx")
    .args(Arrays.asList("mcp-server-time", "--local-timezone=Asia/Shanghai"))
    .build();
```

## 常用开源MCP工具

### 1. 时间工具
```java
// 时间查询工具
new StdioMCPClient("time", "uvx",
    Arrays.asList("mcp-server-time", "--local-timezone=Asia/Shanghai"))
```

### 2. 文件系统工具
```java
// 文件系统操作工具
new StdioMCPClient("file_tools", "npx",
    Arrays.asList("-y", "@modelcontextprotocol/server-filesystem", "./local_file"))
```

### 3. 浏览器工具
```java
// Web浏览和搜索工具
new StdioMCPClient("browser", "npx",
    Arrays.asList("-y", "@modelcontextprotocol/server-brave-search"))
```

## 完整的可运行样例

以下是开源MCP工具集成的完整代码示例（参考 [DemoMCP.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/tools/DemoMCP.java)）：

```java
package com.jd.oxygent.examples.tools;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * MCP开源工具集成演示类
 * 展示了如何集成多种开源MCP工具并在智能体中使用
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class OpensourceMCPToolsExample {

    @OxySpaceBean(value = "opensourceMCPOxySpace", defaultStart = true,
                  query = "What time is it now? Please also list files in current directory.")
    public static List<BaseOxy> getDefaultOxySpace() {
        // 获取环境变量
        String apiKey = System.getenv("OXY_LLM_API_KEY");
        String baseUrl = System.getenv("OXY_LLM_BASE_URL");
        String modelName = System.getenv("OXY_LLM_MODEL_NAME");

        // 跨平台MCP命令配置
        String mcpCommand = System.getProperty("os.name").toLowerCase().contains("win") ? "cmd" : "uvx";
        List<String> timeArgs = System.getProperty("os.name").toLowerCase().contains("win")
            ? Arrays.asList("/c", "uvx", "mcp-server-time", "--local-timezone=Asia/Shanghai")
            : Arrays.asList("mcp-server-time", "--local-timezone=Asia/Shanghai");

        return Arrays.asList(
            // LLM配置
            HttpLlm.builder()
                .name("default_llm")
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .llmParams(Map.of("temperature", 0.01))
                .timeout(240)
                .build(),

            // 时间工具MCP客户端
            new StdioMCPClient("time", mcpCommand, timeArgs),

            // 文件系统工具MCP客户端
            new StdioMCPClient("file_tools", "npx",
                Arrays.asList("-y", "@modelcontextprotocol/server-filesystem", "./local_file")),

            // 浏览器搜索工具MCP客户端（可选）
            // new StdioMCPClient("browser", "npx",
            //     Arrays.asList("-y", "@modelcontextprotocol/server-brave-search")),

            // 时间查询专用智能体
            ReActAgent.builder()
                .name("time_agent")
                .desc("专门处理时间查询的代理")
                .tools(Arrays.asList("time"))
                .trustMode(false)
                .build(),

            // 文件操作专用智能体
            ReActAgent.builder()
                .name("file_agent")
                .desc("专门处理文件操作的代理")
                .tools(Arrays.asList("file_tools"))
                .trustMode(false)
                .build(),

            // 主协调智能体
            ReActAgent.builder()
                .isMaster(true)
                .name("master_agent")
                .subAgents(Arrays.asList("time_agent", "file_agent"))
                .build()
        );
    }

    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(
            Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);
    }
}
```

## 高级MCP工具集成

### 浏览器工具示例 (参考 [`DemoBrowser.java`](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/mcptools/DemoBrowser.java))
```java
// 高级浏览器工具配置
new StdioMCPClient("browser", "npx", Arrays.asList(
    "-y", "@modelcontextprotocol/server-brave-search",
    "--api-key", System.getenv("BRAVE_SEARCH_API_KEY")  // 需要API密钥
))

// 配置使用浏览器工具的智能体
ReActAgent.builder()
    .name("web_search_agent")
    .desc("Web搜索和浏览专家")
    .tools(Arrays.asList("browser"))
    .prompt("You are a web search expert. Use browser tools to find accurate and current information.")
    .trustMode(true)  // 自动执行搜索
    .isSendObservation(true)
    .isSendAnswer(true)
    .isDetailedObservation(true)
    .build()
```

### 多MCP工具协作
```java
// 组合多个开源MCP工具的智能体系统
List<BaseOxy> advancedOxySpace = Arrays.asList(
    // 多个MCP工具
    new StdioMCPClient("time", "uvx", Arrays.asList("mcp-server-time")),
    new StdioMCPClient("weather", "uvx", Arrays.asList("mcp-server-weather")),
    new StdioMCPClient("calculator", "uvx", Arrays.asList("mcp-server-calculator")),
    new StdioMCPClient("file_tools", "npx", Arrays.asList("-y", "@modelcontextprotocol/server-filesystem", "./data")),

    // 专门的工具协调智能体
    ReActAgent.builder()
        .name("tool_coordinator")
        .desc("协调多种MCP工具的使用")
        .tools(Arrays.asList("time", "weather", "calculator", "file_tools"))
        .additionalPrompt("根据用户需求选择最合适的工具组合来完成任务")
        .build()
);
```

**参考现有示例**:
- [DemoMCP.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/tools/DemoMCP.java) - 基础MCP工具集成
- [DemoBrowser.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/mcptools/DemoBrowser.java) - 浏览器MCP工具高级功能
- [DemoHierarchicalAgents.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoHierarchicalAgents.java) - 层次化MCP工具管理

## MCP工具集成最佳实践

### 1. 环境依赖管理
```java
// 好的示例：检查环境依赖
public static boolean checkMCPToolAvailable(String toolName) {
    try {
        ProcessBuilder pb = new ProcessBuilder("which", toolName);
        Process process = pb.start();
        return process.waitFor() == 0;
    } catch (Exception e) {
        return false;
    }
}

// 条件性加载MCP工具
if (checkMCPToolAvailable("uvx")) {
    mcpTools.add(new StdioMCPClient("time", "uvx", Arrays.asList("mcp-server-time")));
}
```

### 2. 错误处理和超时
```java
// 好的示例：配置合理的超时和错误处理
ReActAgent.builder()
    .name("mcp_agent")
    .tools(Arrays.asList("external_mcp_tool"))
    .timeout(120)  // MCP工具可能需要更长时间
    .trustMode(false)  // 外部工具需要谨慎处理
    .build()
```

### 3. 工具权限控制
```java
// 好的示例：分层权限管理
ReActAgent.builder()
    .name("safe_file_agent")
    .tools(Arrays.asList("file_tools"))
    .additionalPrompt("只能在./safe_directory目录下操作文件")
    .trustMode(false)  // 文件操作需要确认
    .build()
```

## 常见问题解答

**Q: 如何安装MCP工具依赖？**

A: 大多数MCP工具通过npm或pip安装。例如：`npm install -g @modelcontextprotocol/server-filesystem` 或 `pip install mcp-server-time`。

**Q: MCP工具调用失败怎么办？**

A: 检查工具是否正确安装、命令路径是否正确、权限是否足够。建议先在命令行测试工具可用性。

**Q: 如何自定义MCP工具参数？**

A: 通过StdioMCPClient的args参数传递，不同工具支持不同参数，参考各工具的文档。

**Q: 多个MCP工具之间如何协作？**

A: 通过主控智能体协调，或使用ParallelAgent并行调用多个工具，然后整合结果。

[上一章：管理工具调用](./02-02-manage_tools.md)
[下一章：使用MCP自定义工具](./02-04-use_mcp_tools.md)
[回到首页](../../README_zh.md)