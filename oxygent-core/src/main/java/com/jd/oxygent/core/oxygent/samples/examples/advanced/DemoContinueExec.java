package com.jd.oxygent.core.oxygent.samples.examples.advanced;

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.MasFactoryRegistry;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo for Continue Execution functionality
 * <p>
 * This demo demonstrates how to continue execution from a specific node in the workflow,
 * allowing for resuming interrupted processes or restarting from intermediate states.
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DemoContinueExec {

    @OxySpaceBean(value = "demoContinueExec", defaultStart = true, query = "Get what time it is Asia/Shanghai")
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
                        .name("time_agent")
                        .desc("A tool for time query.")
                        .tools(List.of("time_tools"))
                        .llmModel("default_llm")
                        .build())
                ;
    }

    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        // Case 1
//        ServerApp.main(args);

        // Case 2
        Mas mas = MasFactoryRegistry.getFactory().createMas();
        Map<String, Object> payload = new HashMap<>();
        payload.put("query", "Get what time it is Asia/Shanghai");
        payload.put("restart_node_id", "c6l6AFs9Ti6sIBvYMYVqpA");// Pass the intermediate node_id from the first call
        payload.put("restart_node_output", "{\n" +
                "\"timezone\": \"Asia/Shanghai\",\n" +
                "\"datetime\": \"2024-10-14T06:18:00+08:00\",\n" +
                "\"day_of_week\": \"Tuesday\",\n" +
                "\"is_dst\": false\n" +
                "}");
        OxyResponse normalResult = mas.chatWithAgent(payload);
        System.out.printf("LLM-second: %s", normalResult.getOutput());
    }
}