package com.jd.oxygent.core.oxygent.samples.examples.agent;

import com.jd.oxygent.core.oxygent.config.Prompts;
import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ShellUseAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.tools.PresetTools;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class DemoShellUseAgent {

    @OxySpaceBean(value = "shellUseAgentOxySpace", defaultStart = true, query = "Please run the demo.py from https://github.com/jd-opensource/OxyGent.git")
    public static List<BaseOxy> getDefaultOxySpace() {
        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("OXY_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("OXY_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("OXY_LLM_MODEL_NAME"))
                        .build(),
                PresetTools.SSH_TOOLS,
                ShellUseAgent.builder()
                        .name("shell_use_agent")
                        .isMaster(true)
                        .systemPrompt(Prompts.SYSTEM_PROMPT_SHELL_USE) // must be set
                        .desc("An tool for execute shell command")
                        .tools(List.of("ssh_tools"))
                        .maxReactRounds(64)
                        .isDiscardReactMemory(false)
                        .authInfo(new ShellUseAgent.AuthInfo(EnvUtils.getEnv("SSH_HOST"), 22, EnvUtils.getEnv("SSH_USER"), EnvUtils.getEnv("SSH_PASSWORD")))
                        .build()
        );
    }

    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);
    }
}
