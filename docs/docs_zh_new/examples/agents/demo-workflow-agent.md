# Workflow Agent 示例

演示如何使用 WorkflowAgent 编排复杂任务流程，包括调用子 Agent、直接调用 LLM、调用工具等操作。

## 前置条件

- JDK 17+
- 设置环境变量：`OXY_LLM_API_KEY`、`OXY_LLM_BASE_URL`、`OXY_LLM_MODEL_NAME`

## 完整代码

```java
package com.jd.oxygent.core.oxygent.samples.examples.agent;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.WorkflowAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.schemas.memory.Memory;
import com.jd.oxygent.core.oxygent.schemas.memory.Message;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.tools.PresetTools;
import com.jd.oxygent.core.oxygent.utils.CommonUtils;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.*;

public class DemoWorkflowAgent {

    @OxySpaceBean(value = "workflowJavaOxySpace", defaultStart = true,
                  query = "Please calculate the 20 positions of Pi")
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
                PresetTools.MATH_TOOLS,
                ChatAgent.builder()
                        .name("chat_agent")
                        .llmModel("default_llm")
                        .build(),
                WorkflowAgent.builder()
                        .name("math_agent")
                        .isMaster(true)
                        .subAgents(Arrays.asList("chat_agent"))
                        .tools(Arrays.asList("math_tools"))
                        .funcWorkflow(x -> {
                            // 获取短期记忆
                            List<Map<String, Object>> currentShortMemory = x.getShortMemory(false);
                            List<Map<String, Object>> masterShortMemory = x.getShortMemory(true);

                            // 获取查询
                            String currentQuery = x.getQuery();

                            // 发送消息
                            x.sendMessage(Map.of("type", "msg_type", "content", "msg_content"));

                            // 调用子 Agent
                            OxyResponse callChatAgent = x.call(new HashMap<>(Map.of(
                                    "callee", "chat_agent",
                                    "arguments", new HashMap<>(Map.of("query", currentQuery)),
                                    "request_id", CommonUtils.generateShortUUID())));

                            // 调用 LLM 判断精度
                            String question = "The user's question is " + currentQuery +
                                    ", how many digits after the decimal point for pi does the user want?";
                            Memory memory = new Memory();
                            memory.setMessages(Arrays.asList(
                                    Message.systemMessage("You are a helpful assistant."),
                                    Message.userMessage(question)));
                            Map<String, Object> arguments = new HashMap<>(Map.of(
                                    "messages", memory,
                                    "llm_params", Map.of("temperature", 0.2)));
                            OxyResponse call = x.call(new HashMap<>(Map.of(
                                    "callee", "default_llm", "arguments", arguments)));

                            Object n = call.getOutput();
                            // 调用数学工具计算 pi
                            OxyResponse prec = x.call(Map.of(
                                    "callee", "calc_pi",
                                    "arguments", new HashMap<>(Map.of("precision", n.toString()))));
                            return "Save " + n + " positions: " + prec.getOutput();
                        })
                        .llmModel("default_llm")
                        .build()
        );
    }

    public static void main(String[] args) throws Exception {
        Mas mas = new Mas("app", getDefaultOxySpace());
        mas.init();
        Map<String, Object> arguments = new HashMap<>(Map.of(
                "query", "Please calculate the 20 positions of Pi",
                "request_id", CommonUtils.generateShortUUID()));
        mas.chatWithAgent(arguments);
    }
}
```

## 运行方式

```bash
java -cp <classpath> com.jd.oxygent.core.oxygent.samples.examples.agent.DemoWorkflowAgent
```

## 预期输出

WorkflowAgent 依次完成：调用 ChatAgent 获得直接回答 -> 调用 LLM 解析精度需求 -> 调用 calc_pi 工具计算结果：

```
Save 20 positions: 3.14159265358979323846
```
