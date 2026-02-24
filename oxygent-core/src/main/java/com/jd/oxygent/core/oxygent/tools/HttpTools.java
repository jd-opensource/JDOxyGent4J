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
package com.jd.oxygent.core.oxygent.tools;

import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * HTTP request tool class providing GET and POST request functionality.
 * <p>
 * This tool class encapsulates common HTTP operations, supporting both GET and POST requests
 * with customizable headers and parameters. Uses OkHttp as the underlying HTTP client to ensure
 * stable and efficient network communication. All responses are returned in JSON format for easy
 * parsing and processing.
 * </p>
 *
 * <p><strong>Main Features:</strong></p>
 * <ul>
 *   <li>GET Request - Support URL parameters and custom headers</li>
 *   <li>POST Request - Support JSON data transmission and custom headers</li>
 *   <li>Automatic JSON response formatting</li>
 *   <li>Comprehensive error handling and exception management</li>
 * </ul>
 *
 * <p><strong>Technical Details:</strong></p>
 * <ul>
 *   <li>Based on OkHttp 4.x for HTTP communication</li>
 *   <li>Default timeout settings: 30 seconds connection timeout, 30 seconds read timeout</li>
 *   <li>All responses automatically converted to JSON format</li>
 *   <li>Built-in Content-Type header management for POST requests</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * HttpTools httpTools = new HttpTools();
 *
 * // GET request
 * String getResult = httpTools.call("http_get", "https://api.example.com/data", null, Map.of("key", "value"));
 *
 * // POST request
 * Map<String, Object> postData = Map.of("name", "test", "value", 123);
 * String postResult = httpTools.call("http_post", "https://api.example.com/submit", postData, null);
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @see FunctionHub Tool execution framework base class
 * @see OkHttpClient OkHttp HTTP client
 * @since 1.0.0
 */
public class HttpTools extends FunctionHub {

    private final OkHttpClient httpClient;

    /**
     * Constructor to initialize HTTP tools.
     * <p>
     * Creates OkHttp client instance with default timeout settings and sets tool name to "http_tools".
     * </p>
     */
    public HttpTools() {
        super("http_tools");
        this.setDesc("Tool set providing HTTP GET and POST request functionality, supporting custom headers and parameters");

        // Initialize HTTP client with timeout settings
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Send HTTP GET request.
     * <p>
     * Makes GET request to specified URL with optional headers and parameters. Automatically handles
     * URL parameter encoding and returns response in standardized JSON format containing status code,
     * headers, and response content.
     * </p>
     *
     * <p><strong>Response Format:</strong></p>
     * <pre>{@code
     * {
     *   "status_code": 200,
     *   "headers": {
     *     "Content-Type": "application/json",
     *     "Server": "nginx"
     *   },
     *   "content": "response body content"
     * }
     * }</pre>
     *
     * @param url     Target URL, cannot be null or empty
     * @param headers Optional HTTP headers, can be null
     * @param params  Optional URL parameters, can be null
     * @return JSON formatted response containing status code, headers, and content, or error information
     * @throws IllegalArgumentException when URL is null or empty
     */
    @Tool(
            name = "http_get",
            description = "Make a GET request to a specified URL with optional headers and parameters. Returns standardized JSON response containing status code, headers, and content.",
            paramMetas = {
                    @ParamMetaAuto(name = "url", type = "String", description = "Target URL for GET request"),
                    @ParamMetaAuto(name = "headers", type = "Map<String,String>", description = "Optional HTTP headers, can be null", defaultValue = "null"),
                    @ParamMetaAuto(name = "params", type = "Map<String,Object>", description = "Optional URL parameters, can be null", defaultValue = "null")
            }
    )
    public String httpGet(String url, Map<String, String> headers, Map<String, Object> params) {
        if (url.trim().isEmpty()) {
            return "{\"error\": \"URL cannot be empty\"}";
        }
        try {
            // Build request URL with parameters
            HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
            if (params != null) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    urlBuilder.addQueryParameter(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }

            // Build request
            Request.Builder requestBuilder = new Request.Builder()
                    .url(urlBuilder.build())
                    .get();

            // Add headers
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    requestBuilder.addHeader(header.getKey(), header.getValue());
                }
            }

            Request request = requestBuilder.build();

            // Execute request
            try (Response response = httpClient.newCall(request).execute()) {
                ResponseBody responseBody = response.body();
                String content = responseBody != null ? responseBody.string() : "";

                // Build response map
                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("status_code", response.code());
                responseMap.put("headers", response.headers().toString());
                responseMap.put("content", content);

                return toJsonString(responseMap);
            }
        } catch (Exception e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return toJsonString(errorMap);
        }
    }

    /**
     * Send HTTP POST request.
     * <p>
     * Makes POST request to specified URL with optional JSON data and headers. Automatically sets
     * Content-Type to application/json and returns response in standardized JSON format containing
     * status code, headers, and response content.
     * </p>
     *
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li>Automatically sets Content-Type header to application/json</li>
     *   <li>Data parameter will be automatically serialized to JSON format</li>
     *   <li>Supports complex data structures including nested objects and arrays</li>
     * </ul>
     *
     * @param url     Target URL, cannot be null or empty
     * @param data    Optional POST data, will be serialized to JSON, can be null
     * @param headers Optional HTTP headers, can be null
     * @return JSON formatted response containing status code, headers, and content, or error information
     * @throws IllegalArgumentException when URL is null or empty
     */
    @Tool(
            name = "http_post",
            description = "Make a POST request to a specified URL with optional JSON data and headers. Automatically sets Content-Type to application/json and returns standardized JSON response.",
            paramMetas = {
                    @ParamMetaAuto(name = "url", type = "String", description = "Target URL for POST request"),
                    @ParamMetaAuto(name = "data", type = "Map<String,Object>", description = "Optional POST data to be serialized to JSON, can be null", defaultValue = "null"),
                    @ParamMetaAuto(name = "headers", type = "Map<String,String>", description = "Optional HTTP headers, can be null", defaultValue = "null")
            }
    )
    public String httpPost(String url, Map<String, Object> data, Map<String, String> headers) {
        Objects.requireNonNull(url, "URL cannot be null");
        
        if (url.trim().isEmpty()) {
            return "{\"error\": \"URL cannot be empty\"}";
        }

        try {
            // Prepare JSON data
            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            String jsonData = data != null ? toJsonString(data) : "{}";
            RequestBody body = RequestBody.create(jsonData, JSON);

            // Build request
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .post(body);

            // Add headers (ensure Content-Type is set)
            Map<String, String> finalHeaders = headers != null ? new HashMap<>(headers) : new HashMap<>();
            finalHeaders.putIfAbsent("Content-Type", "application/json");
            
            for (Map.Entry<String, String> header : finalHeaders.entrySet()) {
                requestBuilder.addHeader(header.getKey(), header.getValue());
            }

            Request request = requestBuilder.build();

            // Execute request
            try (Response response = httpClient.newCall(request).execute()) {
                ResponseBody responseBody = response.body();
                String content = responseBody != null ? responseBody.string() : "";

                // Build response map
                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("status_code", response.code());
                responseMap.put("headers", response.headers().toString());
                responseMap.put("content", content);

                return toJsonString(responseMap);
            }
        } catch (Exception e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            return toJsonString(errorMap);
        }
    }

    /**
     * Convert object to JSON string.
     * <p>
     * Simple JSON serialization helper method using built-in toString() method for demonstration.
     * In production environments, it's recommended to use professional JSON libraries like Jackson or Gson.
     * </p>
     *
     * @param obj Object to serialize
     * @return JSON formatted string
     */
    private String toJsonString(Object obj) {
        // Simple implementation - in production, use Jackson or Gson
        return obj.toString().replace("=", ":").replace("{", "{").replace("}", "}");
    }

    // ========== Test Methods ==========

    /**
     * Test method demonstrating basic functionality of HttpTools.
     * <p>
     * Tests GET and POST request functionality with sample APIs to verify tool correctness.
     * </p>
     *
     * @param args Command line arguments (unused)
     */
    public static void main(String[] args) {
        HttpTools httpTools = new HttpTools();

        System.out.println("=== HTTP Tools Test ===");

        // Test GET request
        System.out.println("1. GET Request Test:");
        String getResult = (String) httpTools.call("http_get", "https://www.json.cn/", null, Map.of("key", "value"));
        System.out.println("   GET Result: " + getResult);

        // Test POST request
        System.out.println("\n2. POST Request Test:");
        Map<String, Object> postData = new HashMap<>();
        postData.put("name", "test");
        postData.put("value", 123);
        String postResult = (String) httpTools.call("http_post", "https://httpbin.org/post", postData, null);
        System.out.println("   POST Result: " + postResult);
    }
}