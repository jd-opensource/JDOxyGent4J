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

/**
 * <h3>Document Extractor Type Enumeration</h3>
 *
 * <p>Defines various document extractor types supported by the OxyGent framework.
 * Each type corresponds to a specific document format processor, used to convert
 * documents of different formats into text format understandable by LLM.</p>
 *
 * <h3>Supported Document Types</h3>
 * <ul>
 *   <li><strong>PDF</strong>: PDF document extractor, supports .pdf format</li>
 *   <li><strong>EXCEL</strong>: Excel spreadsheet extractor, supports .xlsx, .xls formats</li>
 *   <li><strong>WORD</strong>: Word document extractor, supports .docx, .doc formats</li>
 *   <li><strong>CSV</strong>: CSV spreadsheet extractor, supports .csv format</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public enum ExtractorType {

    /**
     * PDF document extractor
     * Supports text content extraction from Adobe PDF format documents
     */
    PDF("PDF Document Extractor", "Supports text content extraction from Adobe PDF format documents"),

    /**
     * Excel spreadsheet extractor
     * Supports data extraction and formatting from Microsoft Excel format spreadsheets
     */
    EXCEL("Excel Spreadsheet Extractor", "Supports data extraction and formatting from Microsoft Excel format spreadsheets"),

    /**
     * Word document extractor
     * Supports text content extraction from Microsoft Word format documents
     */
    WORD("Word Document Extractor", "Supports text content extraction from Microsoft Word format documents"),

    /**
     * CSV spreadsheet extractor
     * Supports data extraction and formatting from comma-separated values format files
     */
    CSV("CSV Spreadsheet Extractor", "Supports data extraction and formatting from comma-separated values format files");

    /**
     * Display name of the extractor type
     */
    private final String displayName;

    /**
     * Detailed description of the extractor type
     */
    private final String description;

    /**
     * Constructor
     *
     * @param displayName Display name
     * @param description Detailed description
     */
    ExtractorType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Get display name
     *
     * @return Display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get detailed description
     *
     * @return Detailed description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Find extractor type by display name
     *
     * @param displayName Display name
     * @return Corresponding extractor type, returns null if not found
     */
    public static ExtractorType fromDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return null;
        }

        for (ExtractorType type : values()) {
            if (type.displayName.equals(displayName.trim())) {
                return type;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return displayName;
    }
}