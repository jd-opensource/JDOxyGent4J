# 如何分布式运行智能体?

OxyGent4J MAS 支持操作简单的分布式调用。您可以使用 `SSEAgent` 连接远端运行的 OxyGent 服务，能够和本地 Agent 以相同的方式运行。

## 基本概念

分布式部署的核心思路是：
1. 将子智能体部署为独立的 OxyGent4J 服务（各自监听不同端口）
2. 在主服务中使用 `SSEAgent` 代理远程子智能体
3. `SSEAgent` 通过 SSE 协议与远程服务通信，对上层 Agent 透明

## 创建远端智能体服务

以下是一个独立的数学计算智能体服务 `AppMathAgent`，它监听 8081 端口：

```java
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AppMathAgent {

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
            new StdioMCPClient("math_tools", "uv", Arrays.asList(
                    "--directory", "./mcp_servers", "run", "math_tools.py"
            )),
            ReActAgent.builder()
                    .isMaster(true)
                    .name("master_agent")
                    .desc("A tool for mathematical calculations")
                    .tools(Arrays.asList("math_tools"))
                    .build()
        );
    }

    public static void main(String[] args) throws Exception {
        // 启动在 8081 端口
        ServerApp.main(new String[]{"-p", "8081"});
    }
}
```

类似地，创建一个时间查询服务 `AppTimeAgent`，监听 8092 端口：

```java
public class AppTimeAgent {

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
            new StdioMCPClient("time_tools", "uvx", Arrays.asList(
                    "mcp-server-time", "--local-timezone=Asia/Shanghai"
            )),
            ReActAgent.builder()
                    .isMaster(true)
                    .name("time_agent")
                    .desc("A tool for time query")
                    .tools(Arrays.asList("time_tools"))
                    .build()
        );
    }

    public static void main(String[] args) throws Exception {
        // 启动在 8092 端口
        ServerApp.main(new String[]{"-p", "8092"});
    }
}
```

## 使用 SSEAgent 连接远端服务

在主服务 `AppMasterAgent` 中，使用 `SSEAgent` 替代本地的子智能体：

```java
import com.jd.oxygent.core.oxygent.oxy.agents.SSEAgent;

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
            // 使用 SSEAgent 连接远端数学计算服务
            SSEAgent.builder()
                    .name("math_agent")
                    .desc("A tool for mathematical calculations")
                    .serverUrl("http://127.0.0.1:8081")
                    .isOxyAgent(true)
                    .isShareCallStack(false)
                    .build(),
            // 使用 SSEAgent 连接远端时间查询服务
            SSEAgent.builder()
                    .name("time_agent")
                    .desc("A tool for time query")
                    .serverUrl("http://127.0.0.1:8092")
                    .isOxyAgent(true)
                    .build(),
            ReActAgent.builder()
                    .isMaster(true)
                    .name("master_agent")
                    .subAgents(Arrays.asList("math_agent", "time_agent"))
                    .build()
        );
    }

    public static void main(String[] args) throws Exception {
        // 主服务启动在 8090 端口
        ServerApp.main(new String[]{"-p", "8090"});
    }
}
```

## SSEAgent 配置参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | String | 必填 | 智能体名称 |
| `desc` | String | `""` | 智能体描述 |
| `serverUrl` | String | 必填 | 远程 OxyGent 服务的 URL 地址 |
| `isOxyAgent` | boolean | `false` | 是否为 OxyGent 协议的远端服务 |
| `isShareCallStack` | boolean | `true` | 是否共享调用栈信息 |
| `customHeaders` | Map\<String, String\> | `{}` | 自定义 HTTP 请求头 |
| `maxRetries` | int | `1` | 最大重试次数 |
| `retryDelayMs` | long | `1000` | 重试间隔（毫秒） |

## 启动分布式服务

您需要按顺序启动各个服务：

```bash
# 终端1：启动数学计算服务（端口8081）
java -jar oxygent-app.jar -p 8081 --app=AppMathAgent

# 终端2：启动时间查询服务（端口8092）
java -jar oxygent-app.jar -p 8092 --app=AppTimeAgent

# 终端3：启动主服务（端口8090）
java -jar oxygent-app.jar -p 8090 --app=AppMasterAgent
```

## 调用栈共享

`isShareCallStack` 参数控制是否将调用栈信息传递给远端服务：

- **true**（默认）：传递完整调用链信息，支持分布式追踪
- **false**：设置 caller 为 "user"，简化调用关系

当需要在分布式环境中追踪完整的请求链路时，建议设为 `true`。

## 自定义请求头

可以通过 `customHeaders` 传递认证信息或其他元数据：

```java
SSEAgent.builder()
    .name("secure_agent")
    .desc("Agent with authentication")
    .serverUrl("http://remote-server:8081")
    .isOxyAgent(true)
    .customHeaders(Map.of(
        "Authorization", "Bearer your-token",
        "app_id", "my-app"
    ))
    .build()
```

[上一章：多智能体系统](./multi-agent-system.md)
[回到首页](../readme.md)
