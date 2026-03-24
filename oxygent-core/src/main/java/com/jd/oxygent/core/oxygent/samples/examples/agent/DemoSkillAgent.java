package com.jd.oxygent.core.oxygent.samples.examples.agent;

import com.jd.oxygent.core.oxygent.config.Prompts;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.SkillAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.tools.PresetTools;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class DemoSkillAgent {

    @OxySpaceBean(value = "skillAgentOxySpace", defaultStart = true, query = "What skills do you have?")
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .build(),
                PresetTools.FILE_TOOLS,
                PresetTools.SHELL_TOOLS,
                SkillAgent.builder()
                        .name("skill_agent")
                        .isMaster(true)
                        .prompt(Prompts.SYSTEM_PROMPT_SKILLS) // must be set
                        .desc("An tool for execute shell command")
                        .tools(List.of("file_tools", "shell_tools"))
                        .skills(List.of(".oxygent/skills"
//                                "D:/skills",
//                                "classpath:skills",
//                                "classpath:com/jd/oxygent/core/oxygent/preset_skills/skill_creator",
//                                Path.of(System.getProperty("user.home"), "skills").toString()
                        )) // Supports absolute paths, project root relative paths, classpath resources, and paths within JAR files.
                        .build()
        );
    }

    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);
    }
}
