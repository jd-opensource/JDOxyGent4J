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
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * String processing tool class providing text analysis and extraction functionality.
 * <p>
 * This tool class offers various string processing capabilities including email address
 * extraction, URL extraction, and email format validation. Based on Java regular expressions
 * for pattern matching, provides accurate text analysis and structured data extraction.
 * </p>
 *
 * <p><strong>Main Features:</strong></p>
 * <ul>
 *   <li>Email address extraction from text</li>
 *   <li>URL link extraction from text</li>
 *   <li>Email format validation</li>
 *   <li>Regular expression based pattern matching</li>
 *   <li>JSON formatted result output</li>
 * </ul>
 *
 * <p><strong>Technical Implementation:</strong></p>
 * <ul>
 *   <li>Based on Java Pattern and Matcher classes for regular expression processing</li>
 *   <li>Uses Jackson for JSON serialization</li>
 *   <li>Duplicate removal and result deduplication</li>
 *   <li>Comprehensive pattern validation and error handling</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * StringTools stringTools = new StringTools();
 *
 * // Extract emails
 * String emails = stringTools.call("extract_emails", "Contact us at support@example.com");
 *
 * // Extract URLs
 * String urls = stringTools.call("extract_urls", "Visit https://www.example.com for more info");
 *
 * // Validate email
 * String validation = stringTools.call("validate_email", "user@example.com");
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @see FunctionHub Tool execution framework base class
 * @see Pattern Java regular expression pattern class
 * @see ObjectMapper Jackson JSON processor
 * @since 1.0.0
 */
public class StringTools extends FunctionHub {

    private final ObjectMapper objectMapper;

    // Email regex pattern - matches standard email format
    private static final String EMAIL_PATTERN = 
        "\\b[A-Za-z0-9](?:[A-Za-z0-9._-]*[A-Za-z0-9])?@[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?\\.[A-Za-z]{2,}\\b";

    // URL regex pattern - matches HTTP/HTTPS URLs
    private static final String URL_PATTERN = 
        "https?://(?:[a-zA-Z]|[0-9]|[$-_@.&+]|[!*\\\\(\\\\),]|(?:%[0-9a-fA-F][0-9a-fA-F]))+";

    // Strict email validation pattern
    private static final String STRICT_EMAIL_PATTERN = 
        "^[a-zA-Z0-9](?:[a-zA-Z0-9._-]*[a-zA-Z0-9])?@[a-zA-Z0-9](?:[a-zA-Z0-9.-]*[a-zA-Z0-9])?\\.[a-zA-Z]{2,}$";

    /**
     * Constructor to initialize String tools.
     * <p>
     * Initializes Jackson ObjectMapper for JSON processing and sets tool name to "string_tools".
     * </p>
     */
    public StringTools() {
        super("string_tools");
        this.setDesc("Tool set providing string processing and text analysis functionality, including email/URL extraction and validation");
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Extract email addresses from text.
     * <p>
     * Uses regular expression pattern matching to extract all email addresses from input text.
     * Automatically removes duplicate email addresses and returns results in JSON array format.
     * Supports standard email format matching including various domain extensions.
     * </p>
     *
     * <p><strong>Supported Email Formats:</strong></p>
     * <ul>
     *   <li>Standard format: user@example.com</li>
     *   <li>With dots: user.name@example.com</li>
     *   <li>With underscores: user_name@example.com</li>
     *   <li>With hyphens: user-name@example.com</li>
     *   <li>Various domain extensions: .com, .org, .net, .cn, etc.</li>
     * </ul>
     *
     * @param text Text to extract email addresses from, cannot be null or empty
     * @return JSON array string containing unique email addresses, returns empty array when no emails found
     * @throws IllegalArgumentException when text is null or empty
     */
    @Tool(
            name = "extract_emails",
            description = "Extract all email addresses from input text using regular expression pattern matching. Returns unique email addresses in JSON array format. Supports various email formats including dots, underscores, and hyphens.",
            paramMetas = {
                    @ParamMetaAuto(name = "text", type = "String", description = "Text content to extract email addresses from")
            }
    )
    public String extractEmails(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "[]";
        }

        try {
            Pattern pattern = Pattern.compile(EMAIL_PATTERN);
            Matcher matcher = pattern.matcher(text);
            
            Set<String> emails = new HashSet<>();
            while (matcher.find()) {
                emails.add(matcher.group().toLowerCase());
            }
            
            List<String> sortedEmails = emails.stream().sorted().collect(Collectors.toList());
            return objectMapper.writeValueAsString(sortedEmails);
            
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * Extract URLs from text.
     * <p>
     * Uses regular expression pattern matching to extract all HTTP/HTTPS URLs from input text.
     * Automatically removes duplicate URLs and returns results in JSON array format.
     * Supports standard web URL format matching.
     * </p>
     *
     * <p><strong>Supported URL Formats:</strong></p>
     * <ul>
     *   <li>HTTP: http://example.com</li>
     *   <li>HTTPS: https://example.com</li>
 *   <li>With paths: https://example.com/path/to/page</li>
     *   <li>With query parameters: https://example.com?param=value</li>
     *   <li>With fragments: https://example.com#section</li>
     * </ul>
     *
     * @param text Text to extract URLs from, cannot be null or empty
     * @return JSON array string containing unique URLs, returns empty array when no URLs found
     * @throws IllegalArgumentException when text is null or empty
     */
    @Tool(
            name = "extract_urls",
            description = "Extract all HTTP/HTTPS URLs from input text using regular expression pattern matching. Returns unique URLs in JSON array format. Supports standard web URL formats with paths, parameters, and fragments.",
            paramMetas = {
                    @ParamMetaAuto(name = "text", type = "String", description = "Text content to extract URLs from")
            }
    )
    public String extractUrls(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "[]";
        }

        try {
            Pattern pattern = Pattern.compile(URL_PATTERN);
            Matcher matcher = pattern.matcher(text);
            
            Set<String> urls = new HashSet<>();
            while (matcher.find()) {
                urls.add(matcher.group());
            }
            
            List<String> sortedUrls = urls.stream().sorted().collect(Collectors.toList());
            return objectMapper.writeValueAsString(sortedUrls);
            
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * Validate email address format.
     * <p>
     * Uses strict regular expression pattern to validate email address format correctness.
     * Returns validation result in JSON object format containing email address and validation status.
     * Performs comprehensive format checking including local part and domain part validation.
     * </p>
     *
     * <p><strong>Validation Rules:</strong></p>
     * <ul>
     *   <li>Must contain exactly one @ symbol</li>
     *   <li>Local part must start and end with alphanumeric characters</li>
     *   <li>Domain part must contain at least one dot</li>
     *   <li>Domain extension must be at least 2 characters</li>
     *   <li>Supports letters, numbers, dots, underscores, and hyphens</li>
     * </ul>
     *
     * @param email Email address to validate, cannot be null or empty
     * @return JSON object string containing email and validation result, format: {"email": "user@example.com", "is_valid": true}
     * @throws IllegalArgumentException when email is null or empty
     */
    @Tool(
            name = "validate_email",
            description = "Validate email address format using strict regular expression pattern. Returns JSON object with email address and validation status. Performs comprehensive format checking including local part and domain validation.",
            paramMetas = {
                    @ParamMetaAuto(name = "email", type = "String", description = "Email address to validate")
            }
    )
    public String validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return createValidationResult(email, false);
        }

        try {
            Pattern pattern = Pattern.compile(STRICT_EMAIL_PATTERN);
            boolean isValid = pattern.matcher(email.trim()).matches();
            return createValidationResult(email, isValid);
        } catch (Exception e) {
            return createValidationResult(email, false);
        }
    }

    /**
     * Create email validation result JSON.
     * <p>
     * Helper method to generate standardized validation result JSON format.
     * </p>
     *
     * @param email   Email address being validated
     * @param isValid Validation result
     * @return JSON formatted validation result
     */
    private String createValidationResult(String email, boolean isValid) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("email", email != null ? email : "");
            result.put("is_valid", isValid);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"email\": \"" + (email != null ? email : "") + "\", \"is_valid\": false}";
        }
    }

    // ========== Test Methods ==========

    /**
     * Test method demonstrating basic functionality of StringTools.
     * <p>
     * Tests email extraction, URL extraction, and email validation functionality
     * with sample text data to verify tool correctness and pattern matching accuracy.
     * </p>
     *
     * @param args Command line arguments (unused)
     */
    public static void main(String[] args) {
        StringTools stringTools = new StringTools();

        System.out.println("=== String Tools Test ===\n");

        // Test data
        String testText = """
            This is sample text containing email addresses and URLs.
            Contact us at support@example.com or visit our website at https://www.example.com
            More information at http://info.example.org or email info@example.org
            Invalid emails: invalid.email@ or missing@domain
            Another valid email: user.name+tag@domain.co.uk
            Visit our blog at https://blog.example.com/posts/latest-article
            """;

        // Test 1: Email extraction
        System.out.println("1. Email Extraction Test:");
        System.out.println("   Input text: " + testText.substring(0, 100) + "...");
        String emailsResult = stringTools.call("extract_emails", testText).toString();
        System.out.println("   Extracted emails: " + emailsResult + "\n");

        // Test 2: URL extraction
        System.out.println("2. URL Extraction Test:");
        System.out.println("   Input text: " + testText.substring(0, 100) + "...");
        String urlsResult = stringTools.call("extract_urls", testText).toString();
        System.out.println("   Extracted URLs: " + urlsResult + "\n");

        // Test 3: Email validation
        System.out.println("3. Email Validation Test:");
        String[] testEmails = {
            "support@example.com",
            "user.name+tag@domain.co.uk",
            "invalid.email@",
            "missing@domain",
            "valid@example.org",
            "test.user@sub.domain.com"
        };

        for (String email : testEmails) {
            String validationResult = stringTools.call("validate_email", email).toString();
            System.out.println("   " + email + ": " + validationResult);
        }

        // Test 4: Edge cases
        System.out.println("\n4. Edge Cases Test:");

        // Empty text
        String emptyResult = stringTools.call("extract_emails", "").toString();
        System.out.println("   Empty text email extraction: " + emptyResult);

        // Null email validation
        String nullValidation = stringTools.call("validate_email", (Object) null).toString();
        System.out.println("   Null email validation: " + nullValidation);
        
        // Text without emails
        String noEmailResult = stringTools.call("extract_emails", "This text contains no emails.").toString();
        System.out.println("   Text without emails: " + noEmailResult);
    }
}