package com.jd.oxygent.core.oxygent.samples.examples.applications;
import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.annotation.ApiEndpoint;
import com.jd.oxygent.core.oxygent.samples.server.annotation.ApiParam;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.MasFactoryRegistry;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bank Manager - BankRouter Style
 */
@Slf4j
public class BankManagerByBankRouter {

    static {
        Config.getServer().setPort(8090);
    }

    private static Map<String, String> userProfileDict = new ConcurrentHashMap<>();

    public BankManagerByBankRouter() {
        // Initialize sample data
        userProfileDict.put("001", "Arlen, a student, likes music");
        userProfileDict.put("002", "Tom, a programmer, likes sports");
    }

    @ApiEndpoint(
            path = "/user_profile_retrieve",
            method = ApiEndpoint.HttpMethod.POST,
            description = "A tool for querying user profile",
            tags = {"bank"}
    )
    public String userProfileRetrieve(
            @ApiParam(name = "query", description = "query") String query,
            @ApiParam(name = "user_pin", description = "SystemArg.user_pin") String user_pin,
            @ApiParam(name = "agent_pin", description = "SystemArg.agent_pin") String agent_pin
    ) {
        log.info("Querying user profile - user_pin: {}, agent_pin: {}, query: {}",
                user_pin, agent_pin, query);

        String portrait = userProfileDict.getOrDefault(user_pin, "Nothing");
        return String.format("The current user profile is: %s", portrait);
    }

    @ApiEndpoint(
            path = "/user_profile_deposit",
            method = ApiEndpoint.HttpMethod.POST,
            description = "A tool for updating user profile",
            tags = {"bank"}
    )
    public String userProfileDeposit(
            @ApiParam(name = "content", description = "content") String content,
            @ApiParam(name = "user_pin", description = "SystemArg.user_pin") String user_pin,
            @ApiParam(name = "agent_pin", description = "SystemArg.agent_pin") String agent_pin
    ) throws Exception {
        log.info("Updating user profile - user_pin: {}, agent_pin: {}, content: {}",
                user_pin, agent_pin, content);
        Mas mas = MasFactoryRegistry.getFactory().getMas();
        Object output = mas.call("bank_manager", new HashMap<>() {{
            this.put("query", "Please update the user profile.");
            this.put("chat", content);
            this.put("profile", userProfileDict.getOrDefault(user_pin, "Nothing"));
        }});
        // Directly store content (simplified processing, no LLM call)
        userProfileDict.put(user_pin, output.toString());
        return output.toString();
    }

    @OxySpaceBean(value = "bankManagerByBankRouter", defaultStart = true)
    public static List<BaseOxy> getBankManagerOxySpace() {
        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .build(),

                ChatAgent.builder()
                        .name("bank_manager")
                        .llmModel("default_llm")
                        .prompt("You are an expert in user profiling. Please update and refine the current user profile by integrating our previous conversation:\n${chat}\nAnd the current user profile:\n${profile}")
                        .build()
        );
    }

    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);
    }
}