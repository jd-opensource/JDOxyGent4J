/**
 * Main master agent for OxyGent distributed samples.
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
package com.jd.oxygent.core.oxygent.samples.examples.distributed;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.SSEAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AppMasterAgent {

    @OxySpaceBean(value = "appMasterAgentJavaOxySpace", defaultStart = true, query = "The first 30 positions of pi")
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

                // mac or linux usage
                new StdioMCPClient("file_tools", "npx", Arrays.asList("-y", "@modelcontextprotocol/server-filesystem", "./local_file")),
                //windows 写法
//                new StdioMCPClient("file_tools", "cmd", Arrays.asList("/c", "npx", "-y", "@modelcontextprotocol/server-filesystem", "local_file")),

                ReActAgent.builder()
                        .name("file_agent")
                        .desc("A tool for querying local files")
                        .tools(Arrays.asList("file_tools")) // List of tool names
                        .build(),
                ReActAgent.builder()
                        .isMaster(true) // Set as master agent
                        .name("master_agent")
                        .subAgents(Arrays.asList("file_agent", "math_agent")) // List of sub agents
                        .build(),
                SSEAgent.builder()
                        .name("math_agent")
                        .desc("A tool for mathematical calculations")
                        .serverUrl("http://127.0.0.1:8081")
                        .isOxyAgent(true)
                        .isShareCallStack(false).build()
        );
    }


    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(new String[]{"-p", "8090"});
    }

}
