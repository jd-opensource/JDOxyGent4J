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
 * Browser content extraction and screenshot functionality
 *
 * Provides functionality to capture page accessibility snapshots and take page screenshots
 */
public class BrowserContent {

    @MCPTool(name = "browser_snapshot", description = "捕获页面的可访问性快照")
    public String browser_snapshot() {
        // Check dependencies
        java.util.List<String> missing_deps = BrowserCore.check_dependencies();
        if (!missing_deps.isEmpty()) {
            return "Missing required libraries: " + String.join(", ", missing_deps) + ". Please install using maven: install " + String.join(" ", missing_deps);
        }

        BrowserCore._set_operation_status(true);
        try {
            Page page = BrowserCore._ensure_page();

            // Get page information
            String title = page.title();
            String url = page.url();

            // Get page content
            String content = page.content();

            // Extract page text
            String text = page.evaluate("() => { return document.body.innerText; }").toString();

            // If text is too long, truncate to first 2000 characters
            if (text.length() > 2000) {
                text = text.substring(0, 2000) + "...(content truncated)";
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
            return "Error occurred while capturing page snapshot: " + e.getMessage();
        }
    }

    @MCPTool(name = "browser_take_screenshot", description = "截取页面截图")
    public Object browser_take_screenshot(
            @ToolParam(description = "保存截图的路径，如果为空则保存到cache_dir目录", defaultValue = "") String path,
            @ToolParam(description = "是否截取整个页面，而不仅仅是可见区域", defaultValue = "false") boolean full_page) {
        // Check dependencies
        List<String> missing_deps = BrowserCore.check_dependencies();
        if (!missing_deps.isEmpty()) {
            return "Missing required libraries: " + String.join(", ", missing_deps) + ". Please install using maven: install " + String.join(" ", missing_deps);
        }

        BrowserCore._set_operation_status(true);
        try {
            Page page = BrowserCore._ensure_page();

            // Wait for page to stabilize
            Thread.sleep(500);

            // Take screenshot
            byte[] screenshot_bytes = page.screenshot(new Page.ScreenshotOptions().setFullPage(full_page));

            // Calculate image size (for information display)
            double size_mb = screenshot_bytes.length / (1024.0 * 1024.0);

            // Determine save path
            String save_path = path;
            if (save_path.isEmpty()) {
                // Create cache_dir directory (if it doesn't exist)
                String cache_dir = new File(System.getProperty("user.dir")).getParent() + File.separator + "cache_dir";
                Files.createDirectories(Paths.get(cache_dir));

                // Create screenshot subdirectory (if it doesn't exist)
                String screenshot_dir = cache_dir + File.separator + "screenshot";
                Files.createDirectories(Paths.get(screenshot_dir));

                // Generate unique filename
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String unique_id = UUID.randomUUID().toString().substring(0, 8);
                String filename = timestamp + "_" + unique_id + ".png";

                // Complete save path
                save_path = screenshot_dir + File.separator + filename;
            } else {
                // Ensure directory exists
                Path parentPath = Paths.get(save_path).getParent();
                if (parentPath != null) {
                    Files.createDirectories(parentPath);
                }
            }

            // Save screenshot to file
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
            return "Error occurred while taking page screenshot: " + e.getMessage();
        }
    }
}
