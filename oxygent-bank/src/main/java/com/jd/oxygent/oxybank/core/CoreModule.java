package com.jd.oxygent.oxybank.core;

import com.jd.oxygent.oxybank.core.model.embedding.EmbeddingFactory;
import com.jd.oxygent.oxybank.core.parser.ParserFactory;

/**
 * Core components for knowledge base platform.
 * Converted from core/__init__.py
 */
public class CoreModule {
    
    // Export the factory classes
    public static final Class<?> ParserFactory = ParserFactory.class;
    public static final Class<?> EmbeddingFactory = EmbeddingFactory.class;
    
    // Private constructor to prevent instantiation
    private CoreModule() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}