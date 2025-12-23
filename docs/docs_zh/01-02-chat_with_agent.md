# 如何和智能体交流？

OxyGent支持多种不同与智能体交流的方式。

## 1.可视化界面

假设您搭建了智能体系统，最简单的方式是启动[官方可视化工具](./13-01-debugging.md)，您可以像主流ai产品客户端一样使用聊天框和agent进行对话。

**参考现有示例**: [DemoSingleAgent.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoSingleAgent.java) 或 [DemoInReadme.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/DemoInReadme.java)

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

public class WebServiceDemo {

    @OxySpaceBean(value = "webServiceDemo", defaultStart = true, query = "Hello!")
    public static List<BaseOxy> getOxySpace() {
        return Arrays.asList(
            HttpLlm.builder()
                .name("default_llm")
                .apiKey(System.getenv("OXY_LLM_API_KEY"))
                .baseUrl(System.getenv("OXY_LLM_BASE_URL"))
                .modelName(System.getenv("OXY_LLM_MODEL_NAME"))
                .llmParams(Map.of("temperature", 0.01))
                .build(),
            ChatAgent.builder()
                .name("master_agent")
                .isMaster(true)
                .llmModel("default_llm")
                .build()
        );
    }

    public static void main(String[] args) throws Exception {
        var currentClassName = Thread.currentThread().getStackTrace()[1].getClassName();
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(currentClassName);
        ServerApp.main(args); // 启动web服务
    }
}
```

## 2.命令行

此外，如果你更倾向与使用命令行进行交互，您可以使用命令行模式来启动您的智能体。

```java
package com.jd.oxygent.examples.cli;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class CliDemo {

    public static List<BaseOxy> getOxySpace() {
        return Arrays.asList(
            HttpLlm.builder()
                .name("default_llm")
                .apiKey(System.getenv("OXY_LLM_API_KEY"))
                .baseUrl(System.getenv("OXY_LLM_BASE_URL"))
                .modelName(System.getenv("OXY_LLM_MODEL_NAME"))
                .build(),
            ChatAgent.builder()
                .name("master_agent")
                .isMaster(true)
                .llmModel("default_llm")
                .build()
        );
    }

    public static void main(String[] args) throws Exception {
        List<BaseOxy> oxySpace = getOxySpace();
        Mas mas = new Mas("app", oxySpace);
        mas.setOxySpace(oxySpace);
        mas.init();

        Map<String, Object> info = new HashMap<>();
        info.put("query", "Hello!");

        // 命令行交互模式
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("User: ");
            String userInput = scanner.nextLine();
            if (userInput.toLowerCase().equals("quit") || userInput.toLowerCase().equals("exit")) {
                break;
            }

            info.put("query", userInput);
            mas.chatWithAgent(info);
        }
        scanner.close();
    }
}
```

如果您只想调用与智能体交互一轮，可以使用`chatWithAgent`，并使用`info`传递对话内容：

```java
package com.jd.oxygent.examples.cli;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SingleChatDemo {

    public static List<BaseOxy> getOxySpace() {
        return Arrays.asList(
            HttpLlm.builder()
                .name("default_llm")
                .apiKey(System.getenv("OXY_LLM_API_KEY"))
                .baseUrl(System.getenv("OXY_LLM_BASE_URL"))
                .modelName(System.getenv("OXY_LLM_MODEL_NAME"))
                .build(),
            ChatAgent.builder()
                .name("master_agent")
                .isMaster(true)
                .llmModel("default_llm")
                .build()
        );
    }

    public static void main(String[] args) throws Exception {
        List<BaseOxy> oxySpace = getOxySpace();
        Mas mas = new Mas("app", oxySpace);
        mas.setOxySpace(oxySpace);
        mas.init();

        Map<String, Object> info = new HashMap<>();
        info.put("query", "The 30 positions of pi.");

        System.out.println("开始请求chatWithAgent");
        var result = mas.chatWithAgent(info);
        System.out.println("output: " + result);
    }
}
```

您还可以使用`call`方法与任意指定的agent进行交流：

```java
package com.jd.oxygent.examples.cli;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DirectCallDemo {

    public static List<BaseOxy> getOxySpace() {
        return Arrays.asList(
            HttpLlm.builder()
                .name("default_llm")
                .apiKey(System.getenv("OXY_LLM_API_KEY"))
                .baseUrl(System.getenv("OXY_LLM_BASE_URL"))
                .modelName(System.getenv("OXY_LLM_MODEL_NAME"))
                .build(),
            ChatAgent.builder()
                .name("master_agent")
                .isMaster(true)
                .llmModel("default_llm")
                .build()
        );
    }

    public static void main(String[] args) throws Exception {
        List<BaseOxy> oxySpace = getOxySpace();
        Mas mas = new Mas("app", oxySpace);
        mas.setOxySpace(oxySpace);
        mas.init();

        List<Map<String, String>> messages = Arrays.asList(
            Map.of("role", "system", "content", "You are a helpful assistant"),
            Map.of("role", "user", "content", "hello")
        );

        Map<String, Object> arguments = Map.of("messages", messages);
        var result = mas.call("master_agent", arguments);
        System.out.println(result);
    }
}
```

如果您希望对OxyGent进行开发，还可以采取其它更复杂而自定义的方式，例如直接编辑对话数据( [DemoChatAgentStream.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoChatAgentStream.java))

```java
package com.jd.oxygent.examples.advanced;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class CustomChatDemo {

    public static List<BaseOxy> getOxySpace() {
        return Arrays.asList(
            HttpLlm.builder()
                .name("default_llm")
                .apiKey(System.getenv("OXY_LLM_API_KEY"))
                .baseUrl(System.getenv("OXY_LLM_BASE_URL"))
                .modelName(System.getenv("OXY_LLM_MODEL_NAME"))
                .build(),
            ChatAgent.builder()
                .name("master_agent")
                .isMaster(true)
                .llmModel("default_llm")
                .build()
        );
    }

    public static void main(String[] args) throws Exception {
        List<BaseOxy> oxySpace = getOxySpace();
        Mas mas = new Mas("app", oxySpace);
        mas.setOxySpace(oxySpace);
        mas.init();

        List<Map<String, String>> history = new ArrayList<>();
        history.add(Map.of("role", "system", "content", "You are a helpful assistant."));

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("User: ");
            String userInput = scanner.nextLine().trim();

            if (userInput.toLowerCase().matches("exit|quit|q")) {
                break;
            }

            history.add(Map.of("role", "user", "content", userInput));

            Map<String, Object> arguments = Map.of("query", history);
            var result = mas.call("master_agent", arguments);
            String assistantOutput = result.toString();

            System.out.println("Assistant: " + assistantOutput + "\n");
            history.add(Map.of("role", "assistant", "content", assistantOutput));
        }
        scanner.close();
    }
}
```

**现有可运行示例**:
 [DemoChatAgentStream.java](../../oxygent-core/src/main/java/com/jd/oxygent/core/oxygent/samples/examples/agent/DemoChatAgentStream.java),
您可以阅读源代码以获取更多相关信息。

[上一章：创建第一个智能体](./01-01-register_single_agent.md)
[下一章：选择智能体使用的LLM](./01-03-select_llm.md)
[回到首页](./readme.md)