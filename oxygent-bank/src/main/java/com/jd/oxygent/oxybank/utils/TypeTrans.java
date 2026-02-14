package com.jd.oxygent.oxybank.utils;

import lombok.extern.slf4j.Slf4j;

/**
 * Type transformation utility class
 * Convert DataFrame field types to frontend display field types
 * Converted from oxybank/utils/type_trans.py
 */
@Slf4j
public class TypeTrans {
    
    /**
     * Convert Python data type to frontend display type
     * @param dtype Python data type string
     * @return Frontend display type string
     */
    public static String getPyType(String dtype) {
        if ("object".equals(dtype)) {
            return "string";
        } else if ("int32".equals(dtype) || "int64".equals(dtype)) {
            return "integer";
        } else if ("float32".equals(dtype) || "float64".equals(dtype)) {
            return "float";
        } else {
            // TODO: Need to add support for other types, such as datetime and boolean types
            log.debug("Unsupported dtype: {}, defaulting to string", dtype);
            return "string";
        }
    }
}