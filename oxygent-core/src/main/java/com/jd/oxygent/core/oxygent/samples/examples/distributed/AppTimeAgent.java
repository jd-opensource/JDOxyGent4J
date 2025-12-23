/**
 * Main time agent for OxyGent distributed samples.
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
package com.jd.oxygent.core.oxygent.samples.examples.distributed;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AppTimeAgent {

    @OxySpaceBean(value = "appTimeAgentJavaOxySpace", defaultStart = true, query = "The first 30 positions of pi")
    public static List<BaseOxy> getDefaultOxySpace() {
        // Use JDK17's var keyword and validate parameters
        var apiKey = EnvUtils.getEnv("OXY_LLM_API_KEY");
        var baseUrl = EnvUtils.getEnv("OXY_LLM_BASE_URL");
        var modelName = EnvUtils.getEnv("OXY_LLM_MODEL_NAME");

        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .llmParams(Map.of("temperature", 0.01))
                        .semaphoreCount(4)
                        .build(),

                new StdioMCPClient("time_tools", "uvx", Arrays.asList("mcp-server-time", "--local-timezone=Asia/Shanghai")),

                ReActAgent.builder()
                        .name("time_agent")
                        .desc("A tool for time query")
                        .tools(Arrays.asList("time_tools")) // 工具名列表
                        .build()
        );
    }


    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(new String[]{"-p", "8092"});
    }
}
