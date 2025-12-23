package com.jd.oxygent.core.oxygent.samples.examples.agent;

/**
 * Single Agent Demo Class
 * Demonstrates how to configure and use a single chat agent with input/output processing functions
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */

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
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);
    }
}
