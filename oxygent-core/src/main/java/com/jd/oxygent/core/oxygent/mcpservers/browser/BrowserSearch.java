package com.jd.oxygent.core.oxygent.mcpservers.browser;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 浏览器搜索功能
 *
 * 提供网络搜索功能，支持Google、Bing和百度搜索引擎
 */
public class BrowserSearch {

    @MCPTool(name = "browser_search", description = "执行网络搜索并返回搜索结果及页面内容")
    public Object browserSearch(
            @ToolParam(description = "搜索查询") String query,
            @ToolParam(description = "搜索引擎，支持google、bing或baidu", defaultValue = "bing") String searchEngine,
            @ToolParam(description = "返回的搜索结果数量，最大10个", defaultValue = "5") int numResults,
            @ToolParam(description = "搜索操作的超时时间(秒)", defaultValue = "30") int timeout,
            @ToolParam(description = "是否提取页面主要内容，用于二次确认", defaultValue = "true") boolean extractPageContent) {

        // 检查依赖
        List<String> missingDeps = BrowserCore.check_dependencies();
        if (!missingDeps.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "缺少必要的库: " + String.join(", ", missingDeps) + "。请使用pip安装: pip install " + String.join(" ", missingDeps));
            return result;
        }

        // 限制结果数量在合理范围内
        if (numResults < 1) numResults = 1;
        if (numResults > 10) numResults = 10;

        // 初始化结果变量
        List<Map<String, Object>> results = new ArrayList<>();
        boolean partialResults = false;
        String errorMessage = null;

        BrowserCore._set_operation_status(true);

        try {
            // 确保浏览器已启动
            Page page = BrowserCore._ensure_page();

            // 编码搜索查询
            String encodedQuery = URLEncoder.encode(query, "UTF-8");

            // 根据选择的搜索引擎构建URL
            Map<String, String> searchUrls = new LinkedHashMap<>();
            searchUrls.put("google", "https://www.google.com/search?q=" + encodedQuery);
            searchUrls.put("bing", "https://www.bing.com/search?q=" + encodedQuery);
            searchUrls.put("baidu", "https://www.baidu.com/s?wd=" + encodedQuery);

            // 获取搜索URL
            String searchUrl = searchUrls.getOrDefault(searchEngine.toLowerCase(), searchUrls.get("bing"));

            // 使用超时机制导航到搜索页面
            try {
                page.navigate(searchUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
                Thread.sleep(2000);
            } catch (Exception e) {
            }
            // 等待页面加载完成，但设置超时
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 根据不同的搜索引擎提取搜索结果，使用超时机制
            String engine = searchEngine.toLowerCase();
            boolean extractionSuccess = false;

            try {
                if ("google".equals(engine)) {
                    // 提取Google搜索结果
                    String script = "(numResults) => { const searchResults = []; const resultElements = document.querySelectorAll('div[data-sokoban-container]'); for (let i = 0; i < resultElements.length && searchResults.length < numResults; i++) { const element = resultElements[i]; const titleElement = element.querySelector('h3'); if (!titleElement) continue; const linkElement = element.querySelector('a'); if (!linkElement) continue; const title = titleElement.innerText.trim(); const link = linkElement.href; let snippet = ''; const snippetElement = element.querySelector('div[style*=\"webkit-line-clamp\"]') || element.querySelector('div[data-content-feature=\"1\"]'); if (snippetElement) { snippet = snippetElement.innerText.trim(); } if (title && link) { searchResults.push({ title, link, snippet }); } } return searchResults; }";
                    Object evalResult = page.evaluate(script, numResults);
                    if (evalResult instanceof List) {
                        for (Object item : (List<?>) evalResult) {
                            if (item instanceof Map) {
                                Map<?, ?> map = (Map<?, ?>) item;
                                Map<String, Object> resultItem = new LinkedHashMap<>();
                                resultItem.put("title", map.get("title"));
                                resultItem.put("link", map.get("link"));
                                resultItem.put("snippet", map.get("snippet"));
                                results.add(resultItem);
                            }
                        }
                    }
                    extractionSuccess = true;
                } else if ("bing".equals(engine)) {
                    // 提取Bing搜索结果
                    String script = "(numResults) => { const searchResults = []; const resultElements = document.querySelectorAll('#b_results > li.b_algo'); for (let i = 0; i < resultElements.length && searchResults.length < numResults; i++) { const element = resultElements[i]; const titleElement = element.querySelector('h2 a'); if (!titleElement) continue; const title = titleElement.innerText.trim(); const link = titleElement.href; let snippet = ''; const snippetElement = element.querySelector('.b_caption p'); if (snippetElement) { snippet = snippetElement.innerText.trim(); } if (title && link) { searchResults.push({ title, link, snippet }); } } return searchResults; }";
                    Object evalResult = page.evaluate(script, numResults);
                    if (evalResult instanceof List) {
                        for (Object item : (List<?>) evalResult) {
                            if (item instanceof Map) {
                                Map<?, ?> map = (Map<?, ?>) item;
                                Map<String, Object> resultItem = new LinkedHashMap<>();
                                resultItem.put("title", map.get("title"));
                                resultItem.put("link", map.get("link"));
                                resultItem.put("snippet", map.get("snippet"));
                                results.add(resultItem);
                            }
                        }
                    }
                    extractionSuccess = true;
                } else if ("baidu".equals(engine)) {
                    // 提取百度搜索结果
                    String script = "(numResults) => { const searchResults = []; const resultElements = document.querySelectorAll('.result'); for (let i = 0; i < resultElements.length && searchResults.length < numResults; i++) { const element = resultElements[i]; const titleElement = element.querySelector('h3 a'); if (!titleElement) continue; const title = titleElement.innerText.trim(); const link = titleElement.href; let snippet = ''; const snippetElement = element.querySelector('.c-abstract'); if (snippetElement) { snippet = snippetElement.innerText.trim(); } if (title && link) { searchResults.push({ title, link, snippet }); } } return searchResults; }";
                    Object evalResult = page.evaluate(script, numResults);
                    if (evalResult instanceof List) {
                        for (Object item : (List<?>) evalResult) {
                            if (item instanceof Map) {
                                Map<?, ?> map = (Map<?, ?>) item;
                                Map<String, Object> resultItem = new LinkedHashMap<>();
                                resultItem.put("title", map.get("title"));
                                resultItem.put("link", map.get("link"));
                                resultItem.put("snippet", map.get("snippet"));
                                results.add(resultItem);
                            }
                        }
                    }
                    extractionSuccess = true;
                }
            } catch (Exception e) {
                // 如果提取结果超时，记录错误并尝试使用通用提取方法
                errorMessage = "提取搜索结果超时(>" + (timeout / 2) + "秒)，尝试使用通用提取方法";
                partialResults = true;
            }

            // 如果没有找到结果或者提取超时，尝试通用提取方法
            if (!extractionSuccess || results.isEmpty()) {
                try {
                    String script = "(numResults) => { const searchResults = []; const allLinks = Array.from(document.querySelectorAll('a')); const resultLinks = allLinks.filter(link => { if (!link.href) return false; if (link.href.includes('search?') || link.href.includes('javascript:') || link.href.includes('#')) return false; if (!link.innerText.trim()) return false; if (link.innerText.trim().length < 15) return false; return true; }); for (let i = 0; i < resultLinks.length && searchResults.length < numResults; i++) { const link = resultLinks[i]; const title = link.innerText.trim(); const url = link.href; let snippet = ''; const parent = link.parentElement; if (parent) { const siblings = Array.from(parent.childNodes); for (const sibling of siblings) { if (sibling !== link && sibling.textContent) { const text = sibling.textContent.trim(); if (text.length > 20) { snippet = text; break; } } } } searchResults.push({ title, link: url, snippet }); } return searchResults; }";
                    Object evalResult = page.evaluate(script, numResults);
                    if (evalResult instanceof List) {
                        for (Object item : (List<?>) evalResult) {
                            if (item instanceof Map) {
                                Map<?, ?> map = (Map<?, ?>) item;
                                Map<String, Object> resultItem = new LinkedHashMap<>();
                                resultItem.put("title", map.get("title"));
                                resultItem.put("link", map.get("link"));
                                resultItem.put("snippet", map.get("snippet"));
                                results.add(resultItem);
                            }
                        }
                    }
                } catch (Exception e) {
                    if (errorMessage != null) {
                        errorMessage += "，通用提取方法也超时";
                    } else {
                        errorMessage = "通用提取方法超时(>" + (timeout / 3) + "秒)";
                    }
                }
            }

            // 构建搜索结果摘要
            StringBuilder summary = new StringBuilder();
            summary.append("搜索查询: \"").append(query).append("\"\n");
            summary.append("搜索引擎: ").append(searchEngine).append("\n");
            if (errorMessage != null) {
                summary.append("警告: ").append(errorMessage).append("\n");
            }
            if (partialResults) {
                summary.append("注意: 由于超时，可能只返回部分结果\n");
            }
            summary.append("找到 ").append(results.size()).append(" 个结果\n\n");

            for (int i = 0; i < results.size(); i++) {
                Map<String, Object> result = results.get(i);
                summary.append(i + 1).append(". ").append(result.getOrDefault("title", "无标题")).append("\n");
                summary.append("   链接: ").append(result.getOrDefault("link", "无链接")).append("\n");
                String snippet = (String) result.getOrDefault("snippet", "");
                if (!snippet.isEmpty()) {
                    if (snippet.length() > 100) {
                        snippet = snippet.substring(0, 100) + "...";
                    }
                    summary.append("   摘要: ").append(snippet).append("\n");
                }
                summary.append("\n");
            }

            // 提取页面主要内容（如果需要）
            Map<String, Object> pageContent = new LinkedHashMap<>();
            if (extractPageContent) {
                try {
                    // 获取页面标题
                    pageContent.put("title", page.title());

                    // 获取页面URL
                    pageContent.put("url", page.url());

                    // 提取页面主要内容
                    String pageText = page.evaluate("() => { const mainContent = document.querySelector('main') || document.querySelector('article') || document.querySelector('#content') || document.querySelector('.content') || document.body; return mainContent ? mainContent.innerText : document.body.innerText; }").toString();

                    // 如果文本太长，截取前3000个字符
                    if (pageText.length() > 3000) {
                        pageContent.put("content", pageText.substring(0, 3000) + "...(内容已截断)");
                    } else {
                        pageContent.put("content", pageText);
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
                        pageContent.put("metadata", importantMeta);
                    } catch (Exception e) {
                    }
                } catch (Exception contentError) {
                    pageContent.put("content_error", "提取页面内容时发生错误: " + contentError.getMessage());
                }
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("query", query);
            response.put("search_engine", searchEngine);
            response.put("results", results);
            response.put("summary", summary.toString());

            if (extractPageContent && !pageContent.isEmpty()) {
                response.put("page_content", pageContent);
            }

            if (errorMessage != null) {
                response.put("error", errorMessage);
                response.put("partial_results", partialResults);
            }

            BrowserCore._verify_data_ready();
            BrowserCore._set_operation_status(false);
            return response;
        } catch (Exception e) {
            // 捕获所有其他异常
            String errorMsg = "执行网络搜索时发生错误: " + e.getMessage();

            // 如果有部分结果，尝试返回
            if (!results.isEmpty() && results.size() > 0) {
                StringBuilder errorSummary = new StringBuilder();
                errorSummary.append("搜索查询: \"").append(query).append("\"\n");
                errorSummary.append("搜索引擎: ").append(searchEngine).append("\n");
                errorSummary.append("警告: ").append(errorMsg).append("，返回部分结果\n");
                errorSummary.append("找到 ").append(results.size()).append(" 个结果\n\n");

                for (int i = 0; i < results.size(); i++) {
                    Map<String, Object> result = results.get(i);
                    errorSummary.append(i + 1).append(". ").append(result.getOrDefault("title", "无标题")).append("\n");
                    errorSummary.append("   链接: ").append(result.getOrDefault("link", "无链接")).append("\n");
                    String snippet = (String) result.getOrDefault("snippet", "");
                    if (!snippet.isEmpty()) {
                        if (snippet.length() > 100) {
                            snippet = snippet.substring(0, 100) + "...";
                        }
                        errorSummary.append("   摘要: ").append(snippet).append("\n");
                    }
                    errorSummary.append("\n");
                }

                Map<String, Object> errorResult = new LinkedHashMap<>();
                errorResult.put("query", query);
                errorResult.put("search_engine", searchEngine);
                errorResult.put("results", results);
                errorResult.put("summary", errorSummary.toString());
                errorResult.put("error", errorMsg);
                errorResult.put("partial_results", true);

                // 在异常处理中，我们不尝试提取页面内容，因为页面可能已经不可用
                // 只添加一个说明，表明由于错误无法提取页面内容
                if (extractPageContent) {
                    errorResult.put("content_error", "由于发生错误，无法提取页面内容");
                }

                BrowserCore._set_operation_status(false);
                return errorResult;
            } else {
                BrowserCore._set_operation_status(false);
                return errorMsg;
            }
        }
    }
}
