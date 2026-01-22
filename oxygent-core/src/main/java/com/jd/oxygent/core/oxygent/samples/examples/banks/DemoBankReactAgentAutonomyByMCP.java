package com.jd.oxygent.core.oxygent.samples.examples.banks;

import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.oxy.mcp.SSEMCPClient;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.MasFactoryRegistry;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * Bank ReActAgent autonomy example (via MCP)
 */
@Slf4j
public class DemoBankReactAgentAutonomyByMCP {

    static {
        Config.getServer().setPort(8092);
    }

    @OxySpaceBean(value = "demoBankReactAgentAutonomyByMCP", defaultStart = true, query = "Who I am")
    public static List<BaseOxy> getDemoBankReactAgentOxySpace() {
        return Arrays.asList(
                // Default LLM
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("DEFAULT_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("DEFAULT_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("DEFAULT_LLM_MODEL_NAME"))
                        .llmParams(new HashMap<String, Object>() {{
                            put("temperature", 0.01);
                        }})
                        .semaphore(new Semaphore(4))
                        .build(),
                // Time tool MCP client
                new StdioMCPClient("time_tools", "uvx",Arrays.asList("mcp-server-time", "--local-timezone=Asia/Shanghai")),
                // ReActAgent
                ReActAgent.builder()
                        .name("qa_agent")
                        .llmModel("default_llm")
                        .tools(Arrays.asList("time_tools", "remote_user_profile_banks"))
                        .build(),
                // SSE MCP client as remote user profile bank
                new SSEMCPClient("remote_user_profile_banks", "http://127.0.0.1:8000/sse", "/sse", Map.of(
                        "is_dynamic_headers", true,
                        "is_inherit_headers", true,
                        "headers", Map.of(
                                "test_key", "test_value"
                        )))
        );
    }

    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        // Set filter
        MasFactoryRegistry.getFactory().createMas().setFuncFilter(payload -> {
                    payload.put("group_data", Map.of("user_pin", "002"));
                    return payload;
        });
        ServerApp.main(args);
    }
}