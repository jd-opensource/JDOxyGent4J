package com.jd.oxygent.core.oxygent.mcpservers.browser;

import com.microsoft.playwright.*;

import java.util.*;

/**
 * 浏览器核心功能
 * 
 * 提供浏览器实例管理、页面管理和状态管理等核心功能
 */
public class BrowserCore {

    // 全局变量，用于存储浏览器实例和页面
    private static Playwright _playwright;
    private static Browser _browser;
    private static BrowserContext _context;
    private static Map<String, Page> _pages = new LinkedHashMap<>();  // 存储页面的字典，键为页面ID，值为页面对象
    private static String _current_page_id;  // 当前活动页面的ID
    private static boolean _data_ready = false;  // 标记数据是否已准备好
    private static boolean _operation_in_progress = false;  // 标记操作是否正在进行中
    private static Map<String, Boolean> _login_in_progress = new HashMap<>();  // 存储每个页面是否正在进行登录操作的字典
    private static Map<String, String> _original_urls = new HashMap<>();  // 存储每个页面的原始URL，用于登录后重新导航

    /**
     * 检查必要的依赖是否已安装
     */
    public static List<String> check_dependencies() {
        List<String> missing_deps = new ArrayList<>();

        // 检查playwright
        try {
            Class.forName("com.microsoft.playwright.Playwright");
        } catch (ClassNotFoundException e) {
            missing_deps.add("playwright");
        }

        return missing_deps;
    }

    /**
     * 确保浏览器已启动
     */
    public static synchronized void _ensure_browser() {
        if (_browser == null) {
            try {
                System.out.println("浏览器实例不存在，创建新的浏览器实例...");
                _playwright = Playwright.create();
                _browser = _playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
                _context = _browser.newContext();
                System.out.println("成功创建新的浏览器实例和上下文");

                // 设置全局请求拦截器，用于检测重定向到登录页面的情况
                _context.route("**/*", route -> route.resume());
                System.out.println("已设置全局请求拦截器");
            } catch (Exception e) {
                System.out.println("启动浏览器时发生错误: " + e.getMessage());
                throw new RuntimeException("启动浏览器时发生错误: " + e.getMessage(), e);
            }
        } else {
            System.out.println("复用现有浏览器实例");
        }
    }

    /**
     * 确保至少有一个页面打开，并返回当前页面
     */
    public static synchronized Page _ensure_page() {
        _ensure_browser();

        if (_pages.isEmpty() || _current_page_id == null || !_pages.containsKey(_current_page_id)) {
            System.out.println("没有可用的页面或当前页面ID无效，创建新页面...");
            // 确保_context已初始化
            if (_context == null) {
                System.out.println("浏览器上下文未初始化，重新初始化浏览器...");
                _ensure_browser();
            }

            // 此时_context应该已经初始化，但为了类型检查，我们再次验证
            if (_context != null) {
                try {
                    Page page = _context.newPage();
                    String page_id = "page_" + (_pages.size() + 1);
                    _pages.put(page_id, page);
                    _current_page_id = page_id;
                    System.out.println("成功创建新页面，ID: " + page_id);

                    // 设置页面导航事件监听器
                    boolean success = _setup_navigation_handler(page);
                    if (success) {
                        System.out.println("成功设置页面导航事件监听器");
                    } else {
                        System.out.println("设置页面导航事件监听器失败，但页面仍可使用");
                    }
                } catch (Exception e) {
                    System.out.println("创建新页面时发生错误: " + e.getMessage());
                    throw new RuntimeException("创建新页面时发生错误: " + e.getMessage(), e);
                }
            } else {
                System.out.println("无法初始化浏览器上下文");
                throw new RuntimeException("无法初始化浏览器上下文");
            }
        } else {
            System.out.println("复用现有页面，ID: " + _current_page_id);
        }

        return _pages.get(_current_page_id);
    }

    /**
     * 设置操作状态
     */
    public static void _set_operation_status(boolean in_progress) {
        _operation_in_progress = in_progress;
        if (in_progress) {
            _data_ready = false;
        }
    }

    /**
     * 验证数据是否已准备好
     */
    public static boolean _verify_data_ready() {
        _data_ready = true;
        return _data_ready;
    }

    /**
     * 检查页面是否需要登录
     */
    public static boolean _check_login_required(Page page) {
        try {
            // 首先检查URL是否包含登录相关关键词
            String current_url = page.url().toLowerCase();
            String[] url_keywords = {
                "login", "signin", "sign-in", "auth", "passport", "账号", "登录"
            };
            for (String keyword : url_keywords) {
                if (current_url.contains(keyword)) {
                    System.out.println("URL中包含登录关键词: " + keyword);
                    return true;
                }
            }

            // 检查常见的登录元素
            String[] login_selectors = {
                "input[name='username']", "input[name='account']", "input[name='email']",
                "input[name='user']", "input[type='password']", "input[name='password']",
                ".login-form", ".signin-form", ".auth-form", "#username", "#account",
                "#email", "#user", "#password", ".login-btn", ".signin-btn", ".auth-btn",
                "#login-btn", "#signin-btn", "#formsubmitButton", "button[type='submit']"
            };

            for (String selector : login_selectors) {
                if (page.querySelector(selector) != null) {
                    System.out.println("检测到登录元素: " + selector);
                    return true;
                }
            }

            // 检查页面文本中是否包含登录相关词语
            String content = page.content().toLowerCase();
            String[] login_keywords = {
                "登录", "登陆", "login", "sign in", "signin", "sign-in", "用户名",
                "账号", "邮箱", "username", "account", "email", "密码", "password",
                "验证码", "captcha", "verification", "忘记密码", "forgot password",
                "remember me", "记住我", "注册账号", "create account", "sign up", "注册"
            };
            for (String keyword : login_keywords) {
                if (content.contains(keyword)) {
                    System.out.println("页面内容包含登录关键词: " + keyword);
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            System.out.println("检查登录页面时发生错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 关闭浏览器
     */
    public static synchronized void _close_browser() {
        if (_browser != null) {
            for (String page_id : new ArrayList<>(_pages.keySet())) {
                Page page = _pages.get(page_id);
                if (page != null) {
                    page.close();
                }
            }

            _pages.clear();
            _current_page_id = null;
            _data_ready = false;
            _operation_in_progress = false;
            _login_in_progress.clear();  // 清理登录状态字典
            _original_urls.clear();  // 清理原始URL字典

            if (_context != null) {
                _context.close();
            }

            _browser.close();
            _browser = null;
            _context = null;
        }

        if (_playwright != null) {
            _playwright.close();
            _playwright = null;
        }
    }

    // ==================== 辅助函数，用于获取和设置全局变量 ====================

    public static Map<String, Page> get_pages() {
        return new LinkedHashMap<>(_pages);
    }

    public static String get_current_page_id() {
        return _current_page_id;
    }

    public static void set_current_page_id(String page_id) {
        _current_page_id = page_id;
    }

    public static void add_page_to_pages(String page_id, Page page) {
        _pages.put(page_id, page);
    }

    public static boolean remove_page_from_pages(String page_id) {
        if (_pages.containsKey(page_id)) {
            // 注意：这里不关闭页面，调用者需要自己处理页面关闭
            _pages.remove(page_id);

            // 如果关闭的是当前页面，则切换到另一个页面
            if (page_id.equals(_current_page_id)) {
                if (!_pages.isEmpty()) {
                    _current_page_id = _pages.keySet().iterator().next();
                } else {
                    _current_page_id = null;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 添加新页面并设置为当前页面
     */
    public static String add_page(Page page, String url) {
        String page_id = "page_" + (_pages.size() + 1);
        _pages.put(page_id, page);
        _current_page_id = page_id;
        return page_id;
    }

    /**
     * 移除指定页面
     */
    public static boolean remove_page(String page_id) {
        if (_pages.containsKey(page_id)) {
            // 注意：这里不关闭页面，调用者需要自己处理页面关闭
            _pages.remove(page_id);

            // 如果关闭的是当前页面，则切换到另一个页面
            if (page_id.equals(_current_page_id)) {
                if (!_pages.isEmpty()) {
                    _current_page_id = _pages.keySet().iterator().next();
                } else {
                    _current_page_id = null;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 设置页面导航事件监听器，用于检测页面跳转到登录页面的情况
     */
    private static boolean _setup_navigation_handler(Page page) {
        // 初始化该页面的登录状态
        String page_id = null;
        for (Map.Entry<String, Page> entry : _pages.entrySet()) {
            if (entry.getValue() == page) {
                page_id = entry.getKey();
                break;
            }
        }
        if (page_id != null) {
            _login_in_progress.put(page_id, false);
        }

        try {
            // 添加检查并处理登录的函数
            Runnable check_and_handle_login = () -> {
                // 这里是登录处理逻辑，暂时留空
                // 在BrowserLogin类中实现具体的登录处理逻辑
            };

            // 添加页面导航事件监听器
            page.onFrameNavigated(frame -> {
                // 只处理主框架的导航
                if (frame.equals(page.mainFrame())) {
                    try {
                        Thread.sleep(1000);
                        check_and_handle_login.run();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });

            // 添加页面加载完成事件监听器
            page.onLoad(page1 -> check_and_handle_login.run());

            // 设置请求拦截器，用于监控重定向
            page.route("**/*", route -> route.resume());

            return true;
        } catch (Exception e) {
            System.out.println("设置页面导航事件监听器时发生错误: " + e.getMessage());
            return false;
        }
    }

    // ==================== 状态管理 ====================

    public static boolean is_operation_in_progress() {
        return _operation_in_progress;
    }

    public static boolean is_data_ready() {
        return _data_ready;
    }

    // ==================== 登录状态管理 ====================

    public static Map<String, Boolean> get_login_in_progress() {
        return new HashMap<>(_login_in_progress);
    }

    public static void set_login_in_progress(String page_id, boolean in_progress) {
        _login_in_progress.put(page_id, in_progress);
    }

    public static Map<String, String> get_original_urls() {
        return new HashMap<>(_original_urls);
    }

    public static void set_original_url(String page_id, String url) {
        _original_urls.put(page_id, url);
    }

    public static void remove_original_url(String page_id) {
        _original_urls.remove(page_id);
    }

    // ==================== 获取器 ====================

    public static Playwright get_playwright() {
        return _playwright;
    }

    public static Browser get_browser() {
        return _browser;
    }

    public static BrowserContext get_context() {
        return _context;
    }

    // ==================== 兼容旧命名的方法（用于其他类的调用）====================

    public static void ensureBrowser() {
        _ensure_browser();
    }

    public static Page ensurePage() {
        return _ensure_page();
    }

    public static void setOperationStatus(boolean inProgress) {
        _set_operation_status(inProgress);
    }

    public static boolean verifyDataReady() {
        return _verify_data_ready();
    }

    public static boolean checkLoginRequired(Page page) {
        return _check_login_required(page);
    }

    public static void closeBrowser() {
        _close_browser();
    }

    public static Map<String, Page> getPages() {
        return get_pages();
    }

    public static String getCurrentPageId() {
        return get_current_page_id();
    }

    public static void setCurrentPageId(String pageId) {
        set_current_page_id(pageId);
    }

    public static void addPageToPages(String pageId, Page page) {
        add_page_to_pages(pageId, page);
    }

    public static boolean removePageFromPages(String pageId) {
        return remove_page_from_pages(pageId);
    }

    public static String addPage(Page page, String url) {
        return add_page(page, url);
    }

    public static boolean removePage(String pageId) {
        return remove_page(pageId);
    }

    public static boolean isOperationInProgress() {
        return is_operation_in_progress();
    }

    public static boolean isDataReady() {
        return is_data_ready();
    }

    public static Map<String, Boolean> getLoginInProgress() {
        return get_login_in_progress();
    }

    public static void setLoginInProgress(String pageId, boolean inProgress) {
        set_login_in_progress(pageId, inProgress);
    }

    public static Map<String, String> getOriginalUrls() {
        return get_original_urls();
    }

    public static void setOriginalUrl(String pageId, String url) {
        set_original_url(pageId, url);
    }

    public static void removeOriginalUrl(String pageId) {
        remove_original_url(pageId);
    }

    public static Playwright getPlaywright() {
        return get_playwright();
    }

    public static Browser getBrowser() {
        return get_browser();
    }

    public static BrowserContext getContext() {
        return get_context();
    }
}
