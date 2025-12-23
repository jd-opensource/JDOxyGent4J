package com.jd.oxygent.core.oxygent.samples.examples.advanced;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.tools.PresetTools;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;

import java.util.Arrays;
import java.util.List;

/**
 * Demo for Send Message From Tool functionality
 * <p>
 * This demo demonstrates how tools can send messages back to the system
 * and interact with agents through message passing mechanisms.
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DemoSendMessageFromTool {

    @OxySpaceBean(value = "demoSendMessageFromTool", defaultStart = true, query = "Please calculate the 20 positions of Pi")
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .build(),
                PresetTools.FH_MATH_TOOLS,
                ReActAgent.builder()
                        .name("master_agent")
                        .tools(Arrays.asList("math_tools"))
                        .llmModel("default_llm")
                        .build()
        );
    }

    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);
    }
}
