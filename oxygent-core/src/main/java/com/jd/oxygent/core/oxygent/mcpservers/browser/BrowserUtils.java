package com.jd.oxygent.core.oxygent.mcpservers.browser;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class BrowserUtils {

    public static String getDomainFromUrl(String url) {
        try {
            URL parsedUrl = new URL(url);
            return parsedUrl.getHost().toLowerCase();
        } catch (Exception e) {
            return "";
        }
    }

    public static String generateScreenshotPath(String prefix, String subDir) {
        try {
            String cacheDir = System.getProperty("user.dir") + File.separator + "cache_dir";
            if (subDir != null && !subDir.isEmpty()) {
                cacheDir += File.separator + subDir;
            }
            Files.createDirectories(Paths.get(cacheDir));
            
            String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            );
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            return cacheDir + java.io.File.separator + prefix + "_" + timestamp + "_" + uniqueId + ".png";
        } catch (Exception e) {
            return System.getProperty("user.dir") + java.io.File.separator + prefix + "_" + 
                   UUID.randomUUID().toString().substring(0, 8) + ".png";
        }
    }
}
