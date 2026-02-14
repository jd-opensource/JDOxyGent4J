package com.jd.oxygent.oxybank.core.parser;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parser factory for knowledge base platform.
 * Converted from core/parser/factory.py
 */
@Slf4j
public class ParserFactory {
    
    // Map of parser types to their implementation classes
    private static final Map<String, Class<? extends Parser>> _parsers = new HashMap<>();
    
    static {
        // Initialize parser mappings
        _parsers.put("token", TokenTextSplitterParser.class);
        _parsers.put("sentence", SentenceSplitterParser.class);
        _parsers.put("markdown", MarkdownNodeParserWrapper.class);
        _parsers.put("html", HTMLNodeParserWrapper.class);
        _parsers.put("json", JSONNodeParserWrapper.class);
        _parsers.put("extensible", ExtensibleSplitterParser.class);
        _parsers.put("smart", SentenceSplitterParser.class);
    }
    
    /**
     * Create a parser instance.
     * 
     * @param parserType Type of parser to create
     * @param kwargs Configuration parameters for the parser
     * @return Parser instance
     * @throws IllegalArgumentException If parser_type is not supported
     * @throws Exception If parser creation fails
     */
    public static Parser createParser(String parserType, Map<String, Object> kwargs) throws Exception {
        if (!_parsers.containsKey(parserType)) {
            throw new IllegalArgumentException(
                "Unsupported parser type: " + parserType + ". " +
                "Available types: " + _parsers.keySet()
            );
        }
        
        Class<? extends Parser> parserClass = _parsers.get(parserType);
        log.info("Creating {} parser: {}", parserType, parserClass.getSimpleName());
        
        try {
            // Create parser instance using default constructor
            // TODO: Implement parameterized constructor support if needed
            return parserClass.newInstance();
        } catch (Exception e) {
            log.error("Failed to create {} parser: {}", parserType, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Register a custom parser type.
     * 
     * @param parserType Type name for the parser
     * @param parserClass Parser class to register
     */
    public static void registerParser(String parserType, Class<? extends Parser> parserClass) {
        _parsers.put(parserType, parserClass);
        log.info("Registered parser type: {}", parserType);
    }
    
    /**
     * Get list of supported parser types.
     * 
     * @return List of supported parser types
     */
    public static List<String> getSupportedTypes() {
        return List.copyOf(_parsers.keySet());
    }
    
    /**
     * Automatically select appropriate parser based on documents.
     * 
     * @param documents List of documents to analyze
     * @param kwargs Configuration parameters
     * @return Appropriate parser instance or null if not supported
     */
    public static Parser createAutoParser(List<Document> documents, Map<String, Object> kwargs) {
        if (documents == null || documents.isEmpty()) {
            log.warn("No documents provided for auto parser selection");
            return null;
        }
        
        // Analyze document types
        Set<String> fileTypes = new java.util.HashSet<>();
        for (Document doc : documents) {
            if (doc != null && doc.getMetadata() != null) {
                String fileType = doc.getMetadata().getOrDefault("file_type", "").toString().toLowerCase();
                if (!fileType.isEmpty()) {
                    fileTypes.add(fileType);
                }
            }
        }
        
        // Select parser based on file types
        if (fileTypes.isEmpty()) {
            log.info("No file type metadata found, using sentence parser");
            try {
                return createParser("sentence", kwargs);
            } catch (Exception e) {
                log.error("Failed to create sentence parser: {}", e.getMessage(), e);
                return null;
            }
        }
        
        // If all documents are of the same type, use specific parser
        if (fileTypes.size() == 1) {
            String fileType = fileTypes.iterator().next();
            
            // Map file extensions to parser types
            Map<String, String> typeMapping = Map.of(
                ".md", "markdown",
                ".markdown", "markdown",
                ".html", "html",
                ".htm", "html",
                ".json", "json",
                ".csv", "token"  // Use token splitter for CSV
            );
            
            String selectedParserType = typeMapping.get(fileType);
            if (selectedParserType != null) {
                log.info("Selected {} parser for file type {}", selectedParserType, fileType);
                try {
                    return createParser(selectedParserType, kwargs);
                } catch (Exception e) {
                    log.error("Failed to create {} parser: {}", selectedParserType, e.getMessage(), e);
                    return null;
                }
            }
        }
        
        // Mixed types or unknown types, use token parser as default
        log.info("Mixed file types {}, using token parser", fileTypes);
        try {
            return createParser("token", kwargs);
        } catch (Exception e) {
            log.error("Failed to create token parser: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Base Parser interface
     */
    public interface Parser {
        // Common parser methods would be defined here
    }
    
    /**
     * Document interface for parser input
     */
    public interface Document {
        Map<String, Object> getMetadata();
    }
    
    // Stub classes for parser implementations
    // These would be replaced with actual implementations
    public static class TokenTextSplitterParser implements Parser {}
    public static class SentenceSplitterParser implements Parser {}
    public static class MarkdownNodeParserWrapper implements Parser {}
    public static class HTMLNodeParserWrapper implements Parser {}
    public static class JSONNodeParserWrapper implements Parser {}
    public static class ExtensibleSplitterParser implements Parser {}
}