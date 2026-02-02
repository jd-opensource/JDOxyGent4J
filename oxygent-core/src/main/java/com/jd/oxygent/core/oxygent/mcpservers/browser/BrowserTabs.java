package com.jd.oxygent.core.oxygent.mcpservers.browser;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.microsoft.playwright.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 浏览器标签页管理功能
 * 
 * 提供列出所有浏览器标签、打开新标签和关闭标签等功能
 */
public class BrowserTabs {

    @MCPTool(name = "browser_tab_list", description = "列出所有浏览器标签")
    public Map<String, Object> browser_tab_list() {
        // 检查依赖
        List<String> missing_deps = BrowserCore.check_dependencies();
        if (!missing_deps.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "缺少必要的库: " + String.join(", ", missing_deps) + "。请使用pip安装: pip install " + String.join(" ", missing_deps));
            return result;
        }

        BrowserCore._set_operation_status(true);
        BrowserCore._ensure_browser();

        Map<String, Page> pages = BrowserCore.get_pages();
        String current_page_id = BrowserCore.get_current_page_id();

        if (pages.isEmpty()) {
            BrowserCore._set_operation_status(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "没有打开的标签");
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
                // 页面可能已关闭
            }
        }

        BrowserCore._verify_data_ready();
        BrowserCore._set_operation_status(false);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tabs", tabs);
        return result;
    }

    @MCPTool(name = "browser_tab_new", description = "打开新标签")
    public Map<String, Object> browser_tab_new(
            @ToolParam(description = "在新标签中打开的URL", defaultValue = "about:blank") String url) {
        // 检查依赖
        List<String> missing_deps = BrowserCore.check_dependencies();
        if (!missing_deps.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "缺少必要的库: " + String.join(", ", missing_deps) + "。请使用pip安装: pip install " + String.join(" ", missing_deps));
            return result;
        }

        BrowserCore._set_operation_status(true);
        BrowserCore._ensure_browser();

        // 确保_context已初始化
        if (BrowserCore.get_context() == null) {
            BrowserCore._ensure_browser();
        }

        // 此时_context应该已经初始化，但为了类型检查，我们再次验证
        if (BrowserCore.get_context() != null) {
            // 创建新页面
            Page page = BrowserCore.get_context().newPage();

            // 添加页面到全局字典
            Map<String, Page> pages = BrowserCore.get_pages();
            String page_id = "page_" + (pages.size() + 1);
            BrowserCore.add_page_to_pages(page_id, page);
            BrowserCore.set_current_page_id(page_id);

            // 如果提供了URL，则导航到该URL
            if (!url.equals("about:blank")) {
                page.navigate(url);
                // 等待页面加载
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            BrowserCore._verify_data_ready();
            BrowserCore._set_operation_status(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "已打开新标签，ID: " + page_id + "，数据已准备就绪");
            result.put("page_id", page_id);
            return result;
        } else {
            BrowserCore._set_operation_status(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "无法初始化浏览器上下文");
            return result;
        }
    }

    @MCPTool(name = "browser_tab_close", description = "关闭标签")
    public Map<String, Object> browser_tab_close(
            @ToolParam(description = "要关闭的标签ID，如果为空则关闭当前标签", defaultValue = "") String page_id) {
        // 检查依赖
        List<String> missing_deps = BrowserCore.check_dependencies();
        if (!missing_deps.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "缺少必要的库: " + String.join(", ", missing_deps) + "。请使用pip安装: pip install " + String.join(" ", missing_deps));
            return result;
        }

        BrowserCore._set_operation_status(true);
        BrowserCore._ensure_browser();

        Map<String, Page> pages = BrowserCore.get_pages();
        String current_page_id = BrowserCore.get_current_page_id();

        if (pages.isEmpty()) {
            BrowserCore._set_operation_status(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "没有打开的标签可关闭");
            return result;
        }

        // 确定要关闭的页面ID
        String target_page_id = (page_id.isEmpty() || !pages.containsKey(page_id)) ? current_page_id : page_id;

        if (!pages.containsKey(target_page_id)) {
            BrowserCore._set_operation_status(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "找不到ID为 " + target_page_id + " 的标签");
            return result;
        }

        // 关闭页面
        pages.get(target_page_id).close();

        // 移除页面
        boolean success = BrowserCore.remove_page_from_pages(target_page_id);

        // 如果关闭的是当前页面，则切换到另一个页面
        if (target_page_id.equals(current_page_id)) {
            pages = BrowserCore.get_pages();  // 重新获取页面字典，因为它可能已经被修改
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
            result.put("message", "已关闭标签，ID: " + target_page_id + "，操作已完成");
            return result;
        } else {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "关闭标签失败，ID: " + target_page_id);
            return result;
        }
    }
}
