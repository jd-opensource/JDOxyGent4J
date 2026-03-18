package com.jd.oxygent.core.oxygent.mcpservers.browser;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Browser automatic login functionality
 * 
 * Provides functionality to automatically detect and handle login forms
 */
public class BrowserLogin {

    // Domain login configuration dictionary, containing selector configurations for different domains
    private static final Map<String, Map<String, String>> LOGIN_DOMAIN_CONFIGS = new LinkedHashMap<>();

    static {
        Map<String, String> jdConfig = new LinkedHashMap<>();
        jdConfig.put("username_selector", "#username");
        jdConfig.put("password_selector", "#password");
        jdConfig.put("submit_selector", "#formsubmitButton");
        LOGIN_DOMAIN_CONFIGS.put("ssa.jd.com", jdConfig);
    }

    /**
     * Use configuration for automatic login
     *
     * Parameters:
     *     - page: Page object
     *     - domain: Domain name, used to get configuration
     *     - username: Username, get from environment variable if null
     *     - password: Password, get from environment variable if null
     *     - envUsernameKey: Key name of username in environment variable
     *     - envPasswordKey: Key name of password in environment variable
     */
    public static boolean autoLoginWithConfig(
            Page page,
            String domain,
            String username,
            String password,
            String envUsernameKey,
            String envPasswordKey) {
        try {
            // If username or password is not provided, get from environment variables
            String loginUsername = username;
            String loginPassword = password;

            if (loginUsername == null || loginUsername.isEmpty()) {
                loginUsername = System.getenv(envUsernameKey);
            }
            if (loginPassword == null || loginPassword.isEmpty()) {
                loginPassword = System.getenv(envPasswordKey);
            }

            if (loginUsername == null || loginUsername.isEmpty() || loginPassword == null || loginPassword.isEmpty()) {
                System.out.println("Username or password not provided, and environment variable " + envUsernameKey + " or " + envPasswordKey + " not found");
                return false;
            }

            // Get domain configuration
            Map<String, String> domainConfig = null;
            if (domain != null && !domain.isEmpty() && LOGIN_DOMAIN_CONFIGS.containsKey(domain)) {
                domainConfig = LOGIN_DOMAIN_CONFIGS.get(domain);
                System.out.println("Using specific configuration for domain " + domain);

                // Use specific configuration
                try {
                    String usernameSelector = domainConfig.get("username_selector");
                    String passwordSelector = domainConfig.get("password_selector");
                    String submitSelector = domainConfig.get("submit_selector");

                    // Find username input box
                    ElementHandle usernameElement = page.querySelector(usernameSelector);
                    if (usernameElement == null) {
                        System.out.println("Username input box not found: " + usernameSelector);
                        // If specific selector is not found, fall back to generic mode
                    } else {
                        // Clear and input username
                        Thread.sleep(1000);
                        usernameElement.fill("");
                        usernameElement.type(loginUsername);

                        // Find password input box
                        ElementHandle passwordElement = page.querySelector(passwordSelector);
                        if (passwordElement == null) {
                            System.out.println("Password input box not found: " + passwordSelector);
                            // If specific selector is not found, fall back to generic mode
                        } else {
                            // Clear and input password
                            Thread.sleep(1000);
                            passwordElement.fill("");
                            passwordElement.type(loginPassword);

                            // Wait a moment to ensure input is complete
                            Thread.sleep(1000);

                            // Find submit button
                            ElementHandle submitButton = page.querySelector(submitSelector);
                            if (submitButton == null) {
                                System.out.println("Submit button not found: " + submitSelector);
                                // If specific selector is not found, fall back to generic mode
                            } else {
                                // Click submit button
                                submitButton.click();
                                // Wait for login to complete
                                Thread.sleep(3000);
                                return true;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Failed to login with specific configuration: " + e.getMessage());
                    // If using specific configuration fails, fall back to generic mode
                }
            }

            // Try different login form patterns
            List<Map<String, String>> loginPatterns = new ArrayList<>();

            // Pattern 1: Standard login form
            Map<String, String> pattern1 = new LinkedHashMap<>();
            pattern1.put("username_selector", "input[name='username'], input[id='username'], #username, .username, [placeholder*='用户名'], [placeholder*='账号'], [placeholder*='邮箱']");
            pattern1.put("password_selector", "input[name='password'], input[type='password'], #password, .password, [placeholder*='密码']");
            pattern1.put("submit_selector", "#formsubmitButton, button[type='submit'], input[type='submit'], .login-btn, #login-btn");
            loginPatterns.add(pattern1);

            // Pattern 2: JD specific login form
            Map<String, String> pattern2 = new LinkedHashMap<>();
            pattern2.put("username_selector", ".itxt[name='username'], .itxt[name='loginname'], .itxt[name='account']");
            pattern2.put("password_selector", ".itxt[name='password'], .itxt[type='password']");
            pattern2.put("submit_selector", "#formsubmitButton, .btn-login, .login-btn");
            loginPatterns.add(pattern2);

            // Pattern 3: Generic email login form
            Map<String, String> pattern3 = new LinkedHashMap<>();
            pattern3.put("username_selector", "input[type='email'], input[name='email']");
            pattern3.put("password_selector", "input[type='password']");
            pattern3.put("submit_selector", "button[type='submit'], input[type='submit'], .submit-btn, #submit");
            loginPatterns.add(pattern3);

            for (Map<String, String> pattern : loginPatterns) {
                try {
                    // Try to find username input box
                    ElementHandle usernameElement = page.querySelector(pattern.get("username_selector"));
                    if (usernameElement == null) {
                        System.out.println("Username input box not found: " + pattern.get("username_selector"));
                        continue;
                    }

                    // Clear and input username
                    usernameElement.fill("");
                    usernameElement.type(loginUsername);

                    // Try to find password input box
                    ElementHandle passwordElement = page.querySelector(pattern.get("password_selector"));
                    if (passwordElement == null) {
                        System.out.println("Password input box not found: " + pattern.get("password_selector"));
                        continue;
                    }

                    // Clear and input password
                    passwordElement.fill("");
                    passwordElement.type(loginPassword);

                    // Wait a moment to ensure input is complete
                    Thread.sleep(1000);

                    // Try to find submit button
                    ElementHandle submitButton = page.querySelector(pattern.get("submit_selector"));
                    if (submitButton == null) {
                        System.out.println("Submit button not found: " + pattern.get("submit_selector"));
                        continue;
                    }

                    submitButton.click();
                    // Wait for login to complete
                    Thread.sleep(3000);
                    return true;
                } catch (Exception e) {
                    System.out.println("Failed to try login pattern: " + e.getMessage());
                    continue;
                }
            }

            return false;
        } catch (Exception e) {
            System.out.println("Automatic login failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Automatic login to JD (compatible with old version, calls _auto_login_with_config)
     */
    public static boolean autoLoginJd(Page page){
        String domainFromUrl = BrowserUtils.getDomainFromUrl(page.url());
        return autoLoginWithConfig(page,domainFromUrl,null,null,"JD_ERP_USERNAME","JD_ERP_PASSWORD");
    }
    /**
     * Detect if page requires two-factor authentication (such as QR code verification)
     */
    public static boolean detect2faRequired(Page page) {
        try {
            // Check if page contains 2FA related elements
            String[] qrSelectors = {
                "img[src*='qrcode']",
                ".qrcode",
                "#qrcode",
                "canvas.qrcode",
                "div[class*='qr']",
                "div[id*='qr']",
                "img[alt*='扫码']",
                "img[alt*='二维码']"
            };

            // Check if QR code elements exist
            for (String selector : qrSelectors) {
                if (page.querySelector(selector) != null) {
                    System.out.println("Detected possible QR code element: " + selector);
                    return true;
                }
            }

            // Check if page text contains 2FA related words
            String content = page.content().toLowerCase();
            String[] authKeywords = {
                "扫码登录", "扫码验证", "二次验证", "两步验证", "双重认证",
                "scan qr code", "scan to login", "two-factor", "2fa",
                "扫描二维码", "扫一扫", "微信扫码", "支付宝扫码", "手机验证"
            };

            for (String keyword : authKeywords) {
                if (content.contains(keyword.toLowerCase())) {
                    System.out.println("Page content contains 2FA keyword: " + keyword);
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            System.out.println("Error occurred while checking 2FA page: " + e.getMessage());
            return false;
        }
    }

    /**
     * Save login page screenshot to cache directory
     */
    public static String saveLoginScreenshot(Page page, String prefix) {
        try {
            // Create directory to save screenshots
            String cacheDir = System.getProperty("user.dir") + File.separator + "cache_dir";
            Files.createDirectories(Paths.get(cacheDir));

            // Create login screenshot subdirectory
            String loginDir = cacheDir + File.separator + "login_screenshots";
            Files.createDirectories(Paths.get(loginDir));

            // Generate unique filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String filename = prefix + "_" + timestamp + "_" + uniqueId + ".png";

            // Complete save path
            String savePath = loginDir + File.separator + filename;

            // Take screenshot
            byte[] screenshotBytes = page.screenshot(new Page.ScreenshotOptions().setFullPage(false));

            // Save screenshot to file
            try (FileOutputStream fos = new FileOutputStream(savePath)) {
                fos.write(screenshotBytes);
            }

            System.out.println("Login page screenshot saved to: " + savePath);
            return savePath;
        } catch (Exception e) {
            System.out.println("Error occurred while saving login page screenshot: " + e.getMessage());
            return null;
        }
    }

    /**
     * Handle two-factor authentication, take screenshot and wait for user to complete authentication
     *     Automatically fill in account and password and login, supports automatic detection of login forms or using specified selectors
     *
     *     If username and password are not provided, they will be obtained from environment variables
     *     If selectors are not provided, it will try to automatically detect login form elements
     */
    public static Map<String, Object> handle2faAuthentication(Page page, boolean waitFor2fa) {
        try {
            System.out.println("Detected need for 2FA, preparing to take screenshot...");

            // Create directory to save screenshots
            String cacheDir = System.getProperty("user.dir") + File.separator + "cache_dir";
            Files.createDirectories(Paths.get(cacheDir));

            // Create 2FA screenshot subdirectory
            String authDir = cacheDir + File.separator + "2fa_auth";
            Files.createDirectories(Paths.get(authDir));

            // Generate unique filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String filename = "2fa_auth_" + timestamp + "_" + uniqueId + ".png";

            // Complete save path
            String savePath = authDir + File.separator + filename;

            // Take screenshot
            byte[] screenshotBytes = page.screenshot(new Page.ScreenshotOptions().setFullPage(false));

            // Save screenshot to file
            try (FileOutputStream fos = new FileOutputStream(savePath)) {
                fos.write(screenshotBytes);
            }

            System.out.println("2FA screenshot saved to: " + savePath);

            Map<String, Object> result = new LinkedHashMap<>();

            if (waitFor2fa) {
                System.out.println("Waiting for user to complete 2FA...");
                System.out.println("Please view screenshot " + savePath + " and complete scan code authentication");

                // Wait for a period of time to give users enough time to view the screenshot
                Thread.sleep(5000);

                // Return result, including screenshot path and status information
                result.put("status", "pending_2fa");
                result.put("message", "2FA required, screenshot saved");
                result.put("screenshot_path", savePath);
                result.put("action_required", "Please view screenshot and complete 2FA, then continue operation");
            } else {
                // If not waiting for 2FA, return result directly
                result.put("status", "success");
                result.put("message", "2FA request handled, but not waiting for completion");
                result.put("screenshot_path", savePath);
            }

            return result;
        } catch (Exception e) {
            System.out.println("Error occurred while handling 2FA: " + e.getMessage());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "error");
            result.put("message", "Error occurred while handling 2FA: " + e.getMessage());
            result.put("error", e.getMessage());
            return result;
        }
    }

    @MCPTool(name = "browser_auto_login", description = "Automatically fill in username and password to login")
    public Map<String, Object> browserAutoLogin(
            @ToolParam(description = "Website URL to login") String url,
            @ToolParam(description = "Username, get from environment variable if empty", defaultValue = "") String username,
            @ToolParam(description = "Password, get from environment variable if empty", defaultValue = "") String password,
            @ToolParam(description = "Wait time after login (seconds)", defaultValue = "3") int waitAfterLogin,
            @ToolParam(description = "Whether to wait for user to complete two-factor authentication", defaultValue = "true") boolean waitFor2fa) {

        BrowserCore.setOperationStatus(true);

        try {
            Page page = BrowserCore.ensurePage();

            // Navigate to login page
            if (url != null && !url.isEmpty()) {
                page.navigate(url);
                Thread.sleep(2000); // Increase wait time to ensure page fully loads and renders
            }

            // Save login page screenshot, regardless of whether second factor authentication is needed
            String loginScreenshotPath = saveLoginScreenshot(page, "login_page");

            // Get login credentials
            String loginUsername = username;
            String loginPassword = password;

            // Remove leading and trailing spaces from username and password
            if (loginUsername != null && !loginUsername.isEmpty() && !"#username".equals(loginUsername)) {
                loginUsername = loginUsername.trim();
            }
            if (loginPassword != null && !loginPassword.isEmpty() && !"#password".equals(loginPassword)) {
                loginPassword = loginPassword.trim();
            }

            // If username or password not provided, get from environment variables
            if (loginUsername == null || loginUsername.isEmpty()) {
                loginUsername = System.getenv("JD_ERP_USERNAME");
            }
            if (loginPassword == null || loginPassword.isEmpty()) {
                loginPassword = System.getenv("JD_ERP_PASSWORD");
            }

            if (loginUsername == null || loginUsername.isEmpty() || loginPassword == null || loginPassword.isEmpty()) {
                BrowserCore.setOperationStatus(false);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "error");

                result.put("message", "Username or password not provided, and environment variable JD_ERP_USERNAME or JD_ERP_PASSWORD not found");
                result.put("login_screenshot_path", loginScreenshotPath);
                return result;
            }

            // Get current page domain
            String currentDomain = BrowserUtils.getDomainFromUrl(page.url());

            // Check if there's specific configuration for this domain
            Map<String, String> domainConfig = LOGIN_DOMAIN_CONFIGS.get(currentDomain);

            // Initialize generic mode flag
            boolean useGenericMode = domainConfig == null;

            // If there's specific configuration for this domain, use specific selectors
            if (domainConfig != null) {

                System.out.println("Detected domain " + currentDomain + ", using specific selectors");
                // Use specific selectors
                String usernameSelector = domainConfig.get("username_selector");
                String passwordSelector = domainConfig.get("password_selector");
                String submitSelector = domainConfig.get("submit_selector");

                try {
                    // Find username input field
                    ElementHandle usernameElement = page.querySelector(usernameSelector);
                    if (usernameElement == null) {
                        System.out.println("Username input box not found: " + usernameSelector);
                        // Fall back to generic mode if specific selector not found
                        useGenericMode = true;
                    } else {
                        // Clear and input username
                        Thread.sleep(1000);
                        usernameElement.fill("");
                        Thread.sleep(1000);
                        usernameElement.type(loginUsername);

                        // Find password input field
                        ElementHandle passwordElement = page.querySelector(passwordSelector);
                        if (passwordElement == null) {
                            System.out.println("Password input box not found: " + passwordSelector);
                            // Fall back to generic mode if specific selector not found
                            useGenericMode = true;
                        } else {
                            // Clear and input password
                            Thread.sleep(1000);
                            passwordElement.fill("");
                            Thread.sleep(1000);
                            passwordElement.type(loginPassword);

                            // Wait a moment to ensure input is complete
                            Thread.sleep(1000);

                            // Find submit button
                            ElementHandle submitButton = page.querySelector(submitSelector);
                            if (submitButton == null) {
                                System.out.println("Submit button not found: " + submitSelector);
                                // Fall back to generic mode if specific selector not found
                                useGenericMode = true;
                            } else {
                                // Click submit button
                                submitButton.click();
                                // Wait for login to complete
                                Thread.sleep(waitAfterLogin * 1000);

                                // Check if second factor authentication is required
                                boolean needs2fa = detect2faRequired(page);
                                if (needs2fa) {
                                    System.out.println("Detected need for secondary authentication");
                                    // Handle second factor authentication
                                    Map<String, Object> authResult = handle2faAuthentication(page, waitFor2fa);

                                    BrowserCore.verifyDataReady();
                                    BrowserCore.setOperationStatus(false);

                                    // Get second factor authentication status
                                    String authStatus = (String) authResult.get("status");

                                    // Build return result
                                    Map<String, Object> result = new LinkedHashMap<>();
                                    result.put("url", page.url());
                                    result.put("title", page.title());
                                    result.put("domain", currentDomain);
                                    result.put("login_username", loginUsername);
                                    result.put("two_factor_auth", true);
                                    result.put("two_factor_auth_status", authStatus);
                                    result.put("screenshot_path", authResult.get("screenshot_path"));
                                    result.put("login_screenshot_path", loginScreenshotPath);

                                    // Set different return messages based on second factor authentication status
                                    if ("pending_2fa".equals(authStatus)) {
                                        result.put("status", "pending_2fa");

                                        result.put("message", "Login successful, but secondary authentication is required");
                                        result.put("action_required", authResult.get("action_required"));
                                    } else if ("success".equals(authStatus)) {
                                        result.put("status", "success");

                                        result.put("message", "Login successful and two-factor authentication has been processed");
                                    } else {
                                        result.put("status", authStatus);
                                        result.put("message", authResult.get("message"));
                                    }

                                    return result;
                                } else {
                                    BrowserCore.verifyDataReady();
                                    BrowserCore.setOperationStatus(false);

                                    Map<String, Object> result = new LinkedHashMap<>();
                                    result.put("status", "success");
                                    result.put("message", "Login successful using specific selectors");
                                    result.put("url", page.url());
                                    result.put("title", page.title());
                                    result.put("domain", currentDomain);
                                    result.put("login_username", loginUsername);
                                    result.put("login_screenshot_path", loginScreenshotPath);
                                    return result;
                                }
                            }
                        }
                    }
                } catch (Exception e) {

                    System.out.println("Failed to login using specific selectors: " + e.getMessage());
                    // Fall back to generic mode if using specific selectors fails
                    useGenericMode = true;
                }
            }

            // Only execute the following code when generic mode is needed
            if (useGenericMode) {

                System.out.println("Attempting login with generic mode...");
                // Use automatic login form detection method
                // Try different login form patterns
                List<Map<String, String>> loginPatterns = new ArrayList<>();

                // Pattern 1: Standard login form
                Map<String, String> pattern1 = new LinkedHashMap<>();
                pattern1.put("username_selector", "input[name='username'], input[id='username'], #username, .username, [placeholder*='用户名'], [placeholder*='账号'], [placeholder*='邮箱']");
                pattern1.put("password_selector", "input[name='password'], input[type='password'], #password, .password, [placeholder*='密码']");
                pattern1.put("submit_selector", "#formsubmitButton, button[type='submit'], input[type='submit'], .login-btn, #login-btn");
                loginPatterns.add(pattern1);

                // Pattern 2: JD-specific login form
                Map<String, String> pattern2 = new LinkedHashMap<>();
                pattern2.put("username_selector", ".itxt[name='username'], .itxt[name='loginname'], .itxt[name='account']");
                pattern2.put("password_selector", ".itxt[name='password'], .itxt[type='password']");
                pattern2.put("submit_selector", "#formsubmitButton, .btn-login, .login-btn");
                loginPatterns.add(pattern2);

                // Pattern 3: Generic email login form
                Map<String, String> pattern3 = new LinkedHashMap<>();
                pattern3.put("username_selector", "input[type='email'], input[name='email']");
                pattern3.put("password_selector", "input[type='password']");
                pattern3.put("submit_selector", "button[type='submit'], input[type='submit'], .submit-btn, #submit");
                loginPatterns.add(pattern3);

                for (int i = 0; i < loginPatterns.size(); i++) {
                    Map<String, String> pattern = loginPatterns.get(i);
                    try {
                        // Print current pattern being tried

                        System.out.println("Trying login pattern: " + (i + 1));

                        // Try to find username input field
                        ElementHandle usernameElement = page.querySelector(pattern.get("username_selector"));
                        if (usernameElement == null) {
                            System.out.println("Username input box not found: " + pattern.get("username_selector"));
                            continue;
                        }

                        // Clear and input username
                        usernameElement.fill("");
                        usernameElement.type(loginUsername);

                        // Try to find password input field
                        ElementHandle passwordElement = page.querySelector(pattern.get("password_selector"));
                        if (passwordElement == null) {
                            System.out.println("Password input box not found: " + pattern.get("password_selector"));
                            continue;
                        }

                        // Clear and input password
                        passwordElement.fill("");
                        passwordElement.type(loginPassword);

                        // Wait a moment to ensure input is complete
                        Thread.sleep(1000);

                        // Try to find submit button
                        ElementHandle submitButton = page.querySelector(pattern.get("submit_selector"));
                        if (submitButton == null) {

                            System.out.println("Submit button not found: " + pattern.get("submit_selector"));
                            continue;
                        }


                        System.out.println("Found complete login form, attempting to log in...");
                        submitButton.click();
                        // Wait for login to complete
                        Thread.sleep(waitAfterLogin * 1000);

                        // Check if second factor authentication is required
                        boolean needs2fa = detect2faRequired(page);
                        if (needs2fa) {
                            System.out.println("Detected need for secondary authentication");
                            // Handle second factor authentication
                            Map<String, Object> authResult = handle2faAuthentication(page, waitFor2fa);

                            BrowserCore.verifyDataReady();
                            BrowserCore.setOperationStatus(false);

                            // Get second factor authentication status
                            String authStatus = (String) authResult.get("status");

                            // Build return result
                            Map<String, Object> result = new LinkedHashMap<>();
                            result.put("url", page.url());
                            result.put("title", page.title());
                            result.put("pattern_used", pattern);
                            result.put("two_factor_auth", true);
                            result.put("two_factor_auth_status", authStatus);
                            result.put("screenshot_path", authResult.get("screenshot_path"));
                            result.put("login_screenshot_path", loginScreenshotPath);

                            // Set different return messages based on second factor authentication status
                            if ("pending_2fa".equals(authStatus)) {
                                result.put("status", "pending_2fa");
                                result.put("message", "Login successful, but secondary authentication is required");
                                result.put("action_required", authResult.get("action_required"));
                            } else if ("success".equals(authStatus)) {
                                result.put("status", "success");
                                result.put("message", "Login successful and two-factor authentication has been processed");
                            } else {
                                result.put("status", authStatus);
                                result.put("message", authResult.get("message"));
                            }

                            return result;
                        } else {
                            BrowserCore.verifyDataReady();
                            BrowserCore.setOperationStatus(false);

                            Map<String, Object> result = new LinkedHashMap<>();
                            result.put("status", "success");
                            result.put("message", "Login successful");
                            result.put("url", page.url());
                            result.put("title", page.title());
                            result.put("pattern_used", pattern);
                            result.put("login_screenshot_path", loginScreenshotPath);
                            return result;
                        }
                    } catch (Exception e) {

                        System.out.println("Failed to try login pattern: " + e.getMessage());
                        continue;
                    }
                }
            }

            // If generic mode was executed but all patterns failed, or if generic mode wasn't executed (specific domain config failed)
            BrowserCore.setOperationStatus(false);

            // Get page HTML structure to help with debugging
            int formsCount = page.querySelectorAll("form").size();
            int inputsCount = page.querySelectorAll("input").size();
            String pageContent = page.content();


            String errorMessage = "Unable to automatically detect login form, please provide specific selectors";
            if (!useGenericMode) {

                errorMessage = "Specific domain configuration processing failed, and generic mode was not enabled";
            }

            Map<String, Object> debugInfo = new LinkedHashMap<>();
            debugInfo.put("url", page.url());
            debugInfo.put("title", page.title());
            debugInfo.put("forms_count", formsCount);
            debugInfo.put("inputs_count", inputsCount);
            debugInfo.put("content_length", pageContent.length());
            debugInfo.put("used_generic_mode", useGenericMode);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "error");
            result.put("message", errorMessage);
            result.put("debug_info", debugInfo);
            result.put("login_screenshot_path", loginScreenshotPath);
            return result;
        } catch (Exception e) {
            BrowserCore.setOperationStatus(false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "error");

            result.put("message", "An error occurred during automatic login: " + e.getMessage());
            return result;
        }
    }
}
