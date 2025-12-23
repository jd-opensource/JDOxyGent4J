package com.jd.oxygent.core.oxygent.samples.examples.advanced;

import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.WorkflowAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;

import java.util.*;

/**
 * Demo for Multimodal functionality
 * <p>
 * This demo demonstrates how to work with multimodal content (text, images, etc.)
 * using vision-language models and multimodal processing capabilities.
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DemoMultimodal {

    @OxySpaceBean(value = "demoMultimodal", defaultStart = true, query = "What is it in the picture?")
    public static List<BaseOxy> getDefaultOxySpace() {
        Config.getAgent().setLlmModel("default_vlm");
        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_vlm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .llmParams(Map.of("temperature", 0.6, "max_tokens", 2048))
                        .maxImagePixels(10000000)
                        .isMultimodalSupported(true)
                        .isConvertUrlToBase64(true)
                        .timeout(30)
                        .build(),
                ChatAgent.builder()
                        .name("generate_agent")
                        .prompt("You are a helpful assistant. Please describe the content of the image in detail.")
                        .build(),
                ChatAgent.builder()
                        .name("discriminate_agent")
                        .prompt("Please determine whether the following text is a description of the content of the image. If it is, please output 'True', otherwise output 'False'.")
                        .build(),
                WorkflowAgent.builder()
                        .isMaster(true)
                        .name("master_agent")
                        .permittedToolNameList(new ArrayList<>(List.of("generate_agent", "discriminate_agent")))
                        .funcWorkflow((oxyRequest) -> {
                            OxyResponse generateAgentOxyResponse = oxyRequest.call(
                                    Map.of("callee", "generate_agent",
                                            "arguments", new HashMap<>(Map.of("query", oxyRequest.getQuery(),
                                                    "attachments", oxyRequest.getArguments().get("attachments"),
                                                    "llm_params", Map.of("temperature", 0.6)))));
                            OxyResponse discriminateAgentOxyResponse = oxyRequest.call(
                                    Map.of("callee", "discriminate_agent",
                                            "arguments", new HashMap<>(Map.of("query", generateAgentOxyResponse.getOutput().toString(),
                                                    "attachments", oxyRequest.getArguments().get("attachments")))));
                            return String.format("generate_agent output: %s \n discriminate_agent output: %s", generateAgentOxyResponse.getOutput(), discriminateAgentOxyResponse.getOutput());
                        })
                        .build());
    }

    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);
    }
}