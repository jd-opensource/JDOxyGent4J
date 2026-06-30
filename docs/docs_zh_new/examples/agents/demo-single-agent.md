# Single Agent 示例

演示如何配置和使用单个 ChatAgent，包括自定义输入处理函数（`funcProcessInput`）和输出处理函数（`funcProcessOutput`）。

## 前置条件

- JDK 17+
- 设置环境变量：`OXY_LLM_API_KEY`、`OXY_LLM_BASE_URL`、`OXY_LLM_MODEL_NAME`

## 完整代码

```java
package com.jd.oxygent.core.oxygent.samples.examples.agent;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DemoSingleAgent {

    @OxySpaceBean(value = "singleAgentJavaOxySpace", defaultStart = true, query = "Hello")
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .llmParams(Map.of("temperature", 0.01))
                        .semaphoreCount(4)
                        .timeout(300)
                        .retries(3)
                        .build(),
                ChatAgent.builder()
                        .isMaster(true)
                        .name("master_agent")
                        .llmModel("default_llm")
                        .prompt("You are a helpful assistant.")
                        .funcProcessInput(x -> {
                            String query = x.getQuery();
                            x.setQuery(query + " Please answer in detail.", false);
                            return x;
                        })
                        .funcProcessOutput(x -> {
                            x.setOutput("Answer: " + x.getOutput());
                            return x;
                        })
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

## 运行方式

```bash
java -cp <classpath> com.jd.oxygent.core.oxygent.samples.examples.agent.DemoSingleAgent
```

## 预期输出

服务启动后发送 `Hello`，Agent 会将输入追加 "Please answer in detail." 后发送给 LLM，返回结果前缀添加 "Answer: "。

```
Answer: Hello! I'm a helpful assistant. How can I assist you today? ...
```
