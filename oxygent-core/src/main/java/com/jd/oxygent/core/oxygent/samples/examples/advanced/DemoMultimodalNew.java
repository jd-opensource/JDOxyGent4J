package com.jd.oxygent.core.oxygent.samples.examples.advanced;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;

import java.util.*;

/**
 * Demo for New Multimodal functionality
 * <p>
 * This demo demonstrates a simplified approach to multimodal processing
 * with vision-language model capabilities for image analysis.
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DemoMultimodalNew {

    @OxySpaceBean(value = "demoMultimodalNew", defaultStart = true, query = "What is this?")
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
                ChatAgent.builder()
                        .name("vision_agent")
                        .llmModel("default_vlm")
                        .build());
    }

    // Test web version
    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);
    }
    // Test command version
//    public static void main(String[] args) throws Exception {
//        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
//        Mas mas= MasFactoryRegistry.getFactory().createMas();
//        Memory  messages = new Memory();
//                messages.addMessage(Message.systemMessage("You are a helpful assistant."));
//                messages.addMessage(Message.userMessage(List.of(
//                                                    Map.of("type","image_url","image_url",Map.of("url","d:/test_pic.png")),
//                                                    Map.of("type","text","text","What is this?")
//                                            )));
//        mas.call("default_vlm",Map.of("messages",messages));
//    }
}