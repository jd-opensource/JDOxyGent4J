package com.jd.oxygent.oxybank.api.controller;

import com.jd.oxygent.oxybank.api.models.APIResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
     * @param levelRequest Log level
     *            - TRACE: Most detailed logging
     *            - DEBUG: Debug information
     *            - INFO: General information (default)
     *            - SUCCESS: Success information
     *            - WARNING: Warning information
     *            - ERROR: Error information
     *            - CRITICAL: Critical error
     * @return APIResponse containing the updated log level
     */
    @PostMapping("/level")
    public APIResponse<String> setLogLevel(@RequestBody LogLevelRequest levelRequest) {
        String level = levelRequest.getLevel();
        try {
            setLogLevel(level);
            return APIResponse.success("success", "Log level updated to: " + level);
        } catch (IllegalArgumentException e) {
            log.error("Invalid log level: {}", level, e);
            return APIResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to set log level", e);
            return APIResponse.error(500, "Failed to set log level");
        }
    }

    /**
     * Set log level
     *
     * @param level Log level
     */
    private void setLogLevel(String level) {
        List<String> validLevels = List.of("TRACE", "DEBUG", "INFO", "SUCCESS", "WARNING", "ERROR", "CRITICAL");
        String levelUpper = level.toUpperCase();

        if (!validLevels.contains(levelUpper)) {
            throw new IllegalArgumentException("Invalid log level: " + level + ", valid values: " + validLevels);
        }

        // FIXME: Implement actual log level setting using SLF4J
        // In Java, we would typically use LoggerFactory to get the logger and set its level
        // This requires access to the underlying logging framework's configuration
        this.logLevel = levelUpper;
        log.info("Log level updated to: {}", levelUpper);
    }

    /**
     * Log level request model
     */
    public static class LogLevelRequest {
        private String level;

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }
    }
}
