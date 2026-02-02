package com.jd.oxygent.core.oxygent.mcpservers.browser;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.EnableMcpServer;
import com.jd.oxygent.core.oxygent.mcpservers.engine.McpServer;

/**
 * MCP Server for Browser Operations
 *
 * 这个文件是浏览器操作MCP服务器的入口点。
 * 它导入所有的浏览器工具模块，并提供一个运行MCP服务器的入口点。
 */
public class BrowserServer {

    @EnableMcpServer(mode = "stdio",scanBasePackages={"com.jd.oxygent.core.oxygent.mcpservers.browser"})
    public static void main(String[] args) {
        // 打印启动信息
        System.out.println("启动浏览器操作MCP服务器...");
        System.out.println("可用工具:");
        System.out.println("- browser_navigate: 导航到指定URL并获取页面内容");
        System.out.println("- browser_navigate_back: 返回上一页");
        System.out.println("- browser_navigate_forward: 前进到下一页");
        System.out.println("- browser_click: 点击元素");
        System.out.println("- browser_hover: 悬停在元素上");
        System.out.println("- browser_type: 在元素中输入文本");
        System.out.println("- browser_snapshot: 捕获页面的可访问性快照");
        System.out.println("- browser_take_screenshot: 截取页面截图");
        System.out.println("- browser_tab_list: 列出所有浏览器标签");
        System.out.println("- browser_tab_new: 打开新标签");
        System.out.println("- browser_tab_close: 关闭标签");
        System.out.println("- browser_auto_login: 自动登录到指定网站");
        System.out.println("- browser_search: 执行网络搜索并返回搜索结果及页面内容");
        System.out.println("- browser_check_status: 检查浏览器操作状态");

        // 注册退出处理函数
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            BrowserCore._close_browser();
        }));

        // 启动MCP服务器
        McpServer.start();
    }
}
