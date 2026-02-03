package com.jd.oxygent.core.oxygent.mcpservers.browser;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.microsoft.playwright.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Browser tab management functionality
 * 
 * Provides functions for listing all browser tabs, opening new tabs, and closing tabs
 */
public class BrowserTabs {

    @MCPTool(name = "browser_tab_list", description = "List all browser tabs")
    public Map<String, Object> browser_tab_list() {
        // Check dependencies
        List<String> missing_deps = BrowserCore.check_dependencies();
        if (!missing_deps.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "Missing required libraries: " + String.join(", ", missing_deps) + ". Please install using maven: install " + String.join(" ", missing_deps));
            return result;
        }

        BrowserCore._set_operation_status(true);
        BrowserCore._ensure_browser();

        Map<String, Page> pages = BrowserCore.get_pages();
        String current_page_id = BrowserCore.get_current_page_id();

        if (pages.isEmpty()) {
            BrowserCore._set_operation_status(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "No open tabs");
            return result;
        }

        List<Map<String, Object>> tabs = new ArrayList<>();
        for (Map.Entry<String, Page> entry : pages.entrySet()) {
            try {
                Map<String, Object> tab = new LinkedHashMap<>();
                tab.put("id", entry.getKey());
                tab.put("title", entry.getValue().title());
                tab.put("url", entry.getValue().url());
                tab.put("is_current", entry.getKey().equals(current_page_id));
                tabs.add(tab);
            } catch (Exception e) {
                // Page may be closed
            }
        }

        BrowserCore._verify_data_ready();
        BrowserCore._set_operation_status(false);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tabs", tabs);
        return result;
    }

    @MCPTool(name = "browser_tab_new", description = "Open new tab")
    public Map<String, Object> browser_tab_new(
            @ToolParam(description = "URL to open in new tab", defaultValue = "about:blank") String url) {
        // 检查依赖
        List<String> missing_deps = BrowserCore.check_dependencies();
        if (!missing_deps.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "Missing required libraries:" + String.join(", ", missing_deps) + "。Please install using maven: install " + String.join(" ", missing_deps));
            return result;
        }

        BrowserCore._set_operation_status(true);
        BrowserCore._ensure_browser();

        // Ensure _context is initialized
        if (BrowserCore.get_context() == null) {
            BrowserCore._ensure_browser();
        }

        // At this point _context should be initialized, but for type checking, we verify again
        if (BrowserCore.get_context() != null) {
            // Create new page
            Page page = BrowserCore.get_context().newPage();

            // Add page to global dictionary
            Map<String, Page> pages = BrowserCore.get_pages();
            String page_id = "page_" + (pages.size() + 1);
            BrowserCore.add_page_to_pages(page_id, page);
            BrowserCore.set_current_page_id(page_id);

            // If URL is provided, navigate to that URL
            if (!url.equals("about:blank")) {
                page.navigate(url);
                // Wait for page to load
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            BrowserCore._verify_data_ready();
            BrowserCore._set_operation_status(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "New tab opened, ID: " + page_id + ", data is ready");
            result.put("page_id", page_id);
            return result;
        } else {
            BrowserCore._set_operation_status(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "Failed to initialize browser context");
            return result;
        }
    }

    @MCPTool(name = "browser_tab_close", description = "Close tab")
    public Map<String, Object> browser_tab_close(
            @ToolParam(description = "ID of tab to close, if empty closes current tab", defaultValue = "") String page_id) {
        // 检查依赖
        List<String> missing_deps = BrowserCore.check_dependencies();
        if (!missing_deps.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "Missing required libraries: " + String.join(", ", missing_deps) + "。Please install using maven: install " + String.join(" ", missing_deps));
            return result;
        }

        BrowserCore._set_operation_status(true);
        BrowserCore._ensure_browser();

        Map<String, Page> pages = BrowserCore.get_pages();
        String current_page_id = BrowserCore.get_current_page_id();

        if (pages.isEmpty()) {
            BrowserCore._set_operation_status(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "No open tabs to close");
            return result;
        }

        // Determine the page ID to close
        String target_page_id = (page_id.isEmpty() || !pages.containsKey(page_id)) ? current_page_id : page_id;

        if (!pages.containsKey(target_page_id)) {
            BrowserCore._set_operation_status(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "Cannot find tab with ID " + target_page_id);
            return result;
        }

        // Close page
        pages.get(target_page_id).close();

        // Remove page
        boolean success = BrowserCore.remove_page_from_pages(target_page_id);

        // If closing the current page, switch to another page
        if (target_page_id.equals(current_page_id)) {
            pages = BrowserCore.get_pages();  // Re-get page dictionary as it may have been modified
            if (!pages.isEmpty()) {
                BrowserCore.set_current_page_id(pages.keySet().iterator().next());
            } else {
                BrowserCore.set_current_page_id(null);
            }
        }

        BrowserCore._verify_data_ready();
        BrowserCore._set_operation_status(false);

        if (success) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "Tab closed, ID: " + target_page_id + ", operation completed");
            return result;
        } else {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "Failed to close tab, ID: " + target_page_id);
            return result;
        }
    }
}
