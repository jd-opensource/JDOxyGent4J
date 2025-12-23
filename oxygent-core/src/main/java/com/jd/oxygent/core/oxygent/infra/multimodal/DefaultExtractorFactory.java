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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * <h3>Default Document Extractor Factory Implementation</h3>
 *
 * <p>DefaultExtractorFactory is the default implementation of the ExtractorFactory interface,
 * automatically discovering and loading document extractor implementations based on Java SPI
 * (Service Provider Interface) mechanism. This factory provides efficient extractor management
 * and lookup functionality.</p>
 *
 * <h3>Core Features</h3>
 * <ul>
 *   <li><strong>SPI Integration</strong>: Automatically discovers DocumentExtractor implementations in classpath</li>
 *   <li><strong>Caching Mechanism</strong>: Caches extractor instances to avoid repeated loading</li>
 *   <li><strong>Priority Support</strong>: Automatically selects best implementation based on extractor priority</li>
 *   <li><strong>Thread Safety</strong>: Uses ConcurrentHashMap to ensure multi-thread safety</li>
 *   <li><strong>Lazy Loading</strong>: Loads extractor instances only on first use</li>
 * </ul>
 *
 * <h3>Loading Strategy</h3>
 * <ul>
 *   <li><strong>Auto Discovery</strong>: Automatically scans META-INF/services configuration through ServiceLoader</li>
 *   <li><strong>Priority Sorting</strong>: Same type extractors are sorted by priority and version</li>
 *   <li><strong>Exception Handling</strong>: Failed extractor loading is skipped and logged</li>
 *   <li><strong>Dynamic Refresh</strong>: Supports runtime reloading of extractors</li>
 * </ul>
 *
 * <h3>Usage Examples</h3>
 * <pre>{@code
 * // Create factory instance
 * ExtractorFactory factory = new DefaultExtractorFactory();
 *
 * // Get PDF extractor
 * Optional<DocumentExtractor> pdfExtractor = factory.getExtractor("pdf");
 *
 * // Check supported file types
 * Set<String> supportedTypes = factory.getSupportedExtensions();
 *
 * // Get statistics
 * String stats = factory.getStatistics();
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DefaultExtractorFactory implements ExtractorFactory {

    private static final Logger logger = LoggerFactory.getLogger(DefaultExtractorFactory.class);

    /**
     * Extension to extractor mapping cache
     * Key: File extension (lowercase)
     * Value: Corresponding document extractor instance
     */
    private final Map<String, DocumentExtractor> extensionToExtractorMap = new ConcurrentHashMap<>();

    /**
     * Type to extractor list mapping cache
     * Key: Extractor type
     * Value: List of all extractors of this type (sorted by priority)
     */
    private final Map<ExtractorType, List<DocumentExtractor>> typeToExtractorsMap = new ConcurrentHashMap<>();

    /**
     * Cache of all extractor instances
     */
    private volatile List<DocumentExtractor> allExtractors = new ArrayList<>();

    /**
     * Initialization flag
     */
    private volatile boolean initialized = false;

    /**
     * Initialization lock object
     */
    private final Object initLock = new Object();

    /**
     * Constructor
     *
     * <p>Creates DefaultExtractorFactory instance. Actual extractor loading uses lazy initialization strategy,
     * executing only when related methods are called for the first time.</p>
     */
    public DefaultExtractorFactory() {
        // Lazy initialization, load extractors on first use
    }

    /**
     * Ensure extractors are initialized
     */
    private void ensureInitialized() {
        if (!initialized) {
            synchronized (initLock) {
                if (!initialized) {
                    loadExtractors();
                    initialized = true;
                }
            }
        }
    }

    /**
     * Load all available document extractors
     */
    private void loadExtractors() {
        logger.info("Starting to load document extractors...");

        List<DocumentExtractor> extractors = new ArrayList<>();
        int loadedCount = 0;
        int failedCount = 0;

        try {
            // Use SPI mechanism to load all DocumentExtractor implementations
            ServiceLoader<DocumentExtractor> serviceLoader = ServiceLoader.load(DocumentExtractor.class);

            for (DocumentExtractor extractor : serviceLoader) {
                try {
                    // Validate basic extractor information
                    validateExtractor(extractor);

                    extractors.add(extractor);
                    loadedCount++;

                    logger.debug("Successfully loaded extractor: {} (type: {}, version: {}, priority: {})",
                            extractor.getClass().getSimpleName(),
                            extractor.getType(),
                            extractor.getVersion(),
                            extractor.getPriority());

                } catch (Exception e) {
                    failedCount++;
                    logger.warn("Failed to load extractor: {}, error: {}",
                            extractor.getClass().getSimpleName(), e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("Exception occurred while loading document extractors", e);
        }

        // Build cache mappings
        buildCacheMaps(extractors);

        logger.info("Document extractor loading completed, successful: {}, failed: {}, supported file types: {}",
                loadedCount, failedCount, extensionToExtractorMap.size());
    }

    /**
     * Validate extractor validity
     */
    private void validateExtractor(DocumentExtractor extractor) {
        if (extractor == null) {
            throw new IllegalArgumentException("Extractor instance cannot be null");
        }

        if (extractor.getType() == null) {
            throw new IllegalArgumentException("Extractor type cannot be null");
        }

        Set<String> extensions = extractor.getSupportedExtensions();
        if (extensions == null || extensions.isEmpty()) {
            throw new IllegalArgumentException("Supported extensions set cannot be null or empty");
        }

        // Check extension format
        for (String ext : extensions) {
            if (ext == null || ext.trim().isEmpty()) {
                throw new IllegalArgumentException("Extension cannot be null or empty");
            }
            if (ext.contains(".")) {
                throw new IllegalArgumentException("Extension should not contain dot: " + ext);
            }
        }
    }

    /**
     * Build cache mappings
     */
    private void buildCacheMaps(List<DocumentExtractor> extractors) {
        // Clear existing cache
        extensionToExtractorMap.clear();
        typeToExtractorsMap.clear();

        // Sort extractors by priority
        List<DocumentExtractor> sortedExtractors = extractors.stream()
                .sorted(Comparator.comparingInt(DocumentExtractor::getPriority)
                        .thenComparing(DocumentExtractor::getVersion, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        // Build extension mapping (higher priority overrides lower priority)
        for (DocumentExtractor extractor : sortedExtractors) {
            for (String extension : extractor.getSupportedExtensions()) {
                String normalizedExt = extension.toLowerCase();

                // If extractor for this extension already exists, compare priority
                DocumentExtractor existing = extensionToExtractorMap.get(normalizedExt);
                if (existing == null || extractor.getPriority() < existing.getPriority()) {
                    extensionToExtractorMap.put(normalizedExt, extractor);
                }
            }
        }

        // Build type mapping
        Map<ExtractorType, List<DocumentExtractor>> tempTypeMap = sortedExtractors.stream()
                .collect(Collectors.groupingBy(DocumentExtractor::getType));

        typeToExtractorsMap.putAll(tempTypeMap);

        // Update all extractors list
        this.allExtractors = new ArrayList<>(sortedExtractors);
    }

    @Override
    public Optional<DocumentExtractor> getExtractor(String fileExtension) {
        if (fileExtension == null || fileExtension.trim().isEmpty()) {
            throw new IllegalArgumentException("File extension cannot be null or empty string");
        }

        ensureInitialized();

        String normalizedExt = fileExtension.toLowerCase().trim();
        DocumentExtractor extractor = extensionToExtractorMap.get(normalizedExt);

        return Optional.ofNullable(extractor);
    }

    @Override
    public Optional<DocumentExtractor> getExtractor(File file) {
        if (file == null) {
            throw new IllegalArgumentException("File object cannot be null");
        }

        String fileName = file.getName();
        int lastDot = fileName.lastIndexOf('.');

        if (lastDot <= 0 || lastDot == fileName.length() - 1) {
            return Optional.empty();
        }

        String extension = fileName.substring(lastDot + 1);
        return getExtractor(extension);
    }

    @Override
    public List<DocumentExtractor> getAllExtractors() {
        ensureInitialized();
        return new ArrayList<>(allExtractors);
    }

    @Override
    public List<DocumentExtractor> getExtractorsByType(ExtractorType extractorType) {
        if (extractorType == null) {
            throw new IllegalArgumentException("Extractor type cannot be null");
        }

        ensureInitialized();

        List<DocumentExtractor> extractors = typeToExtractorsMap.get(extractorType);
        return extractors != null ? new ArrayList<>(extractors) : new ArrayList<>();
    }

    @Override
    public boolean supports(String fileExtension) {
        if (fileExtension == null || fileExtension.trim().isEmpty()) {
            return false;
        }

        ensureInitialized();

        String normalizedExt = fileExtension.toLowerCase().trim();
        return extensionToExtractorMap.containsKey(normalizedExt);
    }

    @Override
    public boolean supports(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }

        return getExtractor(file).isPresent();
    }

    @Override
    public Set<String> getSupportedExtensions() {
        ensureInitialized();
        return new HashSet<>(extensionToExtractorMap.keySet());
    }

    @Override
    public void refresh() {
        logger.info("Refreshing document extractor cache...");

        synchronized (initLock) {
            initialized = false;
            extensionToExtractorMap.clear();
            typeToExtractorsMap.clear();
            allExtractors = new ArrayList<>();
        }

        // Re-initialize
        ensureInitialized();

        logger.info("Document extractor cache refresh completed");
    }

    @Override
    public String getStatistics() {
        ensureInitialized();

        StringBuilder stats = new StringBuilder();
        stats.append("=== Document Extractor Factory Statistics ===\n");
        stats.append("Total extractors: ").append(allExtractors.size()).append("\n");
        stats.append("Supported file types: ").append(extensionToExtractorMap.size()).append("\n");

        // Statistics by type
        stats.append("Distribution by type:\n");
        for (ExtractorType type : ExtractorType.values()) {
            List<DocumentExtractor> extractors = typeToExtractorsMap.get(type);
            int count = extractors != null ? extractors.size() : 0;
            stats.append("  ").append(type.getDisplayName()).append(": ").append(count).append("\n");
        }

        // Supported extensions
        stats.append("Supported extensions: ").append(String.join(", ", getSupportedExtensions())).append("\n");

        return stats.toString();
    }
}