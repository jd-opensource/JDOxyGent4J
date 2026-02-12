package com.jd.oxygent.infra.logging;

import com.jd.oxygent.core.oxygent.logging.AiLogger;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * log formatted data for custom enterprise monitor system
 */
@Service
public class FileAiLogger implements AiLogger {

    private boolean enabled = true;

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * log formatted data for custom enterprise monitor system
     * @param logType llm, tool, agent
     * @param logData OxyResponse
     * @param logTarget permits RemoteLlm, BaseOxy
     * @param extraData exception or elsp
     */
    @Override
    public void log(String logType, Object logData, Object logTarget, Map<String, Object> extraData) {
        return; // do custom logging here
    }
}
