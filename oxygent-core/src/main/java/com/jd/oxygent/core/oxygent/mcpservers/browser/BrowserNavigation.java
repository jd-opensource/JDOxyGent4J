package com.jd.oxygent.core.oxygent.mcpservers.browser;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitUntilState;
import java.util.*;

/**
 * 浏览器导航功能
 *
 * 提供页面导航、前进和后退等功能
 */
public class BrowserNavigation {

    // 域名登录配置字典，包含不同域名对应的选择器配置
    private static final Map<String, Map<String, String>> LOGIN_DOMAIN_CONFIGS = new LinkedHashMap<>();

    static {
        Map<String, String> jdConfig = new LinkedHashMap<>();
        jdConfig.put("username_selector", "#username");
        jdConfig.put("password_selector", "#password");
        jdConfig.put("submit_selector", "#formsubmitButton");
        LOGIN_DOMAIN_CONFIGS.put("ssa.jd.com", jdConfig);
    }

    @MCPTool(name = "browser_navigate", description = "导航到指定URL并获取页面内容")
    public Map<String, Object> browserNavigate(
            @ToolParam(description = "要导航到的网页URL") String url,
            @ToolParam(description = "等待页面加载的条件，可选值: load, domcontentloaded, networkidle", defaultValue = "load") String waitUntil,
            @ToolParam(description = "是否提取页面内容", defaultValue = "true") boolean extractContent) {

        // 检查依赖
        List<String> missingDeps = BrowserCore.check_dependencies();
        if (!missingDeps.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "缺少必要的库: " + String.join(", ", missingDeps) + "。请使用pip安装: pip install " + String.join(" ", missingDeps));
            return result;
        }

        BrowserCore._set_operation_status(true);

        try {
            Page page = BrowserCore._ensure_page();

            // 验证wait_until参数
            String[] validWaitOptions = {"load", "domcontentloaded", "networkidle"};
            if (!Arrays.asList(validWaitOptions).contains(waitUntil.toLowerCase())) {
                waitUntil = "load";
            }

            // 保存原始请求的URL
            String originalRequestUrl = url;

            // 导航到URL
            Response response = page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.valueOf(waitUntil.toUpperCase())));

            // 等待页面稳定
            Thread.sleep(1000);

            // 获取页面ID
            String pageId = null;
            for (Map.Entry<String, Page> entry : BrowserCore.get_pages().entrySet()) {
                if (entry.getValue() == page) {
                    pageId = entry.getKey();
                    break;
                }
            }

            // 检测是否发生了重定向
            String currentUrl = page.url();
            if (!currentUrl.equals(originalRequestUrl)) {
                System.out.println("检测到页面重定向: " + originalRequestUrl + " -> " + currentUrl);

                // 判断重定向后的页面是否是登录页面
                boolean isLoginPage = BrowserCore._check_login_required(page);
                if (isLoginPage) {
                    System.out.println("重定向后的页面是登录页面");
                    if (pageId != null) {
                        BrowserCore.set_original_url(pageId, originalRequestUrl);
                        System.out.println("保存原始请求URL: " + originalRequestUrl);
                    }
                }
            } else {
                // 如果没有重定向，也保存原始URL
                if (pageId != null) {
                    BrowserCore.set_original_url(pageId, url);
                }
            }

            // 获取当前页面的域名
            String currentDomain = BrowserUtils.getDomainFromUrl(currentUrl);

            // 检查页面是否需要登录（不再限制只检查配置的域名）
            if (pageId != null && !BrowserCore.get_login_in_progress().getOrDefault(pageId, false)) {
                // 再次检查当前页面是否是登录页面（可能在重定向检测时已经检查过，但为了确保，这里再检查一次）
                if (BrowserCore._check_login_required(page)) {
                    System.out.println("检测到需要登录");
                    BrowserCore.set_login_in_progress(pageId, true);

                    try {
                        // 优先使用域名特定配置进行登录
                        boolean loginSuccess = false;
                        if (LOGIN_DOMAIN_CONFIGS.containsKey(currentDomain)) {
                            System.out.println("使用域名 " + currentDomain + " 的特定配置进行登录");
                            loginSuccess = BrowserLogin.autoLoginJd(page);
                        } else {
                            // 尝试通用登录方法
                            System.out.println("使用通用登录方法");
                            loginSuccess = BrowserLogin.autoLoginWithConfig(page,null,null,null,"JD_ERP_USERNAME","JD_ERP_PASSWORD");
                        }

                        if (loginSuccess) {
                            System.out.println("自动登录成功");
                            Thread.sleep(2000);

                            // 检查是否需要二次认证
                            boolean needs2fa = BrowserLogin.detect2faRequired(page);
                            if (needs2fa) {
                                System.out.println("检测到需要二次认证");
                                // 处理二次认证
                                Map<String, Object> authResult = BrowserLogin.handle2faAuthentication(page, true);
                                System.out.println("二次认证处理结果: " + authResult);
                            }

                            // 检查登录后页面是否自动重定向到了其他页面
                            String loginRedirectUrl = page.url();
                            if (!loginRedirectUrl.equals(currentUrl)) {
                                System.out.println("登录后页面自动重定向到: " + loginRedirectUrl);
                            }

                            // 如果有原始URL，则重新导航到该URL
                            if (BrowserCore.get_original_urls().containsKey(pageId) &&
                                    !BrowserCore.get_original_urls().get(pageId).equals(page.url())) {
                                String originalUrl = BrowserCore.get_original_urls().get(pageId);
                                System.out.println("重新导航到原始URL: " + originalUrl);
                                try {
                                    page.navigate(originalUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD));
                                    Thread.sleep(2000);

                                    // 检查导航后的URL，可能会有进一步的重定向
                                    String finalUrl = page.url();
                                    if (!finalUrl.equals(originalUrl)) {
                                        System.out.println("导航到原始URL后被重定向到: " + finalUrl);
                                    }

                                    // 验证是否成功导航（即使有重定向也可能是成功的）
                                    if (finalUrl.equals(originalUrl) ||
                                            finalUrl.startsWith(originalUrl) ||
                                            !BrowserCore._check_login_required(page)) {
                                        System.out.println("成功访问目标页面: " + finalUrl);
                                        // 清除原始URL
                                        BrowserCore.remove_original_url(pageId);
                                    } else {
                                        System.out.println("导航到原始URL后页面仍然需要登录，可能需要不同的登录方式");

                                        // 检查重定向后的页面是否仍然需要登录
                                        if (BrowserCore._check_login_required(page)) {
                                            System.out.println("尝试对重定向后的页面进行登录...");
                                            String redirectDomain = BrowserUtils.getDomainFromUrl(finalUrl);
                                            boolean secondLoginSuccess = false;

                                            if (LOGIN_DOMAIN_CONFIGS.containsKey(redirectDomain)) {
                                                System.out.println("使用域名 " + redirectDomain + " 的特定配置进行登录");
                                                secondLoginSuccess = BrowserLogin.autoLoginJd(page);
                                            } else {
                                                System.out.println("使用通用登录方法");
                                                secondLoginSuccess = BrowserLogin.autoLoginWithConfig(page,null,null,null,"JD_ERP_USERNAME","JD_ERP_PASSWORD");
                                            }

                                            if (secondLoginSuccess) {
                                                System.out.println("重定向页面登录成功");
                                                // 清除原始URL，因为已经成功登录
                                                if (BrowserCore.get_original_urls().containsKey(pageId)) {
                                                    BrowserCore.remove_original_url(pageId);
                                                }
                                            } else {
                                                System.out.println("重定向页面登录失败");
                                            }
                                        } else {
                                            System.out.println("重定向后的页面不需要登录，继续访问");
                                            // 清除原始URL，因为已经成功访问
                                            if (BrowserCore.get_original_urls().containsKey(pageId)) {
                                                BrowserCore.remove_original_url(pageId);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    System.out.println("导航到原始URL时发生错误: " + e.getMessage() + "，保留原始URL记录");
                                }
                            }
                        } else {
                            System.out.println("自动登录失败");
                        }
                    } finally {
                        // 无论登录成功与否，都标记登录处理完成
                        BrowserCore.set_login_in_progress(pageId, false);
                    }
                }
            }

            // 检查页面是否需要二次认证（无论是否已经登录）
            boolean needs2fa = BrowserLogin.detect2faRequired(page);
            Map<String, Object> authResult = null;
            if (needs2fa) {
                System.out.println("导航后检测到需要二次认证");
                // 处理二次认证
                authResult = BrowserLogin.handle2faAuthentication(page, true);
                System.out.println("二次认证处理结果: " + authResult);

                // 检查二次认证状态
                String authStatus = (String) authResult.getOrDefault("status", "");
                if ("pending_2fa".equals(authStatus)) {
                    System.out.println("需要用户完成二次认证，已保存截图");
                    // 等待一段时间，让用户有机会查看截图
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } else if ("success".equals(authStatus)) {
                    System.out.println("二次认证处理成功");
                    // 等待页面可能的变化
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    System.out.printf("二次认证处理状态: %s%n", authStatus);
                }
            }

            // 获取页面信息
            Map<String, Object> pageInfo = new LinkedHashMap<>();
            if (extractContent) {
                pageInfo = extractPageInfo(page);
            }

            BrowserCore.verifyDataReady();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "success");
            if (response != null) {
                result.put("status_code", response.status());
            }
            result.put("url", url);
            result.put("final_url", page.url());
            result.put("message", "成功导航到 " + url + "，状态码: " + (response != null ? response.status() : "无") + "，数据已准备就绪");

            // 如果提取了页面内容，则添加到结果中
            if (extractContent && !pageInfo.isEmpty()) {
                result.put("page_info", pageInfo);
            }

            // 如果检测到并处理了二次认证，添加到结果中
            if (needs2fa && authResult != null) {
                result.put("two_factor_auth", true);
                String authStatus = (String) authResult.getOrDefault("status", "");
                result.put("two_factor_auth_status", authStatus);
                result.put("two_factor_auth_message", (String) authResult.getOrDefault("message", ""));
                if (authResult.containsKey("screenshot_path")) {
                    result.put("auth_screenshot_path", authResult.get("screenshot_path"));
                }
                if (authResult.containsKey("action_required")) {
                    result.put("action_required", authResult.get("action_required"));
                }
            }

            BrowserCore._set_operation_status(false);
            return result;
        } catch (Exception e) {
            BrowserCore._set_operation_status(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "error");
            result.put("url", url);
            result.put("message", "导航到 " + url + " 时发生错误: " + e.getMessage());
            return result;
        }
    }

    private Map<String, Object> extractPageInfo(Page page) {
        Map<String, Object> pageInfo = new LinkedHashMap<>();
        try {
            pageInfo.put("title", page.title());
            pageInfo.put("url", page.url());

            // 提取页面文本内容
            String pageText = page.evaluate("() => { const mainContent = document.querySelector('main') || document.querySelector('article') || document.querySelector('#content') || document.querySelector('.content') || document.body; return mainContent ? mainContent.innerText : document.body.innerText; }").toString();

            if (pageText.length() > 3000) {
                pageInfo.put("content", pageText.substring(0, 3000) + "...(内容已截断)");
            } else {
                pageInfo.put("content", pageText);
            }

            // 提取页面元数据
            try {
                Map<String, Object> metaData = (Map<String, Object>) page.evaluate("() => { const metadata = {}; const metaTags = document.querySelectorAll('meta'); for (const meta of metaTags) { const name = meta.getAttribute('name') || meta.getAttribute('property'); const content = meta.getAttribute('content'); if (name && content) { metadata[name] = content; } } return metadata; }");
                Map<String, Object> importantMeta = new LinkedHashMap<>();
                String[] importantKeys = {"description", "keywords", "og:title", "og:description"};
                if (metaData instanceof Map) {
                    for (String key : importantKeys) {
                        if (((Map<?, ?>) metaData).containsKey(key)) {
                            importantMeta.put(key, ((Map<?, ?>) metaData).get(key));
                        }
                    }
                }
                pageInfo.put("metadata", importantMeta);
            } catch (Exception e) {
                // 忽略元数据提取错误
            }

            // 提取页面链接
            try {
                Object linksResult = page.evaluate("() => { const mainLinks = []; const links = document.querySelectorAll('a'); let count = 0; for (const link of links) { if (count >= 10) break; const href = link.getAttribute('href'); const text = link.innerText.trim(); if (href && text && href !== '#' && !href.startsWith('javascript:')) { mainLinks.push({ text, href }); count++; } } return mainLinks; }");
                if (linksResult instanceof List) {
                    List<Map<String, Object>> links = new ArrayList<>();
                    for (Object item : (List<?>) linksResult) {
                        if (item instanceof Map) {
                            Map<?, ?> map = (Map<?, ?>) item;
                            Map<String, Object> link = new LinkedHashMap<>();
                            link.put("text", map.get("text"));
                            link.put("href", map.get("href"));
                            links.add(link);
                        }
                    }
                    pageInfo.put("main_links", links);
                }
            } catch (Exception e) {
                // 忽略链接提取错误
            }
        } catch (Exception e) {
            pageInfo.put("content_error", "提取页面内容时发生错误: " + e.getMessage());
        }
        return pageInfo;
    }

    /**
     * 返回浏览器历史记录中的上一页
     */
    @MCPTool(name = "browser_navigate_back", description = "返回上一页")
    public Map<String, Object> browserNavigateBack() {
        // 检查依赖
        List<String> missingDeps = BrowserCore.check_dependencies();
        if (!missingDeps.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "缺少必要的库: " + String.join(", ", missingDeps) + "。请使用pip安装: pip install " + String.join(" ", missingDeps));
            return result;
        }

        BrowserCore._set_operation_status(true);
        try {
            Page page = BrowserCore._ensure_page();
            page.goBack();
            Thread.sleep(1000);
            BrowserCore._verify_data_ready();
            BrowserCore._set_operation_status(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "成功返回上一页，数据已准备就绪");
            return result;
        } catch (Exception e) {
            BrowserCore._set_operation_status(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "返回上一页时发生错误: " + e.getMessage());
            return result;
        }
    }

    @MCPTool(name = "browser_navigate_forward", description = "前进到下一页")
    public Map<String, Object> browserNavigateForward() {
        // 检查依赖
        List<String> missingDeps = BrowserCore.check_dependencies();
        if (!missingDeps.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "缺少必要的库: " + String.join(", ", missingDeps) + "。请使用pip安装: pip install " + String.join(" ", missingDeps));
            return result;
        }

        BrowserCore._set_operation_status(true);
        try {
            Page page = BrowserCore._ensure_page();
            page.goForward();
            Thread.sleep(1000);
            BrowserCore._verify_data_ready();
            BrowserCore._set_operation_status(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "成功前进到下一页，数据已准备就绪");
            return result;
        } catch (Exception e) {
            BrowserCore._set_operation_status(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "前进到下一页时发生错误: " + e.getMessage());
            return result;
        }
    }
}
