package com.jd.oxygent.core.oxygent.samples.examples.agent;

/**
 * Workflow Agent Demo Class
 * Demonstrates how to configure and use workflow agents for complex task orchestration and execution
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */

import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.WorkflowAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.schemas.memory.Memory;
import com.jd.oxygent.core.oxygent.schemas.memory.Message;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.tools.PresetTools;
import com.jd.oxygent.core.oxygent.utils.CommonUtils;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
public class DemoWorkflowAgent {

    @OxySpaceBean(value = "workflowJavaOxySpace", defaultStart = true, query = "Please calculate the 20 positions of Pi")
    public static List<BaseOxy> getDefaultOxySpace() {

        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .llmParams(Map.of("temperature", 0.01))
                        .semaphoreCount(4)
                        .timeout(300)
                        .retries(3)
                        .build(),
                // Math tools
                PresetTools.MATH_TOOLS,
                ChatAgent.builder()
                        .name("chat_agent")
                        .llmModel("default_llm")
                        .build(),
                WorkflowAgent.builder()
                        .name("math_agent")
                        .isMaster(true)
                        .subAgents(Arrays.asList("chat_agent"))
                        .tools(Arrays.asList("math_tools"))
                        .funcWorkflow(x -> {
                            // Get memory
                            List<Map<String, Object>> currentShortMemory = x.getShortMemory(false);
                            log.info("--- Current Short Memory --- :{}", currentShortMemory);
                            List<Map<String, Object>> masterShortMemory = x.getShortMemory(true);
                            log.info("--- Master Short Memory --- :{}", masterShortMemory);

                            // Get query
                            String currentQuery = x.getQuery();
                            log.info("--- Current Query --- :{}", currentQuery);
                            String masterQuery = x.getQuery(true);
                            log.info("--- Master Query --- :{}", masterQuery);

                            // Send message
                            x.sendMessage(Map.of("type", "msg_type", "content", "msg_content"));

                            // Call Agent
                            OxyResponse callChatAgent = x.call(new HashMap<>(Map.of("callee", "chat_agent", "arguments", new HashMap<>(Map.of("query", currentQuery)), "request_id", CommonUtils.generateShortUUID())));
                            log.info("--- Direct answer ---:{}", callChatAgent.getOutput());

                            // Call LLM
                            String question = "The user's question is " + currentQuery + ", how many digits after the decimal point for pi does the user want? Answer with numbers only";

                            Memory memory = new Memory();
                            memory.setMessages(Arrays.asList(
                                    Message.systemMessage("You are a helpful assistant."),
                                    Message.userMessage(question)
                            ));

                            Map<String, Object> arguments = new HashMap<>(
                                    Map.of(
                                            "messages", memory,
                                            "llm_params", Map.of("temperature", 0.2)
                                    ));

                            Map<String, Object> callRequest = new HashMap<>();
                            callRequest.put("callee", "default_llm");
                            callRequest.put("arguments", arguments);

                            OxyResponse call = x.call(callRequest);
                            log.info("--- Precision ---:{}", call.getOutput());

                            Object n = call.getOutput();
                            // Call Tool
                            OxyResponse prec = x.call(Map.of("callee", "calc_pi", "arguments", new HashMap<>(Map.of("precision", n.toString()))));
                            return "Save " + n + " positions: " + prec.getOutput();

                        })
                        .llmModel("default_llm")
                        .build()
        );
    }

    public static void main(String[] args) throws Exception {
        Map<String, Object> headers = new HashMap<>();
        headers.put("headers", Map.of("ukInfo2", "jtest2", "ukInfo3", "jtest3"));
        headers.put("_headers", Map.of("ukInfo4", "jtest4", "ukInfo5", "jtest5"));

        Mas mas = new Mas("app", getDefaultOxySpace());
        mas.init();

        Map<String, Object> arguments = new HashMap<>(Map.of("query", "Please calculate the 20 positions of Pi", "request_id", CommonUtils.generateShortUUID()));
        mas.chatWithAgent(arguments);

//        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
//        OpenOxySpringBootApplication.main(args);
    }
}
