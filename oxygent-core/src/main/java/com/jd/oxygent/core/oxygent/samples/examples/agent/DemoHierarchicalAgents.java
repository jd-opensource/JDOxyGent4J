package com.jd.oxygent.core.oxygent.samples.examples.agent;

/**
 * Hierarchical Agents Demo Class
 * Demonstrates hierarchical agent architecture with master-slave relationships,
 * showcasing how agents can be organized in a tree structure for complex task coordination.
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */

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

public class DemoHierarchicalAgents {

    @OxySpaceBean(value = "hierarchicalAgentsJavaOxySpace", defaultStart = true, query = "Get what time it is and save in `log.txt` under `/local_file`")
    public static List<BaseOxy> getDefaultOxySpace() {

        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .build(),
                new StdioMCPClient("time", "uvx", Arrays.asList("mcp-server-time", "--local-timezone=Asia/Shanghai")),
                ReActAgent.builder()
                        .name("time_agent")
                        .desc("A tool that can query the time")
                        .additionalPrompt("Do not send other information except time.")
                        .tools(Arrays.asList("time")) // Tool name list
                        .trustMode(false)
                        .build(),
                // Mac or Linux syntax
                new StdioMCPClient("file_tools", "npx", Arrays.asList("-y", "@modelcontextprotocol/server-filesystem", "./local_file")),
                // Windows syntax
//                new StdioMCPClient("file_tools", "cmd", Arrays.asList("/c", "npx", "-y", "@modelcontextprotocol/server-filesystem", "local_file")),
                ReActAgent.builder()
                        .name("file_tools_agent")
                        .desc("A tool for file operation.")
                        .additionalPrompt("A tool for file operation.")
                        .tools(Arrays.asList("file_tools")) // Tool name list
                        .trustMode(false)
                        .build(),
                ReActAgent.builder()
                        .isMaster(true) // Set as master agent
                        .name("master_agent")
                        .subAgents(Arrays.asList("time_agent", "file_tools_agent")) // Sub-agent list
                        .build()
        );
    }

    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);
    }
}