package com.jd.oxygent.core.oxygent.samples.examples.banks;

import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.bank_tools.BankClient;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.MasFactoryRegistry;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import com.jd.oxygent.core.oxygent.utils.OSUtil;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * Bank ReActAgent autonomy example
 */
@Slf4j
public class DemoBankReactAgentAutonomy {

    static {
        Config.getServer().setPort(8091);
    }

    @OxySpaceBean(value = "demoBankReactAgentAutonomy", defaultStart = true, query = "Who I am")
    public static List<BaseOxy> getDemoBankReactAgentOxySpace() {
        return Arrays.asList(
                // Default LLM
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .llmParams(new HashMap<String, Object>() {{
                            put("temperature", 0.01);
                        }})
                        .semaphore(new Semaphore(4))
                        .build(),
                // Time tool MCP client
                OSUtil.isWindows() ?
                        new StdioMCPClient("time_tools", "cmd.exe", Arrays.asList("/c", "uvx", "mcp-server-time", "--local-timezone=Asia/Shanghai"))
                        :
                        new StdioMCPClient("time_tools", "uvx", Arrays.asList("mcp-server-time", "--local-timezone=Asia/Shanghai")),
                // ReActAgent
                ReActAgent.builder()
                        .name("qa_agent")
                        .llmModel("default_llm")
                        .tools(Arrays.asList("time_tools"))
                        .banks(Arrays.asList("remote_user_profile_banks"))
                        .build(),
                // Remote user profile bank client
                BankClient.builder()
                        .name("remote_user_profile_banks")
                        .serverUrl("http://127.0.0.1:8090/api")
                        .build()
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