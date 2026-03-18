package com.jd.oxygent.core.oxygent.mcpservers.browser;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.microsoft.playwright.Page;

import java.util.LinkedHashMap;
import java.util.Map;

public class BrowserInteraction {

    /**
     * Click element on the page
     */
    @MCPTool(name = "browser_click", description = "Click element")
    public Map<String, Object> browserClick(
            @ToolParam(description = "CSS selector of the element to click") String selector,
            @ToolParam(description = "Timeout to wait for element to appear (milliseconds)", defaultValue = "5000") int timeout) {

        BrowserCore.setOperationStatus(true);
        try {
            Page page = BrowserCore.ensurePage();
            page.waitForSelector(selector, new Page.WaitForSelectorOptions().setTimeout(timeout));
            page.click(selector);
            Thread.sleep(1000);
            BrowserCore.verifyDataReady();
            BrowserCore.setOperationStatus(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "Successfully clicked element: " + selector + ", data is ready");
            return result;
        } catch (Exception e) {
            BrowserCore.setOperationStatus(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "Error occurred while clicking element " + selector + ": " + e.getMessage());
            return result;
        }
    }

    @MCPTool(name = "browser_hover", description = "Hover over element")
    public Map<String, Object> browserHover(
            @ToolParam(description = "CSS selector of the element to hover over") String selector,
            @ToolParam(description = "Timeout to wait for element to appear (milliseconds)", defaultValue = "5000") int timeout) {

        BrowserCore.setOperationStatus(true);
        try {
            Page page = BrowserCore.ensurePage();
            page.waitForSelector(selector, new Page.WaitForSelectorOptions().setTimeout(timeout));
            page.hover(selector);
            Thread.sleep(500);
            BrowserCore.verifyDataReady();
            BrowserCore.setOperationStatus(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "Successfully hovered over element: " + selector + ", data is ready");
            return result;
        } catch (Exception e) {
            BrowserCore.setOperationStatus(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "Error occurred while hovering over element " + selector + ": " + e.getMessage());
            return result;
        }
    }

    @MCPTool(name = "browser_type", description = "Type text in element")
    public Map<String, Object> browserType(
            @ToolParam(description = "CSS selector of the element to type text in") String selector,
            @ToolParam(description = "Text to type") String text,
            @ToolParam(description = "Timeout to wait for element to appear (milliseconds)", defaultValue = "5000") int timeout) {

        BrowserCore.setOperationStatus(true);
        try {
            Page page = BrowserCore.ensurePage();
            page.waitForSelector(selector, new Page.WaitForSelectorOptions().setTimeout(timeout));
            page.fill(selector, "");
            page.type(selector, text);
            BrowserCore.verifyDataReady();
            BrowserCore.setOperationStatus(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "Successfully typed text in element " + selector + ", data is ready");
            return result;
        } catch (Exception e) {
            BrowserCore.setOperationStatus(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "Error occurred while typing text in element " + selector + ": " + e.getMessage());
            return result;
        }
    }
}
