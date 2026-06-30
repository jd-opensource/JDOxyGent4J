# ReAct Agent 示例

演示如何配置带有反思（Reflexion）能力的 ReAct Agent。当 Agent 输出不符合预期格式时，会自动重试直到结果正确。

## 前置条件

- JDK 17+
- 设置环境变量：`OXY_LLM_API_KEY`、`OXY_LLM_BASE_URL`、`OXY_LLM_MODEL_NAME`

## 完整代码

```java
package com.jd.oxygent.core.oxygent.samples.examples.agent;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

public class DemoReactAgent {

    // Reflexion 函数：验证返回结果是否为数字格式
    public static final BiFunction<String, OxyRequest, String> MASTER_REFLEXION = (response, oxyRequest) -> {
        var numberPattern = "^[-+]?(\\d+(\\.\\d*)?|\\.\\d+)$";
        var pattern = Pattern.compile(numberPattern);
        if (!pattern.matcher(response.trim()).matches()) {
            return "Only answer with numbers";
        }
        return null; // null 表示校验通过
    };

    @OxySpaceBean(value = "reactAgentJavaOxySpace", defaultStart = true, query = "What is 1+1")
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
                ReActAgent.builder()
                        .name("master_agent")
                        .llmModel("default_llm")
                        .funcReflexion(MASTER_REFLEXION)
                        .additionalPrompt("Please provide the optimal answer based on my question")
                        .maxReactRounds(2)
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
java -cp <classpath> com.jd.oxygent.core.oxygent.samples.examples.agent.DemoReactAgent
```

## 预期输出

发送 `What is 1+1`，Agent 如果首次返回非数字（如 "The answer is 2"），反思函数会提示 "Only answer with numbers" 并要求重试，最终输出纯数字结果：

```
2
```
