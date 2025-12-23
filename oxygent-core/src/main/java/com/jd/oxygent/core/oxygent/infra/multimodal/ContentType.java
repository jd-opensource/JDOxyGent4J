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
package com.jd.oxygent.core.oxygent.infra.multimodal;

import java.util.Arrays;
import java.util.Map;

/**
 * <h3>Multimodal Content Type Enumeration Strategy</h3>
 *
 * <p>ContentType is a strategy enumeration class in the OxyGent framework for handling multimodal content.
 * This class adopts the strategy pattern design, providing a unified processing interface and specific processing logic
 * for different types of multimodal content (text, image, audio, video, file), ensuring correct formatting
 * of content in LLM interactions.</p>
 *
 * <h3>Design Patterns</h3>
 * <ul>
 *   <li><strong>Strategy Pattern</strong>: Each enumeration value implements a specific content processing strategy</li>
 *   <li><strong>Factory Pattern</strong>: Creates appropriate processors through the fromContentType method</li>
 *   <li><strong>Polymorphism</strong>: Unified process interface with different implementation logic</li>
 *   <li><strong>Defensive Programming</strong>: Complete null value checking and default value handling</li>
 * </ul>
 *
 * <h3>Supported Content Types</h3>
 * <ul>
 *   <li><strong>TEXT</strong>: Plain text content, directly passed to LLM</li>
 *   <li><strong>IMAGE_URL</strong>: Image URL or base64 data, supports visual understanding</li>
 *   <li><strong>FILE</strong>: File path reference, used for document processing</li>
 *   <li><strong>AUDIO</strong>: Audio URL or data, supports voice processing</li>
 *   <li><strong>VIDEO</strong>: Video URL or data, supports video understanding</li>
 *   <li><strong>DEFAULT</strong>: Default processing strategy, converts unknown types to text</li>
 * </ul>
 *
 * <h3>Output Format Standards</h3>
 * <p>All processing strategies follow a unified output format:</p>
 * <ul>
 *   <li><strong>type field</strong>: Clearly identifies content type (text, image_url, audio, video, file)</li>
 *   <li><strong>content field</strong>: Contains corresponding data fields based on type (text, image_url, audio_url, etc.)</li>
 *   <li><strong>compatibility</strong>: Output format is compatible with mainstream LLM APIs (OpenAI, Claude, etc.)</li>
 * </ul>
 *
 * <h3>Content Processing Logic</h3>
 * <ul>
 *   <li><strong>TEXT</strong>: Extracts data field as text content</li>
 *   <li><strong>IMAGE_URL</strong>: Prioritizes url field, falls back to data field</li>
 *   <li><strong>FILE</strong>: Supports file_path and data fields, reserves file processing extensions</li>
 *   <li><strong>AUDIO/VIDEO</strong>: Extracts data field as media URL</li>
 *   <li><strong>DEFAULT</strong>: Intelligently converts any content to string format</li>
 * </ul>
 *
 * <h3>Usage Scenarios</h3>
 * <ul>
 *   <li><strong>LLM Message Construction</strong>: Converts multimodal input to LLM-understandable format</li>
 *   <li><strong>Content Type Recognition</strong>: Automatically recognizes and categorizes different types of input content</li>
 *   <li><strong>Format Standardization</strong>: Ensures all content conforms to unified data structure</li>
 *   <li><strong>Extension Support</strong>: Provides extensible processing framework for new content types</li>
 * </ul>
 *
 * <h3>Usage Examples</h3>
 * <pre>{@code
 * // Process text content
 * Map<String, Object> textContent = Map.of("data", "Hello, world!");
 * ContentType textType = ContentType.fromContentType("text");
 * Map<String, Object> result = textType.process(textContent);
 * // Output: {type=text, text=Hello, world!}
 *
 * // Process image URL
 * Map<String, Object> imageContent = Map.of("url", "https://example.com/image.jpg");
 * ContentType imageType = ContentType.fromContentType("image_url");
 * Map<String, Object> imageResult = imageType.process(imageContent);
 * // Output: {type=image_url, image_url={url=https://example.com/image.jpg}}
 *
 * // Automatic type recognition
 * ContentType autoType = ContentType.fromContentType("unknown_type");
 * // Returns: DEFAULT strategy
 * }</pre>
 *
 * <h3>Extensibility Design</h3>
 * <ul>
 *   <li><strong>New Type Addition</strong>: Can easily add new content type enumeration values</li>
 *   <li><strong>Processing Logic Customization</strong>: Each type can independently implement specific processing logic</li>
 *   <li><strong>Flexible Identifiers</strong>: Supports multiple identifiers corresponding to the same processing strategy</li>
 *   <li><strong>Backward Compatibility</strong>: DEFAULT strategy ensures smooth handling of unknown types</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public enum ContentType {
    /**
     * Plain text content.
     * <p>
     * Expects the input {@code content} map to contain a {@code "data"} key with a string value.
     * If {@code "data"} is missing or null, an empty string is used.
     * </p>
     * <p><b>Output format:</b>
     * <pre>{@code { "type": "text", "text": "..." }}</pre>
     * </p>
     */
    TEXT("text") {
        @Override
        public Map<String, Object> process(Map<String, Object> content) {
            String text = content.getOrDefault("data", "").toString();
            return Map.of("type", "text", "text", text != null ? text : "");
        }
    },

    /**
     * Image referenced by a URL.
     * <p>
     * Tries to read the image URL from the {@code "url"} key first.
     * If not present, falls back to the {@code "data"} key.
     * </p>
     * <p><b>Output format:</b>
     * <pre>{@code { "type": "image_url", "image_url": { "url": "https://..." } }}</pre>
     * </p>
     */
    IMAGE_URL("image_url", "url") {
        @Override
        public Map<String, Object> process(Map<String, Object> content) {
            String url = content.getOrDefault("url", "").toString();
            if (url == null) {
                url = content.getOrDefault("data", "").toString();
            }
            return Map.of("type", "image_url", "image_url", Map.of("url", url != null ? url : ""));
        }
    },

    /**
     * Local file path reference.
     * <p>
     * Reads the file path from {@code "data"} if available; otherwise, tries {@code "file_path"}.
     * Note: This implementation only passes through the path string—it does not validate existence
     * or read file contents. Additional logic (e.g., file validation) can be added as needed.
     * </p>
     * <p><b>Output format:</b>
     * <pre>{@code { "type": "file", "file_path": "/path/to/file.txt" }}</pre>
     * </p>
     */
    FILE("file", "path") {
        @Override
        public Map<String, Object> process(Map<String, Object> content) {
            String filePath = content.getOrDefault("data", "").toString();
            if (filePath == null) {
                filePath = content.getOrDefault("file_path", "").toString();
            }
            // File validation or content reading can be implemented here if required.
            return Map.of("type", "file", "file_path", filePath != null ? filePath : "");
        }
    },

    /**
     * Audio content referenced by a URL.
     * <p>
     * Uses the {@code "data"} field as the audio resource URL.
     * Null values are replaced with an empty string.
     * </p>
     * <p><b>Output format:</b>
     * <pre>{@code { "type": "audio", "audio_url": "https://..." }}</pre>
     * </p>
     */
    AUDIO("audio") {
        @Override
        public Map<String, Object> process(Map<String, Object> content) {
            String audioUrl = content.getOrDefault("data", "").toString();
            return Map.of("type", "audio", "audio_url", audioUrl != null ? audioUrl : "");
        }
    },

    /**
     * Video content referenced by a URL.
     * <p>
     * Uses the {@code "data"} field as the video resource URL.
     * Null values are replaced with an empty string.
     * </p>
     * <p><b>Output format:</b>
     * <pre>{@code { "type": "video", "video_url": "https://..." }}</pre>
     * </p>
     */
    VIDEO("video") {
        @Override
        public Map<String, Object> process(Map<String, Object> content) {
            String videoUrl = content.getOrDefault("data", "").toString();
            return Map.of("type", "video", "video_url", videoUrl != null ? videoUrl : "");
        }
    },

    /**
     * Fallback content type for unrecognized or generic input.
     * <p>
     * Converts the entire {@code content} map (or its {@code "data"} field) to a string
     * and treats it as plain text. Ensures that any input can be safely processed.
     * </p>
     * <p><b>Output format:</b>
     * <pre>{@code { "type": "text", "text": "..." }}</pre>
     * </p>
     */
    DEFAULT("default") {
        @Override
        public Map<String, Object> process(Map<String, Object> content) {
            String data = content.get("data") != null ? content.get("data").toString() : content.toString();
            return Map.of("type", "text", "text", data);
        }
    };

    private final String[] identifiers;

    ContentType(String... identifiers) {
        this.identifiers = identifiers;
    }

    /**
     * Core method for processing multimodal content
     * <p>
     * This abstract method defines a unified processing interface for all content types. Each enumeration value
     * must implement this method, providing specific content processing logic to convert input content
     * into a standard format understandable by LLM.
     *
     * <h3>Input Requirements</h3>
     * <ul>
     *   <li><strong>Non-null input</strong>: Implementations should handle null input and provide reasonable default values</li>
     *   <li><strong>Standard fields</strong>: Usually expects standard fields like "data", "url", "file_path"</li>
     *   <li><strong>Type compatibility</strong>: Should handle various data types (String, Map, List, etc.)</li>
     * </ul>
     *
     * <h3>Output Standards</h3>
     * <ul>
     *   <li><strong>type field</strong>: Must contain clear type identification</li>
     *   <li><strong>content field</strong>: Contains corresponding data fields based on type</li>
     *   <li><strong>LLM compatibility</strong>: Output format must be compatible with target LLM API</li>
     * </ul>
     *
     * @param content Map containing raw content data, may include data, url, file_path and other fields
     * @return Standardized content map containing type field and corresponding content fields
     */
    public abstract Map<String, Object> process(Map<String, Object> content);

    /**
     * Get corresponding processing strategy based on content type string
     * <p>
     * This factory method automatically selects the most appropriate processing strategy based on the input
     * content type string. Uses flexible matching algorithm, supports prefix matching and exact matching,
     * ensuring various format type identifiers can be correctly recognized.
     *
     * <h3>Matching Strategy</h3>
     * <ul>
     *   <li><strong>Exact matching</strong>: Prioritizes completely equal string matching</li>
     *   <li><strong>Prefix matching</strong>: Supports type strings starting with identifier</li>
     *   <li><strong>Multiple identifiers</strong>: Single type can support multiple identifiers</li>
     *   <li><strong>Default fallback</strong>: Unmatched types automatically use DEFAULT strategy</li>
     * </ul>
     *
     * <h3>Matching Examples</h3>
     * <ul>
     *   <li>"text" → TEXT</li>
     *   <li>"text/plain" → TEXT (prefix matching)</li>
     *   <li>"image_url" → IMAGE_URL</li>
     *   <li>"url" → IMAGE_URL (multiple identifier support)</li>
     *   <li>"unknown_type" → DEFAULT (default strategy)</li>
     * </ul>
     *
     * @param contentType Content type identifier string, can be complete type name or short identifier
     * @return Corresponding ContentType enumeration instance, returns DEFAULT when unmatched
     */
    public static ContentType fromContentType(String contentType) {
        if (contentType == null || contentType.trim().isEmpty()) {
            return DEFAULT;
        }
        return Arrays.stream(values())
                .filter(type -> Arrays.stream(type.identifiers)
                        .anyMatch(id -> contentType.equals(id) || contentType.startsWith(id)))
                .findFirst()
                .orElse(DEFAULT);
    }

    /**
     * Get identifier array
     *
     * @return Identifier array
     */
    public String[] getIdentifiers() {
        return identifiers.clone();
    }

    /**
     * Check if matches specified content type
     *
     * @param contentType Content type to check
     * @return Returns true if matches, otherwise false
     */
    public boolean matches(String contentType) {
        return Arrays.stream(identifiers)
                .anyMatch(id -> contentType.startsWith(id) || contentType.equals(id));
    }
}