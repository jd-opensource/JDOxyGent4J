package com.jd.oxygent.core.oxygent.samples.examples.agent;

/**
 * Heterogeneous Agents Demo Class
 * Demonstrates how to configure and coordinate different types of agents working together,
 * including master-slave agent relationships and multi-agent collaboration patterns.
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.WorkflowAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DemoHeterogeneousAgents {

    @OxySpaceBean(value = "heterogeneousAgentsJavaOxySpace", defaultStart = true, query = "What time it is?")
    public static List<BaseOxy> getDefaultOxySpace() {

        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .build(),
//                new StdioMCPClient("time_tools", "uvx", Arrays.asList("mcp-server-time", "--local-timezone=Asia/Shanghai")),
                new StdioMCPClient("time", "uvx", Arrays.asList("mcp-server-time", "--local-timezone=Asia/Shanghai")),
                ReActAgent.builder()
                        .isMaster(true) // Set as master agent
                        .name("master_agent")
                        .subAgents(Arrays.asList("QA_agent", "time_agent")) // Sub-agent list
                        .build(),
                ChatAgent.builder()
                        .name("QA_agent")
                        .desc("A tool for knowledge.")
                        .build(),
                WorkflowAgent.builder()
                        .name("time_agent")
                        .desc("A tool that can query the time")
//                        .additionalPrompt("Do not send other information except time.")
                        .tools(Arrays.asList("time"))
                        .funcWorkflow(x -> {

                            Map<String, Object> arguments = Map.of("timezone", "Asia/Shanghai");

                            Map<String, Object> callRequest = new HashMap<>();
                            callRequest.put("callee", "get_current_time");
                            callRequest.put("arguments", arguments);

                            return x.call(callRequest).getOutput();
                        })
                        .build()
        );
    }

    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);
    }
}
