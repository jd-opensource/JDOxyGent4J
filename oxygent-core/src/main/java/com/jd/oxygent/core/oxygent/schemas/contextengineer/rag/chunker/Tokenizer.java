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
package com.jd.oxygent.core.oxygent.schemas.contextengineer.rag.chunker;

/**
 * <h3>Text Token Counter</h3>
 *
 * <p>This class provides simplified text token calculation functionality, used to estimate token consumption in LLM processing.
 * Uses a heuristic algorithm based on character types, supporting mixed text processing for Chinese, English, and other languages.</p>
 *
 * <h3>Core Features:</h3>
 * <ul>
 *   <li><strong>Token Counting</strong>: Estimate the number of tokens in text</li>
 *   <li><strong>Text Truncation</strong>: Truncate text by token limit</li>
 *   <li><strong>Multilingual Support</strong>: Support for mixed Chinese and English text</li>
 * </ul>
 *
 * <h3>Calculation Rules:</h3>
 * <ul>
 *   <li><strong>Chinese Characters</strong>: Each character counts as 0.7 tokens</li>
 *   <li><strong>English Words</strong>: Every 5 letters is approximately one word, counts as 1.3 tokens</li>
 *   <li><strong>Other Characters</strong>: Punctuation, etc., count as 0.5 tokens</li>
 * </ul>
 *
 * <h3>Accuracy Note:</h3>
 * <p>This implementation is a heuristic rough estimate with an expected error within 10%. Applicable to:</p>
 * <ul>
 *   <li>Token budget control for batch processing</li>
 *   <li>Text chunk size estimation</li>
 *   <li>API call cost estimation</li>
 * </ul>
 *
 * <h3>Usage Scenarios:</h3>
 * <ul>
 *   <li>Text chunking in RAG systems</li>
 *   <li>Token control for LLM API calls</li>
 *   <li>Text preprocessing and optimization</li>
 *   <li>Resource planning for batch processing</li>
 * </ul>
 *
 * <h3>Note:</h3>
 * <p>This calculator only provides an estimate; for the actual token count, please refer to the specific LLM model's tokenizer.</p>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public final class Tokenizer {

    /**
     * Calculate the estimated number of tokens in text
     * <p>Uses a heuristic algorithm to calculate the number of tokens, supporting mixed Chinese and English text</p>
     *
     * <h4>Calculation Logic:</h4>
     * <ol>
     *   <li>Count Chinese characters (U+4E00 to U+9FFF)</li>
     *   <li>Count English letters (a-z, A-Z)</li>
     *   <li>Count other characters (punctuation, numbers, etc.)</li>
     *   <li>Apply weighted formula to calculate total tokens</li>
     * </ol>
     *
     * <h4>Weight Coefficients:</h4>
     * <ul>
     *   <li>Chinese characters: 0.7x weight</li>
     *   <li>English words: 1.3x weight (every 5 letters is one word)</li>
     *   <li>Other characters: 0.5x weight</li>
     * </ul>
     *
     * @param text Text content to calculate token count for
     * @return Estimated number of tokens, returns 0 for empty text
     */
    public static int count(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int chinese = 0, english = 0, other = 0;
        for (char c : text.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fff') {
                chinese++;
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                english++;
            } else {
                other++;
            }
        }
        // English word count ≈ letter count / 5
        int englishWords = (english + 4) / 5;
        return Math.round(chinese * 0.7f + englishWords * 1.3f + other * 0.5f);

    }

    /**
     * Truncate text by maximum token count
     * <p>Simple text truncation method, truncates by character length</p>
     *
     * <h4>Truncation Strategy:</h4>
     * <ul>
     *   <li>If the text length does not exceed the limit, return the original text directly</li>
     *   <li>If it exceeds the limit, take the specified number of characters from the beginning</li>
     * </ul>
     *
     * <h4>Note:</h4>
     * <p>This method truncates by character count, not strictly by token count, suitable for fast preprocessing scenarios</p>
     *
     * @param text      Text to be truncated
     * @param maxTokens Maximum token count limit
     * @return Truncated text, does not exceed the specified length
     */
    public static String cut(String text, int maxTokens) {
        return text.length() <= maxTokens ? text : text.substring(0, maxTokens);
    }
}
