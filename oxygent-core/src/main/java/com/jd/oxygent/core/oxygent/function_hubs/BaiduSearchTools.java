/*
 * Copyright 2025 JD.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this project except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.oxygent.core.oxygent.function_hubs;

import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;
import com.jd.oxygent.core.oxygent.tools.ParamMetaAuto;
import com.jd.oxygent.core.oxygent.tools.Tool;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class BaiduSearchTools extends FunctionHub {

    public BaiduSearchTools() {
        super("baidu_search_tools");
        this.setDesc("A tool that can search query on baidu search.");
    }

    @Tool(
            name = "search_baidu",
            description = "Search the query to baidu search",
            paramMetas = {
                    @ParamMetaAuto(
                            name = "query",
                            type = "String",
                            description = "The search query"
                    )
            }
    )
    public String searchBaidu(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be empty");
        }

        try {
            // Encode the query
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            String urlStr = "https://www.baidu.com/s?wd=" + encodedQuery + "&rn=10";

            // Create URL object
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            // Configure connection to follow redirects automatically
            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("GET");
            
            // Set complete browser-like headers
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            // Only accept gzip and deflate compression, not brotli (br)
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
            conn.setRequestProperty("Connection", "keep-alive");
            conn.setRequestProperty("Upgrade-Insecure-Requests", "1");
            conn.setRequestProperty("Cache-Control", "max-age=0");
            
            // Set timeouts
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            // Get response code and handle redirects
            int responseCode = conn.getResponseCode();
            
            // Manually handle redirects if needed
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || 
                responseCode == HttpURLConnection.HTTP_MOVED_TEMP || 
                responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                
                String redirectUrl = conn.getHeaderField("Location");
                if (redirectUrl != null) {
                    conn.disconnect();
                    // Follow redirect with same headers
                    url = new URL(redirectUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                    conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
                    conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
                    responseCode = conn.getResponseCode();
                }
            }

            // Check if response is successful
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Failed to get search results. Response code: " + responseCode);
            }

            // Get response and handle compression/encoding
            String contentEncoding = conn.getContentEncoding();
            BufferedReader in;
            
            // Try to detect and handle different compression methods
            if (contentEncoding != null) {
                String encoding = contentEncoding.toLowerCase();
                if (encoding.contains("gzip")) {
                    // Handle gzip compressed response
                    GZIPInputStream gzipIn = new GZIPInputStream(conn.getInputStream());
                    in = new BufferedReader(new InputStreamReader(gzipIn, StandardCharsets.UTF_8));
                } else if (encoding.contains("deflate")) {
                    // Handle deflate compressed response
                    InflaterInputStream inflaterIn = new InflaterInputStream(conn.getInputStream());
                    in = new BufferedReader(new InputStreamReader(inflaterIn, StandardCharsets.UTF_8));
                } else {
                    // Use declared encoding or default to UTF-8
                    Charset charset = getCharsetFromContentType(conn.getContentType());
                    in = new BufferedReader(new InputStreamReader(conn.getInputStream(), charset));
                }
            } else {
                // No encoding declared, try to auto-detect
                in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            }
            
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                if(!inputLine.equals("")){
                    response.append(inputLine).append("\n");
                }
            }
            in.close();
            conn.disconnect();

            // Parse search results
            List<Map<String, String>> results = parseBaiduResults(response.toString());

            // Convert results to JSON
            return JsonUtils.toJSONString(results);
        } catch (Exception e) {
            throw new RuntimeException("Error searching Baidu: " + e.getMessage(), e);
        }
    }

    // Parse Baidu search results from HTML
    private List<Map<String, String>> parseBaiduResults(String html) {
        List<Map<String, String>> results = new ArrayList<>();
        int rank = 1;

        // Check if HTML contains actual redirect page (not just meta tags in head)
        // Look for body-level redirects or nojs pages
        if (html.contains("document.location.replace") && 
            !html.contains("<body")) {
            // If document.location.replace exists but no body tag, it's a redirect page
            System.err.println("[WARNING] Detected redirect page, unable to parse search results");
            return results;
        }
        
        // Check for nojs=1 in URL context (actual redirect URL, not meta tag)
        int nojsIndex = html.indexOf("nojs=1");
        if (nojsIndex != -1) {
            // Check if it's in a meta refresh tag in the first part of the document
            int metaStart = Math.max(0, nojsIndex - 200);
            String context = html.substring(metaStart, Math.min(nojsIndex + 50, html.length()));
            if (context.contains("http-equiv") && context.contains("refresh")) {
                // It's in a meta tag, this is OK - don't treat as error
                // But we should check if there's actual content
                if (!html.contains("<body") || !html.contains("result")) {
                    System.err.println("[WARNING] Detected nojs redirect page, unable to parse search results");
                    return results;
                }
            }
        }

        // Remove script and style tags to simplify parsing
        String cleanHtml = html.replaceAll("(?s)<script[^>]*>.*?</script>", "")
                               .replaceAll("(?s)<style[^>]*>.*?</style>", "");

        // Find all h3 tags with class containing 't' (title class)
        int startIndex = 0;
        while (startIndex < cleanHtml.length() && results.size() < 10) {
            // Find h3 tag
            int h3Start = cleanHtml.indexOf("<h3", startIndex);
            if (h3Start == -1) break;
            
            // Check if this h3 has class attribute containing standalone 't'
            int h3End = cleanHtml.indexOf(">", h3Start);
            if (h3End == -1) {
                startIndex = h3Start + 1;
                continue;
            }
            
            String h3Tag = cleanHtml.substring(h3Start, h3End + 1);
            
            // Simple check: look for class="t or class='t or class="t or class='t followed by space or quote
            boolean hasTClass = h3Tag.contains("class=\"t ") || 
                               h3Tag.contains("class='t ") ||
                               h3Tag.contains("class=\"t\"") || 
                               h3Tag.contains("class='t'") ||
                               h3Tag.contains(" t \"") || 
                               h3Tag.contains(" t '") ||
                               h3Tag.endsWith(" t\"") || 
                               h3Tag.endsWith(" t'");
            
            if (!hasTClass) {
                startIndex = h3Start + 1;
                continue;
            }
            
            // Find closing </h3>
            int titleEndPos = cleanHtml.indexOf("</h3>", h3Start);
            if (titleEndPos == -1) break;
            
            String titleHtml = cleanHtml.substring(h3Start, titleEndPos + 5);
            String title = extractText(titleHtml);
            
            // Skip if title is empty or too short
            if (title.isEmpty() || title.length() < 2) {
                startIndex = titleEndPos;
                continue;
            }
            
            // Find URL in the h3 tag or nearby
            String url = extractUrlFromH3(cleanHtml, h3Start, titleEndPos);
            if (url == null || url.isEmpty()) {
                startIndex = titleEndPos;
                continue;
            }
            
            // Find abstract/description after the h3
            String abstractText = extractAbstractAfterH3(cleanHtml, titleEndPos);
            
            // Add result
            Map<String, String> result = new HashMap<>();
            result.put("title", title);
            result.put("url", url);
            result.put("abstract", abstractText);
            result.put("rank", String.valueOf(rank++));
            results.add(result);
            
            startIndex = titleEndPos;
        }

        return results;
    }
    
    // Extract URL from h3 tag area
    private String extractUrlFromH3(String html, int h3Start, int h3End) {
        // Look for href in the h3 section
        int searchEnd = Math.min(h3End + 1000, html.length()); // Search beyond h3 to catch anchor tags
        String section = html.substring(h3Start, searchEnd);
        
        // Find the first <a tag and then find href within it
        int aTagStart = section.indexOf("<a");
        if (aTagStart == -1) return null;
        
        // Find the closing > of the <a> tag
        int aTagEnd = section.indexOf(">", aTagStart);
        if (aTagEnd == -1) return null;
        
        // Extract the <a ...> portion
        String aTagContent = section.substring(aTagStart, Math.min(aTagEnd, section.length()));
        
        // Now find href= in the a tag
        int hrefStart = aTagContent.indexOf("href=");
        if (hrefStart == -1) return null;
        
        hrefStart += 5; // Move past "href=" (length is 5, not 6!)
        if (hrefStart >= aTagContent.length()) return null;
        
        char quoteChar = aTagContent.charAt(hrefStart);
        if (quoteChar == '"' || quoteChar == '\'') {
            hrefStart++; // Move past the opening quote
            int hrefEnd = aTagContent.indexOf(quoteChar, hrefStart);
            if (hrefEnd == -1) return null;
            String url = aTagContent.substring(hrefStart, hrefEnd);
            // Filter out invalid URLs
            if (url.startsWith("http") || url.startsWith("/")) {
                return url;
            }
        }
        
        return null;
    }
    
    // Extract abstract text after h3 tag
    private String extractAbstractAfterH3(String html, int h3End) {
        // Look for description in the next few hundred characters
        int searchStart = h3End;
        int searchEnd = Math.min(searchStart + 1000, html.length());
        String section = html.substring(searchStart, searchEnd);
        
        // Try to find common abstract patterns
        String[] patterns = {"c-abstract", "abstract", "summary", "description"};
        for (String pattern : patterns) {
            int pos = section.indexOf(pattern);
            if (pos != -1) {
                // Find the actual text content
                int textStart = section.indexOf(">", pos);
                if (textStart != -1) {
                    int textEnd = section.indexOf("<", textStart);
                    if (textEnd != -1) {
                        String text = extractText(section.substring(textStart, textEnd));
                        if (text.length() > 10) {
                            return text;
                        }
                    }
                }
            }
        }
        
        return "";
    }

    // Extract text from HTML
    private String extractText(String html) {
        return html.replaceAll("<[^>]*>", "").trim();
    }

    // Extract charset from Content-Type header
    private Charset getCharsetFromContentType(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return StandardCharsets.UTF_8;
        }
        
        // Parse charset from Content-Type header (e.g., "text/html; charset=utf-8")
        int charsetIndex = contentType.toLowerCase().indexOf("charset=");
        if (charsetIndex != -1) {
            String charsetName = contentType.substring(charsetIndex + 8).trim();
            // Remove any trailing parameters or quotes
            int semicolonIndex = charsetName.indexOf(';');
            if (semicolonIndex != -1) {
                charsetName = charsetName.substring(0, semicolonIndex).trim();
            }
            charsetName = charsetName.replace("\"", "").replace("'", "");
            
            try {
                return Charset.forName(charsetName);
            } catch (Exception e) {
                // If charset is invalid, fallback to UTF-8
                return StandardCharsets.UTF_8;
            }
        }
        
        return StandardCharsets.UTF_8;
    }

}
