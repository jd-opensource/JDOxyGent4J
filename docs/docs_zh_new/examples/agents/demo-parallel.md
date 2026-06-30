# Parallel Agent 示例

演示如何使用 ParallelAgent 让多个专家 Agent 并行执行，对同一项目进行多维度评估（技术、商业、风险、法律）。

## 前置条件

- JDK 17+
- 设置环境变量：`GPT_LLM_API_KEY`、`GPT_LLM_BASE_URL`、`GPT_LLM_MODEL_NAME`

## 完整代码

```java
package com.jd.oxygent.core.oxygent.samples.examples.agent;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.ParallelAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ParallelDemo {

    @OxySpaceBean(value = "parallelAgentJavaOxySpace", defaultStart = true, query = "...")
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("GPT_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("GPT_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("GPT_LLM_MODEL_NAME"))
                        .llmParams(Map.of("max_tokens", 8192, "temperature", 0.7))
                        .build(),
                ChatAgent.builder()
                        .name("tech_expert")
                        .llmModel("default_llm")
                        .desc("AI product technical feasibility expert")
                        .prompt("You are a senior technical architect...")
                        .build(),
                ChatAgent.builder()
                        .name("business_expert")
                        .llmModel("default_llm")
                        .desc("AI product business value evaluation expert")
                        .prompt("You are an experienced business analyst...")
                        .build(),
                ChatAgent.builder()
                        .name("risk_expert")
                        .llmModel("default_llm")
                        .desc("AI project risk management expert")
                        .prompt("You are a professional risk management expert...")
                        .build(),
                ChatAgent.builder()
                        .name("legal_expert")
                        .llmModel("default_llm")
                        .desc("AI product legal compliance and IP expert")
                        .prompt("You are a professional legal expert...")
                        .build(),
                ParallelAgent.builder()
                        .isMaster(true)
                        .name("expert_panel_agent")
                        .llmModel("default_llm")
                        .desc("Expert panel parallel evaluation")
                        .permittedToolNameList(Arrays.asList(
                                "tech_expert", "business_expert",
                                "risk_expert", "legal_expert"))
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
java -cp <classpath> com.jd.oxygent.core.oxygent.samples.examples.agent.ParallelDemo
```

## 预期输出

四个专家 Agent 并行执行，分别从技术可行性、商业价值、风险管理、法律合规四个维度输出评估报告，最终由 ParallelAgent 汇总：

```
[tech_expert] 技术可行性评分: 8/10 ...
[business_expert] 商业价值评分: 7/10 ...
[risk_expert] 总体风险等级: 中等 ...
[legal_expert] 合规建议清单: ...
```
