package com.jd.oxygent.core.oxygent.mcpservers.browser;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.microsoft.playwright.Page;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 浏览器内容提取和截图功能
 *
 * 提供捕获页面的可访问性快照和截取页面截图等功能
 */
public class BrowserContent {

    @MCPTool(name = "browser_snapshot", description = "捕获页面的可访问性快照")
    public String browser_snapshot() {
        // 检查依赖
        java.util.List<String> missing_deps = BrowserCore.check_dependencies();
        if (!missing_deps.isEmpty()) {
            return "缺少必要的库: " + String.join(", ", missing_deps) + "。请使用pip安装: pip install " + String.join(" ", missing_deps);
        }

        BrowserCore._set_operation_status(true);
        try {
            Page page = BrowserCore._ensure_page();

            // 获取页面信息
            String title = page.title();
            String url = page.url();

            // 获取页面内容
            String content = page.content();

            // 提取页面文本
            String text = page.evaluate("() => { return document.body.innerText; }").toString();

            // 如果文本太长，截取前2000个字符
            if (text.length() > 2000) {
                text = text.substring(0, 2000) + "...(内容已截断)";
            }

            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("title", title);
            snapshot.put("url", url);
            snapshot.put("text", text);
            snapshot.put("data_complete", true);

            BrowserCore._verify_data_ready();
            BrowserCore._set_operation_status(false);
            return snapshot.toString();
        } catch (Exception e) {
            BrowserCore._set_operation_status(false);
            return "捕获页面快照时发生错误: " + e.getMessage();
        }
    }

    @MCPTool(name = "browser_take_screenshot", description = "截取页面截图")
    public Object browser_take_screenshot(
            @ToolParam(description = "保存截图的路径，如果为空则保存到cache_dir目录", defaultValue = "") String path,
            @ToolParam(description = "是否截取整个页面，而不仅仅是可见区域", defaultValue = "false") boolean full_page) {
        // 检查依赖
        List<String> missing_deps = BrowserCore.check_dependencies();
        if (!missing_deps.isEmpty()) {
            return "缺少必要的库: " + String.join(", ", missing_deps) + "。请使用pip安装: pip install " + String.join(" ", missing_deps);
        }

        BrowserCore._set_operation_status(true);
        try {
            Page page = BrowserCore._ensure_page();

            // 等待页面稳定
            Thread.sleep(500);

            // 截取截图
            byte[] screenshot_bytes = page.screenshot(new Page.ScreenshotOptions().setFullPage(full_page));

            // 计算图片大小（用于信息展示）
            double size_mb = screenshot_bytes.length / (1024.0 * 1024.0);

            // 确定保存路径
            String save_path = path;
            if (save_path.isEmpty()) {
                // 创建cache_dir目录（如果不存在）
                String cache_dir = new File(System.getProperty("user.dir")).getParent() + File.separator + "cache_dir";
                Files.createDirectories(Paths.get(cache_dir));

                // 创建screenshot子目录（如果不存在）
                String screenshot_dir = cache_dir + File.separator + "screenshot";
                Files.createDirectories(Paths.get(screenshot_dir));

                // 生成唯一的文件名
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String unique_id = UUID.randomUUID().toString().substring(0, 8);
                String filename = timestamp + "_" + unique_id + ".png";

                // 完整的保存路径
                save_path = screenshot_dir + File.separator + filename;
            } else {
                // 确保目录存在
                Path parentPath = Paths.get(save_path).getParent();
                if (parentPath != null) {
                    Files.createDirectories(parentPath);
                }
            }

            // 保存截图到文件
            try (FileOutputStream fos = new FileOutputStream(save_path)) {
                fos.write(screenshot_bytes);
            }

            BrowserCore._verify_data_ready();
            BrowserCore._set_operation_status(false);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("path", save_path);
            result.put("size_mb", Math.round(size_mb * 100.0) / 100.0);
            return result;
        } catch (Exception e) {
            BrowserCore._set_operation_status(false);
            return "截取页面截图时发生错误: " + e.getMessage();
        }
    }
}
