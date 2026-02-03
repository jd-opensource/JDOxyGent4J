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
 * Browser search functionality
 *
 * Provides web search functionality, supporting Google, Bing and Baidu search engines
 */
public class BrowserSearch {

    @MCPTool(name = "browser_search", description = "执行网络搜索并返回搜索结果及页面内容")
    public Object browserSearch(
            @ToolParam(description = "搜索查询") String query,
            @ToolParam(description = "搜索引擎，支持google、bing或baidu", defaultValue = "bing") String searchEngine,
            @ToolParam(description = "返回的搜索结果数量，最大10个", defaultValue = "5") int numResults,
            @ToolParam(description = "搜索操作的超时时间(秒)", defaultValue = "30") int timeout,
            @ToolParam(description = "是否提取页面主要内容，用于二次确认", defaultValue = "true") boolean extractPageContent) {

        // Check dependencies
        List<String> missingDeps = BrowserCore.check_dependencies();
        if (!missingDeps.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("error", "Missing required libraries: " + String.join(", ", missingDeps) + ". Please install using maven: install " + String.join(" ", missingDeps));
            return result;
        }

        // Limit result count to reasonable range
        if (numResults < 1) numResults = 1;
        if (numResults > 10) numResults = 10;

        // Initialize result variables
        List<Map<String, Object>> results = new ArrayList<>();
        boolean partialResults = false;
        String errorMessage = null;

        BrowserCore._set_operation_status(true);

        try {
            // Ensure browser is started
            Page page = BrowserCore._ensure_page();

            // Encode search query
            String encodedQuery = URLEncoder.encode(query, "UTF-8");

            // Build URL based on selected search engine
            Map<String, String> searchUrls = new LinkedHashMap<>();
            searchUrls.put("google", "https://www.google.com/search?q=" + encodedQuery);
            searchUrls.put("bing", "https://www.bing.com/search?q=" + encodedQuery);
            searchUrls.put("baidu", "https://www.baidu.com/s?wd=" + encodedQuery);

            // Get search URL
            String searchUrl = searchUrls.getOrDefault(searchEngine.toLowerCase(), searchUrls.get("bing"));

            // Use timeout mechanism to navigate to search page
            try {
                page.navigate(searchUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
                Thread.sleep(2000);
            } catch (Exception e) {
            }
            // Wait for page to load completely, but set timeout
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Extract search results based on different search engines, using timeout mechanism
            String engine = searchEngine.toLowerCase();
            boolean extractionSuccess = false;

            try {
                if ("google".equals(engine)) {
                    // Extract Google search results
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
                    // Extract Bing search results
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
                    // Extract Baidu search results
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
                // If result extraction times out, record error and try generic extraction method
                errorMessage = "Result extraction timeout (>" + (timeout / 2) + " seconds), trying generic extraction method";
                partialResults = true;
            }

            // If no results found or extraction timed out, try generic extraction method
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
                        errorMessage += ", generic extraction method also timed out";
                    } else {
                        errorMessage = "Generic extraction method timed out (>" + (timeout / 3) + " seconds)";
                    }
                }
            }

            // Build search result summary
            StringBuilder summary = new StringBuilder();
            summary.append("Search query: \"").append(query).append("\"\n");
            summary.append("Search engine: ").append(searchEngine).append("\n");
            if (errorMessage != null) {
                summary.append("Warning: ").append(errorMessage).append("\n");
            }
            if (partialResults) {
                summary.append("Note: Due to timeout, may only return partial results\n");
            }
            summary.append("Found ").append(results.size()).append(" results\n\n");

            for (int i = 0; i < results.size(); i++) {
                Map<String, Object> result = results.get(i);
                summary.append(i + 1).append(". ").append(result.getOrDefault("title", "No title")).append("\n");
                summary.append("   Link: ").append(result.getOrDefault("link", "No link")).append("\n");
                String snippet = (String) result.getOrDefault("snippet", "");
                if (!snippet.isEmpty()) {
                    if (snippet.length() > 100) {
                        snippet = snippet.substring(0, 100) + "...";
                    }
                    summary.append("   Snippet: ").append(snippet).append("\n");
                }
                summary.append("\n");
            }

            // Extract main page content (if needed)
            Map<String, Object> pageContent = new LinkedHashMap<>();
            if (extractPageContent) {
                try {
                    // Get page title
                    pageContent.put("title", page.title());

                    // Get page URL
                    pageContent.put("url", page.url());

                    // Extract main page content
                    String pageText = page.evaluate("() => { const mainContent = document.querySelector('main') || document.querySelector('article') || document.querySelector('#content') || document.querySelector('.content') || document.body; return mainContent ? mainContent.innerText : document.body.innerText; }").toString();

                    // If text is too long, truncate to first 3000 characters
                    if (pageText.length() > 3000) {
                        pageContent.put("content", pageText.substring(0, 3000) + "...(content truncated)");
                    } else {
                        pageContent.put("content", pageText);
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
                        pageContent.put("metadata", importantMeta);
                    } catch (Exception e) {
                    }
                } catch (Exception contentError) {
                    pageContent.put("content_error", "Error occurred while extracting page content: " + contentError.getMessage());
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
            // Capture all other exceptions
            String errorMsg = "Error occurred while performing web search: " + e.getMessage();

            // If there are partial results, try to return them
            if (!results.isEmpty() && results.size() > 0) {
                StringBuilder errorSummary = new StringBuilder();
                errorSummary.append("Search query: \"").append(query).append("\"\n");
                errorSummary.append("Search engine: ").append(searchEngine).append("\n");
                errorSummary.append("Warning: ").append(errorMsg).append(", returning partial results\n");
                errorSummary.append("Found ").append(results.size()).append(" results\n\n");

                for (int i = 0; i < results.size(); i++) {
                    Map<String, Object> result = results.get(i);
                    errorSummary.append(i + 1).append(". ").append(result.getOrDefault("title", "No title")).append("\n");
                    errorSummary.append("   Link: ").append(result.getOrDefault("link", "No link")).append("\n");
                    String snippet = (String) result.getOrDefault("snippet", "");
                    if (!snippet.isEmpty()) {
                        if (snippet.length() > 100) {
                            snippet = snippet.substring(0, 100) + "...";
                        }
                        errorSummary.append("   Snippet: ").append(snippet).append("\n");
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

                // In exception handling, we don't try to extract page content, as the page may no longer be available
                // Only add a note indicating that page content could not be extracted due to error
                if (extractPageContent) {
                    errorResult.put("content_error", "Unable to extract page content due to error");
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
