package com.jd.oxygent.core.oxygent.samples.server.utils;

import lombok.extern.slf4j.Slf4j;

import java.awt.Desktop;
import java.net.URI;

/**
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class BrowserOpener {

    public static boolean open(String url) {
        try {
            // Check if desktop browsing is supported
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop desktop = Desktop.getDesktop();
                desktop.browse(new URI(url));
                log.info("Browser successfully opened: {}", url);
            } else {
                log.warn("Current environment does not support opening browser for: {}", url);
            }
        } catch (Exception e) {
            log.error("Failed to open browser: {}", e.getMessage());
        }
        // Try to open browser via command line
        return openBrowserViaCommand(url);
    }

    private static boolean openBrowserViaCommand(String url) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (os.contains("win")) {
                // Windows
                pb = new ProcessBuilder("cmd", "/c", "start", url);
            } else if (os.contains("mac")) {
                // macOS
                pb = new ProcessBuilder("open", url);
            } else {
                // Linux/Unix
                String[] commands = {"xdg-open", "gnome-open", "kde-open", "x-www-browser"};
                for (String cmd : commands) {
                    try {
                        pb = new ProcessBuilder(cmd, url);
                        Process process = pb.start();
                        if (process.waitFor() == 0) {
                            return true;
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
                return false;
            }

            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to open browser via command: {}", e.getMessage());
        }
        return false;
    }
}