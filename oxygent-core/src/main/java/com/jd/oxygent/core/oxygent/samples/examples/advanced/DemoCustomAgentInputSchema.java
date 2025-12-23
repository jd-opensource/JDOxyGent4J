package com.jd.oxygent.core.oxygent.samples.examples.advanced;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.WorkflowAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Demo for Custom Agent Input Schema functionality
 * <p>
 * This demo demonstrates how to define custom input schemas for agents,
 * allowing for structured parameter validation and type checking.
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DemoCustomAgentInputSchema {

    @OxySpaceBean(value = "demoCustomAgentInputSchema", defaultStart = true, query = "Please calculate the 20 positions of Pi")
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .build(),
                ReActAgent.builder()
                        .name("master_agent")
                        .subAgents(Arrays.asList("math_agent"))
                        .isMaster(true)
                        .llmModel("default_llm")
                        .build(),
                WorkflowAgent.builder()
                        .name("math_agent")
                        .desc("A tool for pi query")
                        .inputSchema(Map.of("required", List.of("query", "precision"),
                                "properties", Map.of("query", Map.of("description", "Query question"),
                                        "precision", Map.of("description", "How many decimal places are there"))))
                        .funcWorkflow((oxyRequest) -> {
                            String pi = "3.141592653589793238462643383279502884197169399375105820974944592307816406286208998";
                            try {
                                int precision = Integer.parseInt(oxyRequest.getArguments().getOrDefault("precision", "").toString());
                                if (precision <= 0) {
                                    return pi; // If precision <= 0, return complete PI
                                }
                                if (precision >= pi.length()) {
                                    return pi; // If precision exceeds PI length, return complete PI
                                }
                                return pi.substring(0, precision);
                            } catch (NumberFormatException e) {
                                return pi; // If parameter is not a number, return complete PI
                            }
                        })
                        .build());
    }

    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);
    }
}