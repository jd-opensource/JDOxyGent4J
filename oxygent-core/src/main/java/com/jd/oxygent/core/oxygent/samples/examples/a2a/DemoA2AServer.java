package com.jd.oxygent.core.oxygent.samples.examples.a2a;

import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A2A Server Demo - Exposes OxyGent MAS as an A2A-compatible server.
 *
 * <p>Run this demo first, then use the client demos to connect to it.</p>
 *
 * <pre>
 * Prerequisites:
 *   - Set environment variables: OXY_LLM_API_KEY, OXY_LLM_BASE_URL, OXY_LLM_MODEL_NAME
 *
 * Usage:
 *   java -cp ... com.jd.oxygent.core.oxygent.samples.examples.a2a.DemoA2AServer
 * </pre>
 */
@Slf4j
public class DemoA2AServer {

    private static final int PORT = 8090;
    private static final String A2A_BASE_PATH = "/a2a";

    @OxySpaceBean(value = "demoA2AServerOxySpace", defaultStart = true, query = "A2A MAS server is running.")
    public static List<BaseOxy> getDefaultOxySpace() {
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
                        .build(),
                ChatAgent.builder()
                        .name("master_agent")
                        .isMaster(true)
                        .desc("Local chat agent as MAS target agent for A2A")
                        .llmModel("default_llm")
                        .build()
        );
    }

    /**
     * Start OxyGent MAS server with A2A endpoints enabled.
     *
     * <p>The A2A endpoints will be available at:</p>
     * <ul>
     *   <li>GET  http://localhost:8090/a2a/.well-known/agent.json - Agent card discovery</li>
     *   <li>POST http://localhost:8090/a2a - Unified JSON-RPC endpoint</li>
     *   <li>POST http://localhost:8090/a2a/messages/send - Send message</li>
     *   <li>POST http://localhost:8090/a2a/tasks/get - Get task status</li>
     *   <li>POST http://localhost:8090/a2a/tasks/cancel - Cancel task</li>
     * </ul>
     */
    public static void main(String[] args) throws Exception {
        Config.getServer().setPort(PORT);
        Config.getServer().setEnableA2aServer(true);
        Config.getServer().setA2aBasePath(A2A_BASE_PATH);

        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(
                Thread.currentThread().getStackTrace()[1].getClassName());

        log.info("Starting A2A server on port {} with base path {}", PORT, A2A_BASE_PATH);
        ServerApp.main(new String[]{"-p", String.valueOf(PORT)});
    }
}
