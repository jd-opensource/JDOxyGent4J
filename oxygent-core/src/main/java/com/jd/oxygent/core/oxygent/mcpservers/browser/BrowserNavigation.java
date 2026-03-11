package com.jd.oxygent.core.oxygent.mcpservers.browser;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitUntilState;
import java.util.*;

/**
 * Browser navigation functionality
 *
 * Provides functionality for page navigation, forward and backward navigation
 */
public class BrowserNavigation {

    // Domain login configuration dictionary, containing selector configurations for different domains
    private static final Map<String, Map<String, String>> LOGIN_DOMAIN_CONFIGS = new LinkedHashMap<>();

    static {
        Map<String, String> jdConfig = new LinkedHashMap<>();
        jdConfig.put("username_selector", "#username");
        jdConfig.put("password_selector", "#password");
        jdConfig.put("submit_selector", "#formsubmitButton");
        LOGIN_DOMAIN_CONFIGS.put("ssa.jd.com", jdConfig);
    }

    @MCPTool(name = "browser_navigate", description = "Navigate to specified URL and get page content")
    public Map<String, Object> browserNavigate(
            @ToolParam(description = "Web page URL to navigate to") String url,
            @ToolParam(description = "Condition to wait for page load, options: load, domcontentloaded, networkidle", defaultValue = "load") String waitUntil,
            @ToolParam(description = "Whether to extract page content", defaultValue = "true") boolean extractContent) {

        // Check dependencies
        List<String> missingDeps = BrowserCore.check_dependencies();
        if (!missingDeps.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "Missing required libraries: " + String.join(", ", missingDeps) + ". Please install using maven: install " + String.join(" ", missingDeps));
            return result;
        }

        BrowserCore._set_operation_status(true);

        try {
            Page page = BrowserCore._ensure_page();

            // Validate wait_until parameter
            String[] validWaitOptions = {"load", "domcontentloaded", "networkidle"};
            if (!Arrays.asList(validWaitOptions).contains(waitUntil.toLowerCase())) {
                waitUntil = "load";
            }

            // Save original requested URL
            String originalRequestUrl = url;

            // Navigate to URL
            Response response = page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.valueOf(waitUntil.toUpperCase())));

            // Wait for page to stabilize
            Thread.sleep(1000);

            // Get page ID
            String pageId = null;
            for (Map.Entry<String, Page> entry : BrowserCore.get_pages().entrySet()) {
                if (entry.getValue() == page) {
                    pageId = entry.getKey();
                    break;
                }
            }

            // Detect if redirect occurred
            String currentUrl = page.url();
            if (!currentUrl.equals(originalRequestUrl)) {
                System.out.println("Detected page redirect: " + originalRequestUrl + " -> " + currentUrl);

                // Determine if redirected page is a login page
                boolean isLoginPage = BrowserCore._check_login_required(page);
                if (isLoginPage) {
                    System.out.println("Redirected page is a login page");
                    if (pageId != null) {
                        BrowserCore.set_original_url(pageId, originalRequestUrl);
                        System.out.println("Saved original requested URL: " + originalRequestUrl);
                    }
                }
            } else {
                // If no redirect, also save original URL
                if (pageId != null) {
                    BrowserCore.set_original_url(pageId, url);
                }
            }

            // Get current page domain
            String currentDomain = BrowserUtils.getDomainFromUrl(currentUrl);

            // Check if page requires login (no longer limited to only checking configured domains)
            if (pageId != null && !BrowserCore.get_login_in_progress().getOrDefault(pageId, false)) {
                // Check again if current page is a login page (may have been checked during redirect detection, but check again here to be sure)
                if (BrowserCore._check_login_required(page)) {
                    System.out.println("Detected need for login");
                    BrowserCore.set_login_in_progress(pageId, true);

                    try {
                        // Prioritize using domain-specific configuration for login
                        boolean loginSuccess = false;
                        if (LOGIN_DOMAIN_CONFIGS.containsKey(currentDomain)) {
                            System.out.println("Using specific configuration for domain " + currentDomain + " for login");
                            loginSuccess = BrowserLogin.autoLoginJd(page);
                        } else {
                            // Try generic login method
                            System.out.println("Using generic login method");
                            loginSuccess = BrowserLogin.autoLoginWithConfig(page,null,null,null,"JD_ERP_USERNAME","JD_ERP_PASSWORD");
                        }

                        if (loginSuccess) {
                            System.out.println("Automatic login successful");
                            Thread.sleep(2000);

                            // Check if 2FA is required
                            boolean needs2fa = BrowserLogin.detect2faRequired(page);
                            if (needs2fa) {
                                System.out.println("Detected need for 2FA");
                                // Handle 2FA
                                Map<String, Object> authResult = BrowserLogin.handle2faAuthentication(page, true);
                                System.out.println("2FA handling result: " + authResult);
                            }

                            // Check if page automatically redirected to another page after login
                            String loginRedirectUrl = page.url();
                            if (!loginRedirectUrl.equals(currentUrl)) {
                                System.out.println("Page automatically redirected to: " + loginRedirectUrl);
                            }

                            // If there is an original URL, re-navigate to that URL
                            if (BrowserCore.get_original_urls().containsKey(pageId) &&
                                    !BrowserCore.get_original_urls().get(pageId).equals(page.url())) {
                                String originalUrl = BrowserCore.get_original_urls().get(pageId);
                                System.out.println("Re-navigating to original URL: " + originalUrl);
                                try {
                                    page.navigate(originalUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD));
                                    Thread.sleep(2000);

                                    // Check URL after navigation, there may be further redirects
                                    String finalUrl = page.url();
                                    if (!finalUrl.equals(originalUrl)) {
                                        System.out.println("Redirected to: " + finalUrl + " after navigating to original URL");
                                    }

                                    // Verify if navigation was successful (even with redirects it may be successful)
                                    if (finalUrl.equals(originalUrl) ||
                                            finalUrl.startsWith(originalUrl) ||
                                            !BrowserCore._check_login_required(page)) {
                                        System.out.println("Successfully accessed target page: " + finalUrl);
                                        // Clear original URL
                                        BrowserCore.remove_original_url(pageId);
                                    } else {
                                        System.out.println("Page still requires login after navigating to original URL, may need different login method");

                                        // Check if redirected page still requires login
                                        if (BrowserCore._check_login_required(page)) {
                                            System.out.println("Trying to login to redirected page...");
                                            String redirectDomain = BrowserUtils.getDomainFromUrl(finalUrl);
                                            boolean secondLoginSuccess = false;

                                            if (LOGIN_DOMAIN_CONFIGS.containsKey(redirectDomain)) {
                                                System.out.println("Using specific configuration for domain " + redirectDomain + " for login");
                                                secondLoginSuccess = BrowserLogin.autoLoginJd(page);
                                            } else {
                                                System.out.println("Using generic login method");
                                                secondLoginSuccess = BrowserLogin.autoLoginWithConfig(page,null,null,null,"JD_ERP_USERNAME","JD_ERP_PASSWORD");
                                            }

                                            if (secondLoginSuccess) {
                                                System.out.println("Redirected page login successful");
                                                // Clear original URL, since login was successful
                                                if (BrowserCore.get_original_urls().containsKey(pageId)) {
                                                    BrowserCore.remove_original_url(pageId);
                                                }
                                            } else {
                                                System.out.println("Redirected page login failed");
                                            }
                                        } else {
                                            System.out.println("Redirected page does not require login, continue access");
                                            // Clear original URL, since access was successful
                                            if (BrowserCore.get_original_urls().containsKey(pageId)) {
                                                BrowserCore.remove_original_url(pageId);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    System.out.println("Error occurred while navigating to original URL: " + e.getMessage() + ", keeping original URL record");
                                }
                            }
                        } else {
                            System.out.println("Automatic login failed");
                        }
                    } finally {
                        // Whether login is successful or not, mark login handling as complete
                        BrowserCore.set_login_in_progress(pageId, false);
                    }
                }
            }

            // Check if page requires 2FA (whether already logged in or not)
            boolean needs2fa = BrowserLogin.detect2faRequired(page);
            Map<String, Object> authResult = null;
            if (needs2fa) {
                System.out.println("Detected need for 2FA after navigation");
                // Handle 2FA
                authResult = BrowserLogin.handle2faAuthentication(page, true);
                System.out.println("2FA handling result: " + authResult);

                // Check 2FA status
                String authStatus = (String) authResult.getOrDefault("status", "");
                if ("pending_2fa".equals(authStatus)) {
                    System.out.println("User needs to complete 2FA, screenshot saved");
                    // Wait for a period of time to give users a chance to view the screenshot
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } else if ("success".equals(authStatus)) {
                    System.out.println("2FA handling successful");
                    // Wait for possible page changes
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    System.out.printf("2FA handling status: %s%n", authStatus);
                }
            }

            // Get page information
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
            result.put("message", "Successfully navigated to " + url + ", status code: " + (response != null ? response.status() : "none") + ", data is ready");

            // If page content was extracted, add it to the result
            if (extractContent && !pageInfo.isEmpty()) {
                result.put("page_info", pageInfo);
            }

            // If 2FA was detected and handled, add it to the result
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
            result.put("message", "Error occurred while navigating to " + url + ": " + e.getMessage());
            return result;
        }
    }

    private Map<String, Object> extractPageInfo(Page page) {
        Map<String, Object> pageInfo = new LinkedHashMap<>();
        try {
            pageInfo.put("title", page.title());
            pageInfo.put("url", page.url());

            // Extract page text content
                String pageText = page.evaluate("() => { const mainContent = document.querySelector('main') || document.querySelector('article') || document.querySelector('#content') || document.querySelector('.content') || document.body; return mainContent ? mainContent.innerText : document.body.innerText; }").toString();

                if (pageText.length() > 3000) {
                    pageInfo.put("content", pageText.substring(0, 3000) + "...(content truncated)");
                } else {
                    pageInfo.put("content", pageText);
                }

            // Extract page metadata
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
                // Ignore metadata extraction error
            }

            // Extract page links
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
                // Ignore link extraction error
            }
        } catch (Exception e) {
            pageInfo.put("content_error", "Error occurred while extracting page content: " + e.getMessage());
        }
        return pageInfo;
    }

    /**
     * Go back to the previous page in browser history
     */
    @MCPTool(name = "browser_navigate_back", description = "Go back to previous page")
    public Map<String, Object> browserNavigateBack() {
        // Check dependencies
        List<String> missingDeps = BrowserCore.check_dependencies();
        if (!missingDeps.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "Missing required libraries: " + String.join(", ", missingDeps) + ". Please install using maven: install " + String.join(" ", missingDeps));
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
            result.put("message", "Successfully returned to previous page, data is ready");
            return result;
        } catch (Exception e) {
            BrowserCore._set_operation_status(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "Error occurred while returning to previous page: " + e.getMessage());
            return result;
        }
    }

    @MCPTool(name = "browser_navigate_forward", description = "Navigate forward to next page")
    public Map<String, Object> browserNavigateForward() {
        // Check dependencies
        List<String> missingDeps = BrowserCore.check_dependencies();
        if (!missingDeps.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "Missing required libraries: " + String.join(", ", missingDeps) + ". Please install using maven: install " + String.join(" ", missingDeps));
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
            result.put("message", "Successfully navigated forward to next page, data is ready");
            return result;
        } catch (Exception e) {
            BrowserCore._set_operation_status(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "Error occurred while navigating forward to next page: " + e.getMessage());
            return result;
        }
    }
}
