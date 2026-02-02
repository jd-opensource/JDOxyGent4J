package com.jd.oxygent.core.oxygent.mcpservers.browser;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.microsoft.playwright.Page;

import java.util.LinkedHashMap;
import java.util.Map;

public class BrowserInteraction {

    /**
     * 点击页面上的元素
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
            result.put("message", "成功点击元素: " + selector + "，数据已准备就绪");
            return result;
        } catch (Exception e) {
            BrowserCore.setOperationStatus(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "点击元素 " + selector + " 时发生错误: " + e.getMessage());
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
            result.put("message", "成功悬停在元素上: " + selector + "，数据已准备就绪");
            return result;
        } catch (Exception e) {
            BrowserCore.setOperationStatus(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "悬停在元素 " + selector + " 上时发生错误: " + e.getMessage());
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
            result.put("message", "成功在元素 " + selector + " 中输入文本，数据已准备就绪");
            return result;
        } catch (Exception e) {
            BrowserCore.setOperationStatus(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "在元素 " + selector + " 中输入文本时发生错误: " + e.getMessage());
            return result;
        }
    }
}
