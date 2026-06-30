# 如何进行设置？

在 OxyGent4J 中，您可以使用 `Config` 类来管理全局配置。Config 是 OxyGent4J 的集中式配置管理系统，通过 Config 可以设置全局默认值（如默认 LLM 参数、服务器端口等），避免在每个组件中重复传递参数。

Config 支持三层配置机制（后者覆盖前者）：
1. **Java 默认值**：代码中的默认值
2. **config.json 文件**：通过 JSON 文件配置
3. **application.yml**：Spring Boot 配置文件

---

## 1. config.json 配置文件

OxyGent4J 会自动加载 classpath 下的 `config.json` 文件。JSON 文件示例：

```json
{
  "default": {
    "app": {
      "name": "my_app",
      "version": "1.0.0",
      "biz_type": "oxygent",
      "scan_oxygent_path": "com.jd"
    },
    "llm": {
      "temperature": 0.2,
      "max_tokens": 4096,
      "top_p": 1.0,
      "semaphore": 16,
      "timeout": 300
    },
    "server": {
      "host": "127.0.0.1",
      "port": 8080,
      "auto_open_webpage": true
    },
    "agent": {
      "llm_model": "default_llm",
      "short_memory_size": 10,
      "welcome_message": "Hi, I'm OxyGent. How can I assist you?"
    },
    "message": {
      "is_send_tool_call": true,
      "is_send_observation": true,
      "is_send_think": true,
      "is_send_answer": true,
      "is_stored": true
    },
    "es": {
      "type": "local",
      "hosts": [],
      "user": "",
      "password": ""
    },
    "redis": {
      "host": "local",
      "port": 6379,
      "database": 0
    },
    "xfile": {
      "save_dir": "./cache_dir"
    }
  }
}
```

通过 `CONFIG_FILE_ENV` 环境变量可以切换配置环境（如 `default`、`dev`、`test`、`prod`）。

---

## 2. 通过环境变量指定配置路径

```bash
export CONFIG_FILE_PATH="/path/to/my-config.json"
export CONFIG_FILE_ENV="prod"
```

或在代码中动态加载：

```java
Config.loadConfigPath("/path/to/my-config.json", "prod");
```

---

## 3. application.yml 配置（Spring Boot）

在 Spring Boot 项目中，可以通过 `application.yml` 覆盖 Config 值：

```yaml
oxygent:
  app:
    name: my_app
    version: 1.0.0
  llm:
    temperature: 0.2
    max-tokens: 4096
    top-p: 1.0
    semaphore: 16
    timeout: 300
  server:
    host: 127.0.0.1
    port: 8080
  agent:
    llm-model: default_llm
    short-memory-size: 10
  message:
    is-send-tool-call: true
    is-send-observation: true
    is-send-think: true
    is-send-answer: true
  es:
    type: local
  redis:
    host: local
    port: 6379
```

---

## 4. Config 类结构

Config 类通过静态内部类组织各模块配置：

| 配置模块 | 类名 | 说明 |
|----------|------|------|
| 应用配置 | `Config.AppConfig` | 应用名称、版本、扫描路径 |
| LLM 配置 | `Config.LlmConfig` | temperature、maxTokens、timeout |
| 服务器配置 | `Config.ServerConfig` | host、port、workers |
| Agent 配置 | `Config.AgentConfig` | 默认 LLM、记忆大小、欢迎语 |
| 消息配置 | `Config.MessageConfig` | 是否发送工具调用、观察、思考 |
| ES 配置 | `Config.EsConfig` | Elasticsearch 连接配置 |
| Redis 配置 | `Config.RedisConfig` | Redis 连接配置 |
| 文件配置 | `Config.FileConfig` | 文件存储目录 |
| 向量库配置 | `Config.VearchConfig` | Vearch 向量数据库配置 |
| Oxy 配置 | `Config.OxyConfig` | 并发信号量、超时、重试 |
| 工具配置 | `Config.ToolConfig` | MCP keep-alive、并发初始化 |

---

## 5. 在代码中读取配置

```java
import com.jd.oxygent.core.Config;

// 获取应用名称
String appName = Config.getAppName();

// 获取 LLM 配置
double temperature = Config.getLlm().getTemperature();
int maxTokens = Config.getLlm().getMaxTokens();

// 获取服务器端口
int port = Config.getServer().getPort();

// 获取 Agent 默认 LLM
String defaultLlm = Config.getAgent().getLlmModel();

// 获取 LLM 配置 Map（用于传递给 LLM 组件）
Map llmConfigMap = Config.getLlmConfigMap();
```

---

## 6. 设置 LLM 模型参数

在 `HttpLlm.builder()` 中可以通过 `llmParams` 覆盖全局 LLM 配置：

```java
HttpLlm.builder()
        .name("default_llm")
        .apiKey(apiKey)
        .baseUrl(baseUrl)
        .modelName(modelName)
        .llmParams(Map.of(
                "temperature", 0.01,
                "max_tokens", 2048
        ))
        .timeout(240)
        .build()
```

---

## 7. 设置消息输出格式

通过 config.json 中的 `message` 节点控制结果输出：

```json
{
  "default": {
    "message": {
      "is_send_tool_call": false,
      "is_send_observation": false,
      "is_send_think": false,
      "is_send_answer": true,
      "is_stored": true,
      "is_show_in_terminal": false
    }
  }
}
```

---

## 完整的可运行样例

以下是使用 Config 配置的完整代码示例：

```java
import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.tools.PresetTools;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ConfigDemo {
    public static void main(String[] args) throws Exception {
        // Config 会自动加载 classpath 下的 config.json
        // 也可以手动指定路径
        // Config.loadConfigPath("./my-config.json", "default");

        List<BaseOxy> oxySpace = Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .llmParams(Map.of("temperature", 0.01))
                        .timeout(240)
                        .build(),
                PresetTools.FILE_TOOLS,
                ReActAgent.builder()
                        .name("master_agent")
                        .isMaster(true)
                        .llmModel("default_llm")
                        .tools(Arrays.asList("file_tools"))
                        .build()
        );

        Mas mas = new Mas("app", oxySpace);
        mas.init();
        mas.startCliMode("Hello!");
    }
}
```

---

[上一章：架构总览](./architecture.md)
[回到首页](../readme.md)

---

## 相关示例

- [Config 设置示例](../../examples/backend/demo_config.md) -- Config 的详细使用方法
- [LLM 参数设置示例](../../examples/llms/demo_llm_params.md) -- 动态设置 LLM 参数
