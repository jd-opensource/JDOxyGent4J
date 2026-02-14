package com.jd.oxygent.oxybank.api.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.jd.oxygent.oxybank.api.model.APIResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Log Configuration Controller
 * <p>
 * For dynamically modifying log levels
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/log")
public class LogConfigController {

    // Store current log level
    private String logLevel = "DEBUG";

    /**
     * Get current log level
     *
     * @return Current log level
     */
    @GetMapping("/level")
    public APIResponse<String> getCurrentLogLevel() {
        return APIResponse.success("Successfully retrieved log level", logLevel);
    }

    /**
     * Dynamically set log level
     * <p>
     * Allows modifying log level at runtime without restarting the service.
     *
     * @return APIResponse containing the updated log level
     */
    @PostMapping("/level")
    public APIResponse<String> setLogLevel(String packageName, String level) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        // 如果 packageName 为空，则修改 Root Logger
        loggerContext.getLogger(packageName).setLevel(Level.toLevel(level));
        return APIResponse.success("success", "Log level updated to: " + level);
    }
}
