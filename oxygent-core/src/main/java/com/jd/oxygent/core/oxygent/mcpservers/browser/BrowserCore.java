package com.jd.oxygent.core.oxygent.mcpservers.browser;

import com.microsoft.playwright.*;

import java.util.*;

/**
 * Browser core functionality
 * 
 * Provides core functionality for browser instance management, page management, and status management
 */
public class BrowserCore {

    // Global variables for storing browser instance and pages
    private static Playwright _playwright;
    private static Browser _browser;
    private static BrowserContext _context;
    private static Map<String, Page> _pages = new LinkedHashMap<>();  // Dictionary to store pages, key is page ID, value is page object
    private static String _current_page_id;  // ID of the current active page
    private static boolean _data_ready = false;  // Flag indicating if data is ready
    private static boolean _operation_in_progress = false;  // Flag indicating if an operation is in progress
    private static Map<String, Boolean> _login_in_progress = new HashMap<>();  // Dictionary to store whether each page is currently in login operation
    private static Map<String, String> _original_urls = new HashMap<>();  // Dictionary to store original URLs for each page, used for re-navigation after login

    /**
     * Check if necessary dependencies are installed
     */
    public static List<String> check_dependencies() {
        List<String> missing_deps = new ArrayList<>();

        // Check playwright
        try {
            Class.forName("com.microsoft.playwright.Playwright");
        } catch (ClassNotFoundException e) {
            missing_deps.add("playwright");
        }

        return missing_deps;
    }

    /**
     * Ensure browser is started
     */
    public static synchronized void _ensure_browser() {
        if (_browser == null) {
            try {
                System.out.println("Browser instance does not exist, creating new browser instance...");
                _playwright = Playwright.create();
                _browser = _playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
                _context = _browser.newContext();
                System.out.println("Successfully created new browser instance and context");

                // Set global request interceptor to detect redirects to login pages
                _context.route("**/*", route -> route.resume());
                System.out.println("Global request interceptor has been set");
            } catch (Exception e) {
                System.out.println("Error occurred while starting browser: " + e.getMessage());
                throw new RuntimeException("Error occurred while starting browser: " + e.getMessage(), e);
            }
        } else {
            System.out.println("Reusing existing browser instance");
        }
    }

    /**
     * Ensure at least one page is open and return the current page
     */
    public static synchronized Page _ensure_page() {
        _ensure_browser();

        if (_pages.isEmpty() || _current_page_id == null || !_pages.containsKey(_current_page_id)) {
            System.out.println("No available page or invalid current page ID, creating new page...");
            // Ensure _context is initialized
            if (_context == null) {
                System.out.println("Browser context not initialized, reinitializing browser...");
                _ensure_browser();
            }

            // At this point _context should be initialized, but for type checking, we verify again
            if (_context != null) {
                try {
                    Page page = _context.newPage();
                    String page_id = "page_" + (_pages.size() + 1);
                    _pages.put(page_id, page);
                    _current_page_id = page_id;
                    System.out.println("Successfully created new page, ID: " + page_id);

                    // Set up page navigation event listener
                    boolean success = _setup_navigation_handler(page);
                    if (success) {
                        System.out.println("Successfully set up page navigation event listener");
                    } else {
                        System.out.println("Failed to set up page navigation event listener, but page is still usable");
                    }
                } catch (Exception e) {
                    System.out.println("Error occurred while creating new page: " + e.getMessage());
                    throw new RuntimeException("Error occurred while creating new page: " + e.getMessage(), e);
                }
            } else {
                System.out.println("Unable to initialize browser context");
                throw new RuntimeException("Unable to initialize browser context");
            }
        } else {
            System.out.println("Reusing existing page, ID: " + _current_page_id);
        }

        return _pages.get(_current_page_id);
    }

    /**
     * Set operation status
     */
    public static void _set_operation_status(boolean in_progress) {
        _operation_in_progress = in_progress;
        if (in_progress) {
            _data_ready = false;
        }
    }

    /**
     * Verify if data is ready
     */
    public static boolean _verify_data_ready() {
        _data_ready = true;
        return _data_ready;
    }

    /**
     * Check if page requires login
     */
    public static boolean _check_login_required(Page page) {
        try {
            // First check if URL contains login-related keywords
            String current_url = page.url().toLowerCase();
            String[] url_keywords = {
                "login", "signin", "sign-in", "auth", "passport", "account", "login"
            };
            for (String keyword : url_keywords) {
                if (current_url.contains(keyword)) {
                    System.out.println("URL contains login keyword: " + keyword);
                    return true;
                }
            }

            // Check common login elements
            String[] login_selectors = {
                "input[name='username']", "input[name='account']", "input[name='email']",
                "input[name='user']", "input[type='password']", "input[name='password']",
                ".login-form", ".signin-form", ".auth-form", "#username", "#account",
                "#email", "#user", "#password", ".login-btn", ".signin-btn", ".auth-btn",
                "#login-btn", "#signin-btn", "#formsubmitButton", "button[type='submit']"
            };

            for (String selector : login_selectors) {
                if (page.querySelector(selector) != null) {
                    System.out.println("Detected login element: " + selector);
                    return true;
                }
            }

            // Check if page text contains login-related words
            String content = page.content().toLowerCase();
            String[] login_keywords = {
                "登录", "登陆", "login", "sign in", "signin", "sign-in", "用户名",
                "账号", "邮箱", "username", "account", "email", "密码", "password",
                "验证码", "captcha", "verification", "忘记密码", "forgot password",
                "remember me", "记住我", "注册账号", "create account", "sign up", "注册"
            };
            for (String keyword : login_keywords) {
                if (content.contains(keyword)) {
                    System.out.println("Page content contains login keyword: " + keyword);
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            System.out.println("Error occurred while checking login page: " + e.getMessage());
            return false;
        }
    }

    /**
     * Close browser
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
            _login_in_progress.clear();  // Clear login status dictionary
            _original_urls.clear();  // Clear original URL dictionary

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

    // ==================== Helper functions for getting and setting global variables ====================

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
            // Note: This does not close the page, the caller needs to handle page closing themselves
            _pages.remove(page_id);

            // If the closed page is the current page, switch to another page
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
     * Add new page and set as current page
     */
    public static String add_page(Page page, String url) {
        String page_id = "page_" + (_pages.size() + 1);
        _pages.put(page_id, page);
        _current_page_id = page_id;
        return page_id;
    }

    /**
     * Remove specified page
     */
    public static boolean remove_page(String page_id) {
        if (_pages.containsKey(page_id)) {
            // Note: This does not close the page, the caller needs to handle page closing themselves
            _pages.remove(page_id);

            // If the closed page is the current page, switch to another page
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
     * Set up page navigation event listener to detect page redirects to login pages
     */
    private static boolean _setup_navigation_handler(Page page) {
        // Initialize login status for this page
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
            // Add function to check and handle login
            Runnable check_and_handle_login = () -> {
                // This is login handling logic, temporarily left empty
                // Specific login handling logic will be implemented in BrowserLogin class
            };

            // Add page navigation event listener
            page.onFrameNavigated(frame -> {
                // Only handle main frame navigation
                if (frame.equals(page.mainFrame())) {
                    try {
                        Thread.sleep(1000);
                        check_and_handle_login.run();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });

            // Add page load complete event listener
            page.onLoad(page1 -> check_and_handle_login.run());

            // Set request interceptor to monitor redirects
            page.route("**/*", route -> route.resume());

            return true;
        } catch (Exception e) {
            System.out.println("Error occurred while setting up page navigation event listener: " + e.getMessage());
            return false;
        }
    }

    // ==================== Status Management ====================

    public static boolean is_operation_in_progress() {
        return _operation_in_progress;
    }

    public static boolean is_data_ready() {
        return _data_ready;
    }

    // ==================== Login Status Management ====================

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

    // ==================== Getters ====================

    public static Playwright get_playwright() {
        return _playwright;
    }

    public static Browser get_browser() {
        return _browser;
    }

    public static BrowserContext get_context() {
        return _context;
    }

    // ==================== Compatibility with old naming methods (for calls from other classes) ====================

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
