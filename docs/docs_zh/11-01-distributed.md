# 如何分布式运行智能体？

OxyGent MAS支持操作简单的分布式调用。您可以使用`SSEAgent`连接远端运行的agent，能够和本地agent以相同的方式运行。

考虑[如何自定义处理提示词？](./08-04-update_prompts.md)中的例子入手，我们可以创建一个分布式的获取时间的智能体：

```java
// AppTimeAgent.java
package com.jd.oxygent.examples.distributed;

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

public class AppTimeAgent {

    @OxySpaceBean(value = "appTimeAgent", defaultStart = true, query = "What time is it now?")
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
            HttpLlm.builder()
                .name("default_llm")
                .apiKey(System.getenv("OXY_LLM_API_KEY"))
                .baseUrl(System.getenv("OXY_LLM_BASE_URL"))
                .modelName(System.getenv("OXY_LLM_MODEL_NAME"))
                .llmParams(Map.of("temperature", 0.01))
                .timeout(240)
                .build(),

            new StdioMCPClient("time_tools", "uvx",
                Arrays.asList("mcp-server-time", "--local-timezone=Asia/Shanghai")),

            ReActAgent.builder()
                .name("time_agent")
                .desc("A tool for time query")
                .isMaster(true)
                .tools(Arrays.asList("time_tools"))
                .llmModel("default_llm")
                .timeout(10)
                .build()
        );
    }

    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(new String[]{"-p", "8092"});// 设置服务端口
    }
}
```

然后您可以使用`SSEAgent`替代原有的time_agent：

```java
SSEAgent.builder()
    .name("time_agent")
    .desc("Remote time query agent")
    .serverUrl("http://127.0.0.1:8082") // 替换为AppTimeAgent实际所在的位置
    .isOxyAgent(true)
    .build()
```

如果您使用localhost，可以使用以下的简单脚本启动分布式服务：

```bash
#!/bin/bash

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR:${NC} $1"
}

cleanup() {
    log "Cleaning up processes..."
    jobs -p | xargs -r kill 2>/dev/null || true
    wait 2>/dev/null || true
    log "Cleanup complete"
}

trap cleanup EXIT INT TERM

start_service() {
    local cmd=$1
    local name=$2
    local wait_time=${3:-5}

    log "Starting $name..."
    $cmd &
    local pid=$!

    sleep $wait_time

    # Check if the process is still running
    if kill -0 $pid 2>/dev/null; then
        log "$name started successfully (PID: $pid)"
        return 0
    else
        error "$name failed to start"
        return 1
    fi
}

main() {
    log "Starting distributed services..."

    # 启动Java应用 - 使用maven exec插件
    start_service "mvn exec:java -Dexec.mainClass='com.jd.oxygent.core.oxygent.samples.examples.distributed.AppTimeAgent' -pl oxygent-studio" "TimeAgent" 5
    start_service "mvn exec:java -Dexec.mainClass='com.jd.oxygent.core.oxygent.samples.examples.distributed.AppMasterAgent' -pl oxygent-studio" "MasterAgent" 5

    log "All services have been started"
    log "Press Ctrl+C to stop all services"

    wait
}

main "$@"
```

## 数学Agent分布式服务

除了时间Agent，您还可以创建专门的数学计算服务（参考 `AppMathAgent.java`）：

```java
@OxySpaceBean(value = "appMathAgentJavaOxySpace", defaultStart = true, query = "The first 30 positions of pi")
public static List<BaseOxy> getDefaultOxySpace() {
    return Arrays.asList(
        HttpLlm.builder()
            .name("default_llm")
            .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
            .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
            .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
            .llmParams(Map.of("temperature",0.01))
            .semaphoreCount(4)
            .build(),

        // MCP数学工具服务
        new StdioMCPClient("math_tools", "uv",
            Arrays.asList("--directory", "/path/to/mcp_servers/", "run", "math_tools.py")),

        // 远程时间Agent连接
        SSEAgent.builder()
            .name("time_agent")
            .desc("An tool for time query")
            .serverUrl("http://127.0.0.1:8092")  // 连接到另一个时间服务
            .isOxyAgent(true)
            .build(),

        // 数学计算工作流
        WorkflowAgent.builder()
            .isMaster(true)
            .name("master_agent")
            .desc("An tool for pi query")
            .subAgents(Arrays.asList("time_agent"))
            .tools(Arrays.asList("math_tools"))
            .funcWorkflow(x -> {
                // 获取内存和查询
                List<Map<String, Object>> currentShortMemory = x.getShortMemory(false);
                String masterQuery = x.getQuery(true);

                // 调用远程时间Agent
                OxyResponse timeResponse = x.call(new HashMap<>(Map.of(
                    "callee", "time_agent",
                    "arguments", new HashMap<>(Map.of("query", "What time is it now?"))
                )));

                // 解析数字并计算Pi
                Pattern pattern = Pattern.compile("\\d+");
                Matcher matcher = pattern.matcher(timeResponse.getOutput().toString());
                String n = null;
                while (matcher.find()) {
                    n = matcher.group();
                }

                if (n != null) {
                    OxyResponse piResponse = x.call(new HashMap<>(Map.of(
                        "callee", "calc_pi",
                        "arguments", new HashMap<>(Map.of("prec", n))
                    )));
                    return String.format("Save %s positions: %s", n, piResponse.getOutput());
                } else {
                    return "Save 2 positions: 3.14, or you could ask me to save how many positions you want.";
                }
            })
            .build()
    );
}

public static void main(String[] args) throws Exception {
    GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
    ServerApp.main(new String[]{"-p", "8091"});// 独立端口运行
}
```

### 分布式架构模式

| 组件                 | 端口   | 功能     | 依赖           |
| ------------------ | ---- | ------ | ------------ |
| **AppMathAgent**   | 8091 | 数学计算服务 | 连接时间服务(8092) |
| **AppTimeAgent**   | 8082 | 时间查询服务 | 独立MCP时间工具    |
| **AppMasterAgent** | 8090 | 主协调服务  | 连接时间服务(8082) |

## 完整的可运行样例

你可以通过以下方式运行分布式示例：

```bash
# 运行时间Agent服务 (端口8082)
mvn exec:java -Dexec.mainClass="com.jd.oxygent.core.oxygent.samples.examples.distributed.AppTimeAgent" -pl oxygent-core

# 运行数学Agent服务 (端口8091)
mvn exec:java -Dexec.mainClass="com.jd.oxygent.core.oxygent.samples.examples.distributed.AppMathAgent" -pl oxygent-core

# 运行主协调Agent服务 (端口8090)
mvn exec:java -Dexec.mainClass="com.jd.oxygent.core.oxygent.samples.examples.distributed.AppMasterAgent" -pl oxygent-core
```

**参考现有示例**:

- [`AppTimeAgent.java`](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/distributed/AppTimeAgent.java) - 时间查询分布式服务
- [`AppMathAgent.java`](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/distributed/AppMathAgent.java) - 数学计算分布式服务
- [`AppMasterAgent.java`](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/distributed/AppMasterAgent.java) - 主协调分布式服务

```java
package com.jd.oxygent.examples.distributed;

import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.ParallelAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.SSEAgent;
import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.tools.ParamMetaAuto;
import com.jd.oxygent.core.oxygent.tools.Tool;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class AppMasterAgent {

    // 自定义文件工具
    public static class FileTools extends FunctionHub {
        public FileTools() {
            super("file_tools");
            this.setDesc("文件操作工具集合");
        }

        @Tool(name = "write_file", description = "写入文件内容",
              paramMetas = {
                  @ParamMetaAuto(name = "path", type = "String", description = "文件路径"),
                  @ParamMetaAuto(name = "content", type = "String", description = "文件内容")
              })
        public static String writeFile(String path, String content) {
            try {
                Files.writeString(Path.of(path), content, StandardCharsets.UTF_8);
                return "Successfully wrote to " + path;
            } catch (IOException e) {
                return "Error writing file: " + e.getMessage();
            }
        }

        @Tool(name = "read_file", description = "读取文件内容",
              paramMetas = {
                  @ParamMetaAuto(name = "path", type = "String", description = "文件路径")
              })
        public static String readFile(String path) {
            try {
                return Files.readString(Path.of(path), StandardCharsets.UTF_8);
            } catch (IOException e) {
                return "Error reading file: " + e.getMessage();
            }
        }
    }

    // 查询更新函数 - Java版本使用Function接口
    private static final Function<OxyRequest, OxyRequest> UPDATE_QUERY = (oxyRequest) -> {
        String userQuery = oxyRequest.getQuery(); // Java版本简化实现
        String currentQuery = oxyRequest.getQuery();
        oxyRequest.setQuery(String.format("user query is %s\ncurrent query is %s", userQuery, currentQuery));
        return oxyRequest;
    };

    @OxySpaceBean(value = "appMasterAgent", defaultStart = true, query = "Hello!")
    public static List<BaseOxy> getDefaultOxySpace() {
        // 设置默认LLM模型
        Config.getAgent().setLlmModel("default_llm");

        return Arrays.asList(
            // LLM配置
            HttpLlm.builder()
                .name("default_llm")
                .apiKey(System.getenv("OXY_LLM_API_KEY"))
                .baseUrl(System.getenv("OXY_LLM_BASE_URL"))
                .modelName(System.getenv("OXY_LLM_MODEL_NAME"))
                .llmParams(Map.of("temperature", 0.01))
                .timeout(240)
                .build(),

            // MCP时间工具
            new StdioMCPClient("time_tools", "uvx",
                Arrays.asList("mcp-server-time", "--local-timezone=Asia/Shanghai")),

            // 文件工具
            new FileTools(),

            // 文件代理
            ReActAgent.builder()
                .name("file_agent")
                .desc("A tool that can operate the file system")
                .tools(Arrays.asList("file_tools"))
                .funcProcessInput(UPDATE_QUERY)
                .build(),

            // 远程时间代理
            SSEAgent.builder()
                .name("time_agent")
                .desc("Remote time query agent")
                .serverUrl("http://127.0.0.1:8082")
                .isOxyAgent(true)
                .build(),

            // 文本摘要代理
            ChatAgent.builder()
                .name("text_summarizer")
                .desc("A tool that can summarize markdown text")
                .prompt("你是一个文档摘要专家，请对提供的markdown文本进行简要总结。")
                .funcProcessInput(UPDATE_QUERY)
                .build(),

            // 数据分析代理
            ChatAgent.builder()
                .name("data_analyser")
                .desc("A tool that can summarize echart data")
                .prompt("你是一个数据分析专家，请对提供的图表数据进行分析总结。")
                .funcProcessInput(UPDATE_QUERY)
                .build(),

            // 文档检查代理
            ChatAgent.builder()
                .name("document_checker")
                .desc("A tool that can find problems in document")
                .prompt("你是一个文档审查专家，请检查文档中可能存在的问题。")
                .funcProcessInput(UPDATE_QUERY)
                .build(),

            // 并行分析代理
            ParallelAgent.builder()
                .name("analyzer")
                .desc("A tool that analyze markdown document")
                .permittedToolNameList(Arrays.asList("text_summarizer", "data_analyser", "document_checker"))
                .funcProcessInput(UPDATE_QUERY)
                .build(),

            // 主代理
            ReActAgent.builder()
                .name("master_agent")
                .isMaster(true)
                .subAgents(Arrays.asList("file_agent", "time_agent", "analyzer"))
                .build()
        );
    }

    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(new String[]{"-p", "8090"});// 设置主服务端口
    }
}
```

[上一章：获取记忆和重新生成](./09-01-continue_exec.md)
[下一章：使用多模态智能体](./10_multimodal.md)
[回到首页](./readme.md)