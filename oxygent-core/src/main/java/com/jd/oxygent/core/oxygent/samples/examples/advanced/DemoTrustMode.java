package com.jd.oxygent.core.oxygent.samples.examples.advanced;

/**
 * Demo showcasing the difference between trust mode and normal mode in agent execution.
 * <p>
 * This example demonstrates how agents behave differently when trust mode is enabled vs disabled:
 * - Normal mode: Agent follows standard security protocols and validation
 * - Trust mode: Agent operates with elevated trust levels for tool execution
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @date 2025/11/7
 */

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.MasFactoryRegistry;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DemoTrustMode {

    @OxySpaceBean(value = "demoTrustMode", defaultStart = true, query = "What is the current time")
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .build(),

                new StdioMCPClient("time_tools", "uvx", List.of("mcp-server-time", "--local-timezone=Asia/Shanghai")),

                ReActAgent.builder()
                        .name("normal_agent")
                        .tools(List.of("time_tools"))
                        .llmModel("default_llm")
                        .trustMode(false)
                        .build(),

                ReActAgent.builder()
                        .name("trust_agent")
                        .tools(List.of("time_tools"))
                        .llmModel("default_llm")
                        .trustMode(true)
                        .build())
                ;
    }

    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        Mas mas = MasFactoryRegistry.getFactory().createMas();

        Object normalResult = mas.call("normal_agent", new HashMap<>(Map.of("query", "What is the current time")));
        Object trustResult = mas.call("trust_agent", new HashMap<>(Map.of("query", "What is the current time")));

        System.out.printf("normal mode output: %s%n", normalResult);
        System.out.printf("trust mode output: %s%n", trustResult);
    }
}