package com.jd.oxygent.core.oxygent.samples.examples.banks;
import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.bank_tools.BankClient;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.MasFactoryRegistry;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
/**
 * Bank chat agent - Save memory example
 */
@Slf4j
public class DemoBankChatAgentDumpMemory {

    static {
        Config.getServer().setPort(8999); // Use different port
    }

    /**
     * Filter function - Set user PIN
     */
    public static Function<Map<String, Object>, Map<String, Object>> funcFilter = (payload) -> {
        Map<String, Object> groupData = new HashMap<>();
        groupData.put("user_pin", "002");
        payload.put("group_data", groupData);
        return payload;
    };

    @OxySpaceBean(value = "temp_app", defaultStart = true , query = "Who I am")
    public static List<BaseOxy> getDemoBankChatAgentOxySpace() {
        return Arrays.asList(
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

                ChatAgent.builder()
                        .name("qa_agent")
                        .llmModel("default_llm")
                        .prompt("You can refer to the following information to answer the question:\n${preceding_text}")
                        .banks(Arrays.asList("remote_user_profile_banks"))
                        .precedingOxy(Arrays.asList("userProfileRetrieve"))
                        .precedingPlaceholder("preceding_text")
                        .funcProcessOutput((oxyResponse)->{
                            OxyRequest oxyRequest = oxyResponse.getOxyRequest();
                            // Build history record
                            Map<String, String> history = new HashMap<>();
                            history.put("query", (String) oxyRequest.getArguments().get("query"));
                            history.put("answer", (String) oxyResponse.getOutput());
                            oxyRequest.callAsync(new HashMap<String, Object>(){{
                                this.put("callee","userProfileDeposit");
                                this.put("arguments",new HashMap<String,Object>(){{
                                    this.put("content",history);
                                }});
                                this.put("is_send_message",false);
                            }});
                            return oxyResponse;
                        })
                        .build(),
                BankClient.builder()
                        .name("remote_user_profile_banks")
                        .serverUrl("http://127.0.0.1:8090/api")
                        .build()
        );
    }

    /**
     * Start MAS and set filter
     */
    public static void main(String[] args) throws Exception {
        // 启动服务
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        MasFactoryRegistry.getFactory().createMas().setFuncFilter((payload)->{
            payload.put("group_data",Map.of("user_pin","002"));
            return payload;
        });
        ServerApp.main(args);
    }
}