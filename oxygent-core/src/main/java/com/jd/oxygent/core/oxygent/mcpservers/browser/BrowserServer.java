package com.jd.oxygent.core.oxygent.mcpservers.browser;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.EnableMcpServer;
import com.jd.oxygent.core.oxygent.mcpservers.engine.McpServer;

/**
 * MCP Server for Browser Operations
 *
 * This file is the entry point for the browser operations MCP server.
 * It imports all browser tool modules and provides an entry point to run the MCP server.
 */
public class BrowserServer {

    @EnableMcpServer(mode = "stdio",scanBasePackages={"com.jd.oxygent.core.oxygent.mcpservers.browser"})
    public static void main(String[] args) {
        // Print startup information
        System.out.println("Starting browser operations MCP server...");
        System.out.println("Available tools:");
        System.out.println("- browser_navigate: Navigate to specified URL and get page content");
        System.out.println("- browser_navigate_back: Return to previous page");
        System.out.println("- browser_navigate_forward: Navigate forward to next page");
        System.out.println("- browser_click: Click element");
        System.out.println("- browser_hover: Hover over element");
        System.out.println("- browser_type: Type text in element");
        System.out.println("- browser_snapshot: Capture page accessibility snapshot");
        System.out.println("- browser_take_screenshot: Take page screenshot");
        System.out.println("- browser_tab_list: List all browser tabs");
        System.out.println("- browser_tab_new: Open new tab");
        System.out.println("- browser_tab_close: Close tab");
        System.out.println("- browser_auto_login: Automatically login to specified website");
        System.out.println("- browser_search: Perform web search and return search results and page content");
        System.out.println("- browser_check_status: Check browser operation status");

        // Register exit handler
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            BrowserCore._close_browser();
        }));

        // Start MCP server
        McpServer.start();
    }
}
