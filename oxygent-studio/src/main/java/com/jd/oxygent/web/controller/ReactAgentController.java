package com.jd.oxygent.web.controller;

import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.Mas;
import com.jd.oxygent.core.MasFactoryBean;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyResponse;
import com.jd.oxygent.core.oxygent.tools.PresetTools;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * React agent web controller.
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/react")
public class ReactAgentController {

    @Autowired
    private MasFactoryBean masFactoryBean;

    private static final Logger logger = LoggerFactory.getLogger(ReactAgentController.class);

    @RequestMapping(value = "/chatWithAgent", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public OxyResponse chatWithAgent(@RequestParam(required = false) String query, @RequestParam(required = false) String pin) throws Exception {

        List<BaseOxy> oxySpaceList = getDefaultOxySpace();
        masFactoryBean.setOxySpace("app", oxySpaceList);
        Mas mas = masFactoryBean.getObject();

        if (query == null) {
            query = "What time is it now? Please save it into time.txt.";
        }
        mas.setFirstQuery(query);
        Map<String, Object> info = new HashMap<>();
        info.put("current_trace_id", UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        info.put("request_id", UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        info.put("from_trace_id", "");
        info.put("query", query);
        Map<String, Object> groupData = new HashMap<>();
        Map<String, Object> user = new HashMap<>();
        groupData.put("user", user);
        groupData.put("biz_type", Config.getBizType());
        groupData.put("app_id", Config.getAppName());
        info.put("group_data", groupData);

        logger.info("Starting request to chatWithAgent");
        OxyResponse oxyResponse = mas.chatWithAgent(info);

        return oxyResponse;
    }

    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .llmParams(Map.of("temperature", 0.01)) // Use Map.of to create immutable Map
                        .timeout(30)
                        .build(),

                // 2. Time Tools (assume preset_tools.time_tools is a predefined Tool instance)
                PresetTools.TIME_TOOLS, // Need to define PresetTools class

                // 3. Time Agent
                ReActAgent.builder()
                        .name("time_agent")
                        .desc("A tool that can query the time")
                        .tools(Arrays.asList("time_tools")) // Tool name list
                        .build(),
                // 4. File Tools
                PresetTools.FILE_TOOLS,

                // 5. File Agent
                ReActAgent.builder()
                        .name("file_agent")
                        .desc("A tool that can operate the file system")
                        .tools(Arrays.asList("file_tools"))
                        .build(),

                // 6. Math Tools
                PresetTools.MATH_TOOLS,
                // 7. Math Agent
                ReActAgent.builder()
                        .name("math_agent")
                        .desc("A tool that can perform mathematical calculations.")
                        .tools(Arrays.asList("math_tools"))
                        .build(),

                // 8. Master Agent
                ReActAgent.builder()
                        .isMaster(true) // Set as master agent
                        .name("master_agent")
                        .llmModel("default_llm")
                        .subAgents(Arrays.asList("time_agent", "file_agent", "math_agent")) // Sub-agent list
                        .build()
        );
    }
}
