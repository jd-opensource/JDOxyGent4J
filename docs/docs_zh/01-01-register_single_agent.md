# 如何注册一个智能体?

在OxyGent中，基础的智能体由[智能体（Agent）](./01-05-select_agent.md)和内部封装的[大语言模型（LLM）](./01-03-select_llm.md)组成。

对于新用户，您可以使用`HttpLlm.builder()`方法通过您的`apiKey`注册LLM：

```java
HttpLlm.builder()
    .name("default_llm")
    .apiKey(System.getenv("OXY_LLM_API_KEY"))
    .baseUrl(System.getenv("OXY_LLM_BASE_URL"))
    .modelName(System.getenv("OXY_LLM_MODEL_NAME"))
    .llmParams(Map.of("temperature", 0.01))
    .timeout(240) // 最大执行时间（秒）
    .build()
```
> 其中并发控制在Java版本中通过其他机制管理，详细说明请参见 [并行](./07-01-parallel.md) 部分。

接下来，您可以使用`ChatAgent`或者`ReActAgent`封装您的第一个agent：
```java
ReActAgent.builder()
    .name("master_agent")
    .additionalPrompt(masterPrompt) // 支持自定义prompt
    .isMaster(true) // 设置为master
    .llmModel("default_llm")
    .build()
```

为了使 LLM 和智能体生效，它们需要被添加到 OxySpace 中。

## 完整的可运行样例

以下是可运行的完整代码示例（源代码：[DemoSingleAgent.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoSingleAgent.java) ）：

```java
package com.jd.oxygent.core.oxygent.samples.examples.agent;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DemoSingleAgent {
    @OxySpaceBean(value = "singleAgentJavaOxySpace", defaultStart = true, query = "Hello")
    public static List<BaseOxy> getDefaultOxySpace() {

        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(System.getenv("OXY_LLM_API_KEY")) // 使用环境变量
                        .baseUrl(System.getenv("OXY_LLM_BASE_URL")) // 使用环境变量
                        .modelName(System.getenv("OXY_LLM_MODEL_NAME")) // 使用环境变量
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
                        .funcProcessInput(x->{
                            String query = x.getQuery();
                            x.setQuery(query+" Please answer in detail.",false);
                            return x;
                        })
                        .funcProcessOutput(x->{
                            x.setOutput("Answer: " + x.getOutput());
                            return x;
                        })
                        .build()
        );
    }

    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);
    }
}
```

[上一章：运行demo](./00-02-demo.md)
[下一章：和智能体交流](./01-02-chat_with_agent.md)
[回到首页](./readme.md)