package com.jd.oxygent.core.oxygent.samples.server.masprovider.oxyspace.impl;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ReActAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.tools.PresetTools;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.oxyspace.OxySpaceProvider;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DefaultOxySpaceProvider implements OxySpaceProvider {
    @Override
    public Map<String, List<BaseOxy>> getCustomOxySpace() {
        return new HashMap<String, List<BaseOxy>>() {{
            put("defaultAppOxySpace", getDefaultOxySpace());
        }};
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

                // 2. Time tools (assuming preset_tools.time_tools is a predefined Tool instance)
                PresetTools.TIME_TOOLS, // Need to define PresetTools class

                // 3. Time agent
                ReActAgent.builder()
                        .name("time_agent")
                        .desc("A tool that can query the time")
                        .tools(Arrays.asList("time_tools")) // Tool name list
                        .build(),
                // 4. File tools
                PresetTools.FILE_TOOLS,

                // 5. File agent
                ReActAgent.builder()
                        .name("file_agent")
                        .desc("A tool that can operate the file system")
                        .tools(Arrays.asList("file_tools"))
                        .build(),

                // 6. Math tools
                PresetTools.MATH_TOOLS,
                // 7. Math agent
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

    @Override
    public String getOxySpaceName() {
        return "default";
    }
}