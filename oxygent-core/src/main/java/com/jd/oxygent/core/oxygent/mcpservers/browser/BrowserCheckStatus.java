package com.jd.oxygent.core.oxygent.mcpservers.browser;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 浏览器状态检查功能
 *
 * 提供检查浏览器操作状态的功能
 */
public class BrowserCheckStatus {

    private BrowserCheckStatus() {
    }

    @MCPTool(name = "browser_check_status", description = "检查浏览器操作状态")
    public Object browser_check_status() {
        /**
         * 检查浏览器操作状态，确认数据是否已准备就绪
         */
        try {
            if (BrowserCore.get_browser() == null) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("browser_initialized", false);
                result.put("operation_in_progress", false);
                result.put("data_ready", false);
                result.put("message", "浏览器尚未初始化");
                return result;
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("browser_initialized", true);
            result.put("operation_in_progress", BrowserCore.is_operation_in_progress());
            result.put("data_ready", BrowserCore.is_data_ready());
            result.put("active_pages", BrowserCore.get_pages().size());
            result.put("message", BrowserCore.is_data_ready() ? "数据已准备就绪" : "操作正在进行中，数据尚未准备就绪");
            return result;
        } catch (Exception e) {
            return "检查浏览器状态时发生错误: " + e.getMessage();
        }
    }
}
