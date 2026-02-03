package com.jd.oxygent.core.oxygent.mcpservers.browser;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Browser status check functionality
 *
 * Provides functionality to check browser operation status
 */
public class BrowserCheckStatus {

    @MCPTool(name = "browser_check_status", description = "检查浏览器操作状态")
    public Object browser_check_status() {
        /**
         * Check browser operation status and confirm if data is ready
         */
        try {
            if (BrowserCore.get_browser() == null) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("browser_initialized", false);
                result.put("operation_in_progress", false);
                result.put("data_ready", false);
                result.put("message", "Browser not initialized");
                return result;
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("browser_initialized", true);
            result.put("operation_in_progress", BrowserCore.is_operation_in_progress());
            result.put("data_ready", BrowserCore.is_data_ready());
            result.put("active_pages", BrowserCore.get_pages().size());
            result.put("message", BrowserCore.is_data_ready() ? "Data is ready" : "Operation in progress, data not ready yet");
            return result;
        } catch (Exception e) {
            return "Error occurred while checking browser status: " + e.getMessage();
        }
    }
}
