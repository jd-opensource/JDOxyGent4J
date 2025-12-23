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
package com.jd.oxygent.core.oxygent.utils;

import java.util.Optional;

/**
 * String utility class
 *
 * <p>Provides common string operation methods, including null checking, whitespace checking
 * and other functions. This utility class is designed to simplify string processing operations
 * and improve code readability and robustness.</p>
 *
 * <p>Main Features:</p>
 * <ul>
 *     <li>Safe string length retrieval</li>
 *     <li>Blank string checking (including null, empty string, whitespace-only)</li>
 *     <li>Empty string checking (including null, empty string)</li>
 *     <li>Non-empty string validation</li>
 * </ul>
 *
 * <p>Usage Examples:</p>
 * <pre>{@code
 * // Check if string is blank
 * boolean blank = StringUtils.isBlank("   ");        // true
 * boolean notBlank = StringUtils.isNotBlank("abc");  // true
 *
 * // Safe string length retrieval
 * int length = StringUtils.length(null);             // 0
 * int length2 = StringUtils.length("hello");         // 5
 *
 * // Check if string is empty
 * boolean empty = StringUtils.isEmpty("");           // true
 * boolean empty2 = StringUtils.isEmpty(" ");         // false
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class StringUtils {

    /**
     * Safely gets the length of a character sequence
     *
     * <p>Gets the length of a character sequence, returns 0 if the character sequence is null.
     * This method avoids the NullPointerException that might be thrown by calling length() directly.</p>
     *
     * @param cs Character sequence to get length for, can be null
     * @return Length of character sequence, returns 0 if null
     * @since 1.0.0
     */
    public static int length(CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }

    /**
     * Checks if a character sequence is blank
     *
     * <p>Checks if a character sequence is null, empty string, or contains only whitespace characters.
     * Whitespace characters include spaces, tabs, newlines and all characters identified by Character.isWhitespace().</p>
     *
     * <p>Examples:</p>
     * <pre>{@code
     * StringUtils.isBlank(null)      = true
     * StringUtils.isBlank("")        = true
     * StringUtils.isBlank(" ")       = true
     * StringUtils.isBlank("bob")     = false
     * StringUtils.isBlank("  bob  ") = false
     * }</pre>
     *
     * @param cs Character sequence to check
     * @return Returns true if character sequence is blank, otherwise false
     * @since 1.0.0
     */
    public static boolean isBlank(CharSequence cs) {
        var strLen = length(cs);
        if (strLen == 0) {
            return true;
        }

        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a character sequence is not blank
     *
     * <p>This method is the negation of isBlank(), returns true when the character sequence
     * is not null, not empty string and contains non-whitespace characters.</p>
     *
     * @param cs Character sequence to check
     * @return Returns true if character sequence is not blank, otherwise false
     * @see #isBlank(CharSequence)
     * @since 1.0.0
     */
    public static boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
    }

    /**
     * Checks if a character sequence is empty
     *
     * <p>Checks if a character sequence is null or empty string. Note: This method does not
     * trim whitespace characters, a string containing only whitespace characters will be
     * considered non-empty. To check for blank strings, use the isBlank() method.</p>
     *
     * <p>Examples:</p>
     * <pre>{@code
     * StringUtils.isEmpty(null)      = true
     * StringUtils.isEmpty("")        = true
     * StringUtils.isEmpty(" ")       = false
     * StringUtils.isEmpty("bob")     = false
     * StringUtils.isEmpty("  bob  ") = false
     * }</pre>
     *
     * @param cs Character sequence to check, can be null
     * @return Returns true if character sequence is empty or null, otherwise false
     * @see #isBlank(CharSequence)
     * @since 1.0.0
     */
    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    /**
     * Checks if a character sequence is not empty
     *
     * <p>This method is the negation of isEmpty(), returns true when the character sequence
     * is not null and not empty string.</p>
     *
     * @param cs Character sequence to check
     * @return Returns true if character sequence is not empty, otherwise false
     * @see #isEmpty(CharSequence)
     * @since 1.0.0
     */
    public static boolean isNotEmpty(CharSequence cs) {
        return !isEmpty(cs);
    }

    /**
     * Safely converts a character sequence to Optional wrapper
     *
     * <p>If the character sequence is not blank, returns an Optional containing that character sequence,
     * otherwise returns an empty Optional. This method combines blank checking and Optional wrapping,
     * facilitating functional programming.</p>
     *
     * @param cs Character sequence to wrap
     * @return Returns Optional containing the character sequence if not blank, otherwise empty Optional
     * @since 1.0.0
     */
    public static Optional<String> toOptional(CharSequence cs) {
        return isNotBlank(cs) ? Optional.of(cs.toString()) : Optional.empty();
    }

    /**
     * Gets safe string representation of character sequence
     *
     * <p>If the character sequence is null, returns empty string; otherwise returns the string
     * representation of the character sequence. This method ensures never returns null value.</p>
     *
     * @param cs Character sequence
     * @return String representation of character sequence, never null
     * @since 1.0.0
     */
    public static String nullSafeToString(CharSequence cs) {
        return cs == null ? "" : cs.toString();
    }


}
