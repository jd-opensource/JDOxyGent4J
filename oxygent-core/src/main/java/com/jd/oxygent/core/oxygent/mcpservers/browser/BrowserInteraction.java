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
    @MCPTool(name = "browser_click", description = "点击元素")
    public Map<String, Object> browserClick(
            @ToolParam(description = "要点击的元素的CSS选择器") String selector,
            @ToolParam(description = "等待元素出现的超时时间(毫秒)", defaultValue = "5000") int timeout) {

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

    @MCPTool(name = "browser_hover", description = "悬停在元素上")
    public Map<String, Object> browserHover(
            @ToolParam(description = "要悬停的元素的CSS选择器") String selector,
            @ToolParam(description = "等待元素出现的超时时间(毫秒)", defaultValue = "5000") int timeout) {

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

    @MCPTool(name = "browser_type", description = "在元素中输入文本")
    public Map<String, Object> browserType(
            @ToolParam(description = "要输入文本的元素的CSS选择器") String selector,
            @ToolParam(description = "要输入的文本") String text,
            @ToolParam(description = "等待元素出现的超时时间(毫秒)", defaultValue = "5000") int timeout) {

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
