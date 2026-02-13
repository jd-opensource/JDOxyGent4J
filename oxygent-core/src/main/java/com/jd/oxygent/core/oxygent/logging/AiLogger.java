package com.jd.oxygent.core.oxygent.logging;

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.llms.RemoteLlm;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * log formatted data for custom enterprise monitor system
 */
public interface AiLogger {

    void setEnabled(boolean enabled);

    boolean isEnabled();

    /**
     * log formatted data for custom enterprise monitor system
     * @param logType llm, tool, agent
     * @param logData OxyResponse
     * @param logTarget permits RemoteLlm, BaseOxy
     * @param extraData
     */
    void log(String logType, Object logData, Object logTarget, Map<String, String> extraData);
}
