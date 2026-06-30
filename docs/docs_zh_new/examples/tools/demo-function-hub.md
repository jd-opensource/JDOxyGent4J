# Function Hub 示例

演示如何创建自定义函数工具并注册到 FunctionHub，让 Agent 可以调用自定义的 Java 函数。本例实现了一个笑话生成工具。

## 前置条件

- JDK 17+
- 设置环境变量：`OXY_LLM_API_KEY`、`OXY_LLM_BASE_URL`、`OXY_LLM_MODEL_NAME`

## 完整代码

```java
package com.jd.oxygent.core.oxygent.samples.examples.tools;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

public class DemoFunctionHub {

    private static final Random RANDOM = new Random();
    private static final FunctionHub JOKE_TOOLS = new FunctionHub("joke_tools");

    private static void init() {
        JOKE_TOOLS.setDesc("Tool collection for telling jokes");
        JOKE_TOOLS.registerTool(
                "joke_tool",
                "A tool that can generate various types of jokes",
                (args) -> {
                    var jokeType = "any";
                    return jokeTool(jokeType);
                },
                Arrays.asList(
                        new FunctionHub.ParamMeta("joke_type", "String", "Type of the joke", "any")
                )
        );
    }

    public static String jokeTool(String jokeType) {
        var jokes = List.of(
                "Why don't scientists trust atoms? Because they make up everything!",
                "Why did the scarecrow win an award? Because he was outstanding in his field!",
                "Why don't eggs tell jokes? They'd crack each other up!"
        );
        return jokes.get(RANDOM.nextInt(jokes.size()));
    }

    @OxySpaceBean(value = "functionhubJavaOxySpace", defaultStart = true, query = "Please tell a joke")
    public static List<BaseOxy> getDefaultOxySpace() {
        init();
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
                JOKE_TOOLS,
                ReActAgent.builder()
                        .name("joke_agent")
                        .llmModel("default_llm")
                        .additionalPrompt("You are a humorous assistant. When users need jokes, please use joke_tool to get jokes.")
                        .tools(Arrays.asList("joke_tool"))
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
java -cp <classpath> com.jd.oxygent.core.oxygent.samples.examples.tools.DemoFunctionHub
```

## 预期输出

发送 `Please tell a joke`，Agent 调用 joke_tool 获取随机笑话并返回：

```
Why don't scientists trust atoms? Because they make up everything!
```
