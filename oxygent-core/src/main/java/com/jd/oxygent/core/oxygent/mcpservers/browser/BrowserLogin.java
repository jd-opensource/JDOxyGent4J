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
 * 浏览器自动登录功能
 * 
 * 提供自动检测和处理登录表单的功能
 */
public class BrowserLogin {

    // 域名登录配置字典，包含不同域名对应的选择器配置
    private static final Map<String, Map<String, String>> LOGIN_DOMAIN_CONFIGS = new LinkedHashMap<>();

    static {
        Map<String, String> jdConfig = new LinkedHashMap<>();
        jdConfig.put("username_selector", "#username");
        jdConfig.put("password_selector", "#password");
        jdConfig.put("submit_selector", "#formsubmitButton");
        LOGIN_DOMAIN_CONFIGS.put("ssa.jd.com", jdConfig);
    }

    /**
     *  使用配置自动登录
     *
     *  参数:
     *     - page: 页面对象
     *     - domain: 域名，用于获取配置
     *     - username: 用户名，如果为None则从环境变量获取
     *     - password: 密码，如果为None则从环境变量获取
     *     - env_username_key: 环境变量中用户名的键名
     *     - env_password_key: 环境变量中密码的键名
     */
    public static boolean autoLoginWithConfig(
            Page page,
            String domain,
            String username,
            String password,
            String envUsernameKey,
            String envPasswordKey) {
        try {
            // 如果未提供用户名或密码，则从环境变量获取
            String loginUsername = username;
            String loginPassword = password;

            if (loginUsername == null || loginUsername.isEmpty()) {
                loginUsername = System.getenv(envUsernameKey);
            }
            if (loginPassword == null || loginPassword.isEmpty()) {
                loginPassword = System.getenv(envPasswordKey);
            }

            if (loginUsername == null || loginUsername.isEmpty() || loginPassword == null || loginPassword.isEmpty()) {
                System.out.println("未提供用户名或密码，且未找到环境变量 " + envUsernameKey + " 或 " + envPasswordKey);
                return false;
            }

            // 获取域名配置
            Map<String, String> domainConfig = null;
            if (domain != null && !domain.isEmpty() && LOGIN_DOMAIN_CONFIGS.containsKey(domain)) {
                domainConfig = LOGIN_DOMAIN_CONFIGS.get(domain);
                System.out.println("使用域名 " + domain + " 的特定配置");

                // 使用特定配置
                try {
                    String usernameSelector = domainConfig.get("username_selector");
                    String passwordSelector = domainConfig.get("password_selector");
                    String submitSelector = domainConfig.get("submit_selector");

                    // 查找用户名输入框
                    ElementHandle usernameElement = page.querySelector(usernameSelector);
                    if (usernameElement == null) {
                        System.out.println("未找到用户名输入框: " + usernameSelector);
                        // 如果找不到特定选择器，回退到通用模式
                    } else {
                        // 清除并输入用户名
                        Thread.sleep(1000);
                        usernameElement.fill("");
                        usernameElement.type(loginUsername);

                        // 查找密码输入框
                        ElementHandle passwordElement = page.querySelector(passwordSelector);
                        if (passwordElement == null) {
                            System.out.println("未找到密码输入框: " + passwordSelector);
                            // 如果找不到特定选择器，回退到通用模式
                        } else {
                            // 清除并输入密码
                            Thread.sleep(1000);
                            passwordElement.fill("");
                            passwordElement.type(loginPassword);

                            // 等待一下，确保输入完成
                            Thread.sleep(1000);

                            // 查找提交按钮
                            ElementHandle submitButton = page.querySelector(submitSelector);
                            if (submitButton == null) {
                                System.out.println("未找到提交按钮: " + submitSelector);
                                // 如果找不到特定选择器，回退到通用模式
                            } else {
                                // 点击提交按钮
                                submitButton.click();
                                // 等待登录完成
                                Thread.sleep(3000);
                                return true;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("使用特定配置登录失败: " + e.getMessage());
                    // 如果使用特定配置失败，回退到通用模式
                }
            }

            // 尝试不同的登录表单模式
            List<Map<String, String>> loginPatterns = new ArrayList<>();

            // 模式1: 标准登录表单
            Map<String, String> pattern1 = new LinkedHashMap<>();
            pattern1.put("username_selector", "input[name='username'], input[id='username'], #username, .username, [placeholder*='用户名'], [placeholder*='账号'], [placeholder*='邮箱']");
            pattern1.put("password_selector", "input[name='password'], input[type='password'], #password, .password, [placeholder*='密码']");
            pattern1.put("submit_selector", "#formsubmitButton, button[type='submit'], input[type='submit'], .login-btn, #login-btn");
            loginPatterns.add(pattern1);

            // 模式2: 京东特定登录表单
            Map<String, String> pattern2 = new LinkedHashMap<>();
            pattern2.put("username_selector", ".itxt[name='username'], .itxt[name='loginname'], .itxt[name='account']");
            pattern2.put("password_selector", ".itxt[name='password'], .itxt[type='password']");
            pattern2.put("submit_selector", "#formsubmitButton, .btn-login, .login-btn");
            loginPatterns.add(pattern2);

            // 模式3: 通用邮箱登录表单
            Map<String, String> pattern3 = new LinkedHashMap<>();
            pattern3.put("username_selector", "input[type='email'], input[name='email']");
            pattern3.put("password_selector", "input[type='password']");
            pattern3.put("submit_selector", "button[type='submit'], input[type='submit'], .submit-btn, #submit");
            loginPatterns.add(pattern3);

            for (Map<String, String> pattern : loginPatterns) {
                try {
                    // 尝试查找用户名输入框
                    ElementHandle usernameElement = page.querySelector(pattern.get("username_selector"));
                    if (usernameElement == null) {
                        System.out.println("未找到用户名输入框: " + pattern.get("username_selector"));
                        continue;
                    }

                    // 清除并输入用户名
                    usernameElement.fill("");
                    usernameElement.type(loginUsername);

                    // 尝试查找密码输入框
                    ElementHandle passwordElement = page.querySelector(pattern.get("password_selector"));
                    if (passwordElement == null) {
                        System.out.println("未找到密码输入框: " + pattern.get("password_selector"));
                        continue;
                    }

                    // 清除并输入密码
                    passwordElement.fill("");
                    passwordElement.type(loginPassword);

                    // 等待一下，确保输入完成
                    Thread.sleep(1000);

                    // 尝试查找提交按钮
                    ElementHandle submitButton = page.querySelector(pattern.get("submit_selector"));
                    if (submitButton == null) {
                        System.out.println("未找到提交按钮: " + pattern.get("submit_selector"));
                        continue;
                    }

                    submitButton.click();
                    // 等待登录完成
                    Thread.sleep(3000);
                    return true;
                } catch (Exception e) {
                    System.out.println("尝试登录模式失败: " + e.getMessage());
                    continue;
                }
            }

            return false;
        } catch (Exception e) {
            System.out.println("自动登录失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 自动登录京东（兼容旧版本，调用_auto_login_with_config）
     */
    public static boolean autoLoginJd(Page page){
        String domainFromUrl = BrowserUtils.getDomainFromUrl(page.url());
        return autoLoginWithConfig(page,domainFromUrl,null,null,"JD_ERP_USERNAME","JD_ERP_PASSWORD");
    }
    /**
     * 检测页面是否需要二次认证（如扫码验证）
     */
    public static boolean detect2faRequired(Page page) {
        try {
            // 检查页面是否包含二次认证相关元素
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

            // 检查是否存在二维码元素
            for (String selector : qrSelectors) {
                if (page.querySelector(selector) != null) {
                    System.out.println("检测到可能的二维码元素: " + selector);
                    return true;
                }
            }

            // 检查页面文本中是否包含二次认证相关词语
            String content = page.content().toLowerCase();
            String[] authKeywords = {
                "扫码登录", "扫码验证", "二次验证", "两步验证", "双重认证",
                "scan qr code", "scan to login", "two-factor", "2fa",
                "扫描二维码", "扫一扫", "微信扫码", "支付宝扫码", "手机验证"
            };

            for (String keyword : authKeywords) {
                if (content.contains(keyword.toLowerCase())) {
                    System.out.println("页面内容包含二次认证关键词: " + keyword);
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            System.out.println("检查二次认证页面时发生错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 保存登录页面截图到缓存目录
     */
    public static String saveLoginScreenshot(Page page, String prefix) {
        try {
            // 创建保存截图的目录
            String cacheDir = System.getProperty("user.dir") + File.separator + "cache_dir";
            Files.createDirectories(Paths.get(cacheDir));

            // 创建登录截图子目录
            String loginDir = cacheDir + File.separator + "login_screenshots";
            Files.createDirectories(Paths.get(loginDir));

            // 生成唯一的文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String filename = prefix + "_" + timestamp + "_" + uniqueId + ".png";

            // 完整的保存路径
            String savePath = loginDir + File.separator + filename;

            // 截取截图
            byte[] screenshotBytes = page.screenshot(new Page.ScreenshotOptions().setFullPage(false));

            // 保存截图到文件
            try (FileOutputStream fos = new FileOutputStream(savePath)) {
                fos.write(screenshotBytes);
            }

            System.out.println("登录页面截图已保存到: " + savePath);
            return savePath;
        } catch (Exception e) {
            System.out.println("保存登录页面截图时发生错误: " + e.getMessage());
            return null;
        }
    }

    /**
     * 处理二次认证，截屏并等待用户完成认证
     *     自动填充账号密码并登录，支持自动检测登录表单或使用指定的选择器
     *
     *     如果未提供用户名和密码，将从环境变量中获取
     *     如果未提供选择器，将尝试自动检测登录表单元素
     */
    public static Map<String, Object> handle2faAuthentication(Page page, boolean waitFor2fa) {
        try {
            System.out.println("检测到需要二次认证，准备截屏...");

            // 创建保存截图的目录
            String cacheDir = System.getProperty("user.dir") + File.separator + "cache_dir";
            Files.createDirectories(Paths.get(cacheDir));

            // 创建二次认证截图子目录
            String authDir = cacheDir + File.separator + "2fa_auth";
            Files.createDirectories(Paths.get(authDir));

            // 生成唯一的文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String filename = "2fa_auth_" + timestamp + "_" + uniqueId + ".png";

            // 完整的保存路径
            String savePath = authDir + File.separator + filename;

            // 截取截图
            byte[] screenshotBytes = page.screenshot(new Page.ScreenshotOptions().setFullPage(false));

            // 保存截图到文件
            try (FileOutputStream fos = new FileOutputStream(savePath)) {
                fos.write(screenshotBytes);
            }

            System.out.println("二次认证截图已保存到: " + savePath);

            Map<String, Object> result = new LinkedHashMap<>();

            if (waitFor2fa) {
                System.out.println("等待用户完成二次认证...");
                System.out.println("请查看截图 " + savePath + " 并完成扫码认证");

                // 等待一段时间，给用户足够的时间查看截图
                Thread.sleep(5000);

                // 返回结果，包含截图路径和状态信息
                result.put("status", "pending_2fa");
                result.put("message", "需要二次认证，已保存截图");
                result.put("screenshot_path", savePath);
                result.put("action_required", "请查看截图并完成二次认证，然后继续操作");
            } else {
                // 如果不等待二次认证，直接返回结果
                result.put("status", "success");
                result.put("message", "已处理二次认证请求，但不等待完成");
                result.put("screenshot_path", savePath);
            }

            return result;
        } catch (Exception e) {
            System.out.println("处理二次认证时发生错误: " + e.getMessage());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "error");
            result.put("message", "处理二次认证时发生错误: " + e.getMessage());
            result.put("error", e.getMessage());
            return result;
        }
    }

    @MCPTool(name = "browser_auto_login", description = "自动填充账号密码并登录")
    public Map<String, Object> browserAutoLogin(
            @ToolParam(description = "要登录的网站URL") String url,
            @ToolParam(description = "用户名，如果为空则从环境变量获取", defaultValue = "") String username,
            @ToolParam(description = "密码，如果为空则从环境变量获取", defaultValue = "") String password,
            @ToolParam(description = "登录后等待的时间(秒)", defaultValue = "3") int waitAfterLogin,
            @ToolParam(description = "是否等待用户完成二次认证", defaultValue = "true") boolean waitFor2fa) {

        BrowserCore.setOperationStatus(true);

        try {
            Page page = BrowserCore.ensurePage();

            // 导航到登录页面
            if (url != null && !url.isEmpty()) {
                page.navigate(url);
                Thread.sleep(2000); // 增加等待时间，确保页面完全加载和渲染
            }

            // 保存登录页面截图，无论是否需要二次认证
            String loginScreenshotPath = saveLoginScreenshot(page, "login_page");

            // 获取登录凭据
            String loginUsername = username;
            String loginPassword = password;

            // 去除用户名和密码的前后空格
            if (loginUsername != null && !loginUsername.isEmpty() && !"#username".equals(loginUsername)) {
                loginUsername = loginUsername.trim();
            }
            if (loginPassword != null && !loginPassword.isEmpty() && !"#password".equals(loginPassword)) {
                loginPassword = loginPassword.trim();
            }

            // 如果未提供用户名或密码，则从环境变量获取
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
                result.put("message", "未提供用户名或密码，且未找到环境变量 JD_ERP_USERNAME 或 JD_ERP_PASSWORD");
                result.put("login_screenshot_path", loginScreenshotPath);
                return result;
            }

            // 获取当前页面的域名
            String currentDomain = BrowserUtils.getDomainFromUrl(page.url());

            // 检查是否有该域名的特定配置
            Map<String, String> domainConfig = LOGIN_DOMAIN_CONFIGS.get(currentDomain);

            // 初始化通用模式标志
            boolean useGenericMode = domainConfig == null;

            // 如果有该域名的特定配置，使用特定的选择器
            if (domainConfig != null) {
                System.out.println("检测到" + currentDomain + "域名，使用特定选择器");
                // 使用特定的选择器
                String usernameSelector = domainConfig.get("username_selector");
                String passwordSelector = domainConfig.get("password_selector");
                String submitSelector = domainConfig.get("submit_selector");

                try {
                    // 查找用户名输入框
                    ElementHandle usernameElement = page.querySelector(usernameSelector);
                    if (usernameElement == null) {
                        System.out.println("未找到用户名输入框: " + usernameSelector);
                        // 如果找不到特定选择器，回退到通用模式
                        useGenericMode = true;
                    } else {
                        // 清除并输入用户名
                        Thread.sleep(1000);
                        usernameElement.fill("");
                        Thread.sleep(1000);
                        usernameElement.type(loginUsername);

                        // 查找密码输入框
                        ElementHandle passwordElement = page.querySelector(passwordSelector);
                        if (passwordElement == null) {
                            System.out.println("未找到密码输入框: " + passwordSelector);
                            // 如果找不到特定选择器，回退到通用模式
                            useGenericMode = true;
                        } else {
                            // 清除并输入密码
                            Thread.sleep(1000);
                            passwordElement.fill("");
                            Thread.sleep(1000);
                            passwordElement.type(loginPassword);

                            // 等待一下，确保输入完成
                            Thread.sleep(1000);

                            // 查找提交按钮
                            ElementHandle submitButton = page.querySelector(submitSelector);
                            if (submitButton == null) {
                                System.out.println("未找到提交按钮: " + submitSelector);
                                // 如果找不到特定选择器，回退到通用模式
                                useGenericMode = true;
                            } else {
                                // 点击提交按钮
                                submitButton.click();
                                // 等待登录完成
                                Thread.sleep(waitAfterLogin * 1000);

                                // 检查是否需要二次认证
                                boolean needs2fa = detect2faRequired(page);
                                if (needs2fa) {
                                    System.out.println("检测到需要二次认证");
                                    // 处理二次认证
                                    Map<String, Object> authResult = handle2faAuthentication(page, waitFor2fa);

                                    BrowserCore.verifyDataReady();
                                    BrowserCore.setOperationStatus(false);

                                    // 获取二次认证状态
                                    String authStatus = (String) authResult.get("status");

                                    // 构建返回结果
                                    Map<String, Object> result = new LinkedHashMap<>();
                                    result.put("url", page.url());
                                    result.put("title", page.title());
                                    result.put("domain", currentDomain);
                                    result.put("login_username", loginUsername);
                                    result.put("two_factor_auth", true);
                                    result.put("two_factor_auth_status", authStatus);
                                    result.put("screenshot_path", authResult.get("screenshot_path"));
                                    result.put("login_screenshot_path", loginScreenshotPath);

                                    // 根据二次认证状态设置不同的返回信息
                                    if ("pending_2fa".equals(authStatus)) {
                                        result.put("status", "pending_2fa");
                                        result.put("message", "登录成功，但需要完成二次认证");
                                        result.put("action_required", authResult.get("action_required"));
                                    } else if ("success".equals(authStatus)) {
                                        result.put("status", "success");
                                        result.put("message", "登录成功，并已处理二次认证");
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
                                    result.put("message", "使用特定选择器登录成功");
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
                    System.out.println("使用特定选择器登录失败: " + e.getMessage());
                    // 如果使用特定选择器失败，回退到通用模式
                    useGenericMode = true;
                }
            }

            // 只有在需要使用通用模式时才执行下面的代码
            if (useGenericMode) {
                System.out.println("使用通用模式尝试登录...");
                // 使用自动检测登录表单的方式
                // 尝试不同的登录表单模式
                List<Map<String, String>> loginPatterns = new ArrayList<>();

                // 模式1: 标准登录表单
                Map<String, String> pattern1 = new LinkedHashMap<>();
                pattern1.put("username_selector", "input[name='username'], input[id='username'], #username, .username, [placeholder*='用户名'], [placeholder*='账号'], [placeholder*='邮箱']");
                pattern1.put("password_selector", "input[name='password'], input[type='password'], #password, .password, [placeholder*='密码']");
                pattern1.put("submit_selector", "#formsubmitButton, button[type='submit'], input[type='submit'], .login-btn, #login-btn");
                loginPatterns.add(pattern1);

                // 模式2: 京东特定登录表单
                Map<String, String> pattern2 = new LinkedHashMap<>();
                pattern2.put("username_selector", ".itxt[name='username'], .itxt[name='loginname'], .itxt[name='account']");
                pattern2.put("password_selector", ".itxt[name='password'], .itxt[type='password']");
                pattern2.put("submit_selector", "#formsubmitButton, .btn-login, .login-btn");
                loginPatterns.add(pattern2);

                // 模式3: 通用邮箱登录表单
                Map<String, String> pattern3 = new LinkedHashMap<>();
                pattern3.put("username_selector", "input[type='email'], input[name='email']");
                pattern3.put("password_selector", "input[type='password']");
                pattern3.put("submit_selector", "button[type='submit'], input[type='submit'], .submit-btn, #submit");
                loginPatterns.add(pattern3);

                for (int i = 0; i < loginPatterns.size(); i++) {
                    Map<String, String> pattern = loginPatterns.get(i);
                    try {
                        // 打印当前尝试的模式
                        System.out.println("尝试登录模式: " + (i + 1));

                        // 尝试查找用户名输入框
                        ElementHandle usernameElement = page.querySelector(pattern.get("username_selector"));
                        if (usernameElement == null) {
                            System.out.println("未找到用户名输入框: " + pattern.get("username_selector"));
                            continue;
                        }

                        // 清除并输入用户名
                        usernameElement.fill("");
                        usernameElement.type(loginUsername);

                        // 尝试查找密码输入框
                        ElementHandle passwordElement = page.querySelector(pattern.get("password_selector"));
                        if (passwordElement == null) {
                            System.out.println("未找到密码输入框: " + pattern.get("password_selector"));
                            continue;
                        }

                        // 清除并输入密码
                        passwordElement.fill("");
                        passwordElement.type(loginPassword);

                        // 等待一下，确保输入完成
                        Thread.sleep(1000);

                        // 尝试查找提交按钮
                        ElementHandle submitButton = page.querySelector(pattern.get("submit_selector"));
                        if (submitButton == null) {
                            System.out.println("未找到提交按钮: " + pattern.get("submit_selector"));
                            continue;
                        }

                        System.out.println("找到完整的登录表单，尝试登录...");
                        submitButton.click();
                        // 等待登录完成
                        Thread.sleep(waitAfterLogin * 1000);

                        // 检查是否需要二次认证
                        boolean needs2fa = detect2faRequired(page);
                        if (needs2fa) {
                            System.out.println("检测到需要二次认证");
                            // 处理二次认证
                            Map<String, Object> authResult = handle2faAuthentication(page, waitFor2fa);

                            BrowserCore.verifyDataReady();
                            BrowserCore.setOperationStatus(false);

                            // 获取二次认证状态
                            String authStatus = (String) authResult.get("status");

                            // 构建返回结果
                            Map<String, Object> result = new LinkedHashMap<>();
                            result.put("url", page.url());
                            result.put("title", page.title());
                            result.put("pattern_used", pattern);
                            result.put("two_factor_auth", true);
                            result.put("two_factor_auth_status", authStatus);
                            result.put("screenshot_path", authResult.get("screenshot_path"));
                            result.put("login_screenshot_path", loginScreenshotPath);

                            // 根据二次认证状态设置不同的返回信息
                            if ("pending_2fa".equals(authStatus)) {
                                result.put("status", "pending_2fa");
                                result.put("message", "登录成功，但需要完成二次认证");
                                result.put("action_required", authResult.get("action_required"));
                            } else if ("success".equals(authStatus)) {
                                result.put("status", "success");
                                result.put("message", "登录成功，并已处理二次认证");
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
                            result.put("message", "登录成功");
                            result.put("url", page.url());
                            result.put("title", page.title());
                            result.put("pattern_used", pattern);
                            result.put("login_screenshot_path", loginScreenshotPath);
                            return result;
                        }
                    } catch (Exception e) {
                        System.out.println("尝试登录模式失败: " + e.getMessage());
                        continue;
                    }
                }
            }

            // 如果执行了通用模式但所有模式都失败，或者没有执行通用模式（特定域名配置失败）
            BrowserCore.setOperationStatus(false);

            // 获取页面HTML结构，帮助调试
            int formsCount = page.querySelectorAll("form").size();
            int inputsCount = page.querySelectorAll("input").size();
            String pageContent = page.content();

            String errorMessage = "无法自动检测登录表单，请提供具体的选择器";
            if (!useGenericMode) {
                errorMessage = "特定域名配置处理失败，且未启用通用模式";
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
            result.put("message", "自动登录时发生错误: " + e.getMessage());
            return result;
        }
    }
}
