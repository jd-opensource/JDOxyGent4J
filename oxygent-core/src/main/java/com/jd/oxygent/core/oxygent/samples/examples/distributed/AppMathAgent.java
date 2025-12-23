/**
 * Main math agent for OxyGent distributed samples.
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
package com.jd.oxygent.core.oxygent.samples.examples.distributed;

import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.SSEAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.WorkflowAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.oxy.mcp.StdioMCPClient;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.utils.CommonUtils;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class AppMathAgent {

    Config config;
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d+");

    @OxySpaceBean(value = "appMathAgentJavaOxySpace", defaultStart = true, query = "The first 30 positions of pi")
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

//                mac or linux usage (mcp_servers directory points to python mcp server)
                new StdioMCPClient("math_tools", "uv", Arrays.asList("--directory", "/Users/{username}/Documents/git2024/OxyGent/mcp_servers/", "run", "math_tools.py")),
                //windows 写法  (mcp_servers目录指向python mcp server)
//                new StdioMCPClient("math_tools", "cmd", Arrays.asList("/c", "uv", "--directory", "./mcp_servers", "run", "math_tools.py")),
//                new StdioMCPClient("math_tools", "cmd", Arrays.asList("/c", "uv", "--directory", "D:\\ProjectsCode\\PythonProjects\\GitHub\\OxyGent\\mcp_servers", "run", "math_tools.py")),

                SSEAgent.builder()
                        .name("time_agent")
                        .desc("An tool for time query")
                        .serverUrl("http://127.0.0.1:8092")
                        .isOxyAgent(true)
                        .build(),
                WorkflowAgent.builder()
                        .isMaster(true) // Set as master agent
                        .name("master_agent")
                        .desc("An tool for pi query")
                        .subAgents(Arrays.asList("time_agent")) // List of sub agents
                        .tools(Arrays.asList("math_tools"))
                        .funcWorkflow(x -> {
                            // Get memory
                            List<Map<String, Object>> currentShortMemory = x.getShortMemory(false);
                            log.info("--- History record --- :{}", currentShortMemory);
                            List<Map<String, Object>> masterShortMemory = x.getShortMemory(true);
                            log.info("--- History record: User Layer --- :{}", masterShortMemory);

                            // Get query

                            String masterQuery = x.getQuery(true);
                            log.info("--- user query: --- :{}", masterQuery);

                            // Call agent
                            OxyResponse callChatAgent = x.call(new HashMap<>(Map.of("callee", "time_agent", "arguments", new HashMap<>(Map.of("query", "What time is it now?")), "request_id", CommonUtils.generateShortUUID())));
                            log.info("--- Current time ---:{}", callChatAgent.getOutput());

                            // Use regular expression to find all numbers
                            Matcher matcher = DIGIT_PATTERN.matcher(callChatAgent.getOutput().toString());

                            String n = null;
                            // Find all matches and get the last number
                            while (matcher.find()) {
                                n = matcher.group();
                            }

                            if (n != null) {
                                // Call calc_pi method
                                OxyResponse callCalcPiResponse = x.call(new HashMap<>(Map.of("callee", "calc_pi", "arguments", new HashMap<>(Map.of("prec", n)), "request_id", CommonUtils.generateShortUUID())));
                                return String.format("Save %s positions: %s", n, callCalcPiResponse.getOutput());
                            } else {
                                return CompletableFuture.completedFuture(
                                        "Save 2 positions: 3.14, or you could ask me to save how many positions you want."
                                );
                            }
                        })
                        .build()
        );
    }


    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(new String[]{"-p", "8091"});
    }
}
