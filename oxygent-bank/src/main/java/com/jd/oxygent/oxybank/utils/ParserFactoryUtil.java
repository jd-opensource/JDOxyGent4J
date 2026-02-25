package com.jd.oxygent.oxybank.utils;

import com.jd.oxygent.oxybank.core.model.ParserConfig;
import com.jd.oxygent.oxybank.core.parser.ParserFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class ParserFactoryUtil {

    /**
     * Create parser instance based on configuration.
     *
     * @param parserConfig Parser configuration
     * @return Parser instance
     */
    public static Object createParserFromConfig(ParserConfig parserConfig) throws Exception {
        if (parserConfig == null) {
            log.info("Using default parser configuration (sentence parser)");
            return ParserFactory.createParser("sentence",
                Map.of("chunk_size", 300, "chunk_overlap", 20));
        }

        String parserType = parserConfig.getParserType();

        if ("extensible".equals(parserType)) {
            return ParserFactory.createParser(parserType, 
                Map.of(
                    "splitter_type", parserConfig.getSplitterType(),
                    "chunk_size", parserConfig.getChunkSize(),
                    "chunk_overlap", parserConfig.getChunkOverlap(),
                    "separator", parserConfig.getSeparator()
                ));
        } else if ("token".equals(parserType) || "sentence".equals(parserType)) {
            return ParserFactory.createParser(parserType, 
                Map.of(
                    "chunk_size", parserConfig.getChunkSize(),
                    "chunk_overlap", parserConfig.getChunkOverlap(),
                    "separator", parserConfig.getSeparator()
                ));
        } else if ("markdown".equals(parserType) || "html".equals(parserType) || "json".equals(parserType)) {
            return ParserFactory.createParser(parserType, 
                Map.of(
                    "include_metadata", parserConfig.isIncludeMetadata(),
                    "include_prev_next_rel", parserConfig.isIncludePrevNextRel()
                ));
        } else {
            log.warn("Unknown parser type: {}, using default sentence parser", parserType);
            return ParserFactory.createParser("sentence", 
                Map.of("chunk_size", 300, "chunk_overlap", 20));
        }
    }
}
