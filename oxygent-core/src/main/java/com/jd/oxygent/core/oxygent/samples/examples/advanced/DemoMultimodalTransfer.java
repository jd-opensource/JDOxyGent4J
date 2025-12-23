package com.jd.oxygent.core.oxygent.samples.examples.advanced;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
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
 * Demo for Multimodal Transfer functionality
 * <p>
 * This demo demonstrates multimodal transfer capabilities, combining image understanding
 * with image generation to create a complete multimodal workflow.
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DemoMultimodalTransfer {

    @OxySpaceBean(value = "demoMultimodalTransfer", defaultStart = true, query = "What is this, generate a cartoon version")
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_vlm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .isMultimodalSupported(true)
                        .isConvertUrlToBase64(true)
                        .build(),
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .isMultimodalSupported(true)
                        .build(),
                PresetTools.IMAGE_GEN_TOOLS,
                ChatAgent.builder()
                        .name("vision_agent")
                        .desc("An image understanding tool")
                        .llmModel("default_vlm")
                        .build(),
                ReActAgent.builder()
                        .name("master_agent")
                        .isMaster(true)
                        .subAgents(Arrays.asList("vision_agent"))
                        .tools(Arrays.asList("image_gen_tools"))
                        .llmModel("default_llm")
                        .build()
        );
    }

    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);
    }
}
