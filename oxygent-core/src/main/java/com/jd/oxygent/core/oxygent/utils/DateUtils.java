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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

/**
 * Date and time utility class
 *
 * <p>Provides rich date and time operation functions, supporting formatting, parsing, timestamp generation and other common operations.
 * This utility class combines traditional Date/SimpleDateFormat with modern LocalDateTime API,
 * providing flexible solutions for different usage scenarios.</p>
 *
 * <p>Main functional modules:</p>
 * <ul>
 *     <li>Date formatting: Supports custom format date string formatting</li>
 *     <li>Date parsing: Parse strings into Date objects</li>
 *     <li>Timestamp generation: Millisecond-level and minute-level timestamp generation</li>
 *     <li>Current time retrieval: Current time strings in various formats</li>
 *     <li>Special date handling: Permanent dates and other special business requirements</li>
 * </ul>
 *
 * <p>Predefined format constants:</p>
 * <ul>
 *     <li>{@link #DEFAULT_DATE_TIME_FORMAT}: yyyy-MM-dd HH:mm:ss</li>
 *     <li>{@link #DEFAULT_DATE_TIME_FORMAT2}: yyyy-MM-dd HH:mm:ss.SSSSSSSSS</li>
 *     <li>{@link #DEFAULT_DATE_FORMAT}: yyyy-MM-dd</li>
 *     <li>{@link #DEFAULT_TIME_FORMAT}: HH:mm:ss</li>
 * </ul>
 *
 * <p>Usage examples:</p>
 * <pre>{@code
 * // Format current date
 * String dateStr = DateUtils.formatDate(new Date(), DateUtils.DEFAULT_DATE_TIME_FORMAT);
 *
 * // Parse date string
 * Date date = DateUtils.parseDate("2023-10-01 12:34:56", DateUtils.DEFAULT_DATE_TIME_FORMAT);
 *
 * // Get timestamp
 * String timestamp = DateUtils.getCurrentTimeStamp();
 * String minuteTimestamp = DateUtils.getMinuteLevelTimeStamp();
 *
 * // Get current formatted time
 * String currentTime = DateUtils.getCurrentDateTime(DateUtils.DEFAULT_DATE_FORMAT);
 * }</pre>
 *
 * <p>Notes:</p>
 * <ul>
 *     <li>SimpleDateFormat is not thread-safe, new instances are created for each use</li>
 *     <li>Recommended to use LocalDateTime related methods for modern date-time needs</li>
 *     <li>Timestamp methods return string format for easy storage and transmission</li>
 * </ul>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DateUtils {
    /**
     * Default date-time format: yyyy-MM-dd HH:mm:ss
     */
    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * Default date-time format with nanoseconds: yyyy-MM-dd HH:mm:ss.SSSSSSSSS
     */
    public static final String DEFAULT_DATE_TIME_FORMAT2 = "yyyy-MM-dd HH:mm:ss.SSSSSSSSS";

    /**
     * Default date format: yyyy-MM-dd
     */
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

    /**
     * Default time format: HH:mm:ss
     */
    public static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";

    /**
     * Format Date object to string
     *
     * <p>Convert Date object to string representation using specified format pattern.
     * This method creates a new SimpleDateFormat instance for each call to ensure thread safety.</p>
     *
     * <p>Common format patterns:</p>
     * <ul>
     *     <li>yyyy-MM-dd HH:mm:ss - Standard date-time format</li>
     *     <li>yyyy-MM-dd - Date only format</li>
     *     <li>HH:mm:ss - Time only format</li>
     *     <li>yyyy/MM/dd - Slash-separated date format</li>
     * </ul>
     *
     * @param date    Date object to format, cannot be null
     * @param pattern Time formatting pattern, cannot be null or empty string
     * @return Formatted date-time string
     * @throws IllegalArgumentException When date or pattern is null or pattern is empty string
     * @since 1.0.0
     */
    public static String formatDate(Date date, String pattern) {
        Objects.requireNonNull(date, "Date object cannot be null");
        if (pattern == null || pattern.trim().isEmpty()) {
            throw new IllegalArgumentException("Format pattern cannot be null or empty string");
        }

        var sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }

    /**
     * Format current time
     *
     * <p>Format current system time using specified format pattern.
     * This is a convenience method for formatDate(new Date(), pattern).</p>
     *
     * @param pattern Time formatting pattern, cannot be null or empty string
     * @return Formatted current time string
     * @throws IllegalArgumentException When pattern is null or empty string
     * @see #formatDate(Date, String)
     * @since 1.0.0
     */
    public static String currentDateformat(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            throw new IllegalArgumentException("Format pattern cannot be null or empty string");
        }

        var date = new Date();
        var sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }


    /**
     * Parse string to Date object
     *
     * @param str     String to parse
     * @param pattern Time formatting pattern
     * @return Parsed Date object
     * @throws ParseException If string does not conform to specified format
     */
    public static Date parseDate(String str, String pattern) throws ParseException {
        if (str == null || pattern == null || pattern.isEmpty()) {
            throw new IllegalArgumentException("String and pattern cannot be null or empty");
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.parse(str);
    }

    /**
     * Get string representation of current time
     *
     * @param pattern Time formatting pattern
     * @return String representation of current time
     */
    public static String getCurrentDateTime(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            throw new IllegalArgumentException("Pattern cannot be null or empty");
        }
        return formatDate(new Date(), pattern);
    }

    /**
     * Get current timestamp in seconds
     *
     * @return Current timestamp in milliseconds as string
     */
    public static String getCurrentTimeStamp() {
        return String.valueOf(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    /**
     * Get current minute-level timestamp
     *
     * @return Minute-level timestamp as string
     */
    public static String getMinuteLevelTimeStamp() {
        // Get current time LocalDateTime object
        LocalDateTime now = LocalDateTime.now();

        // Set seconds and milliseconds to 0
        LocalDateTime minuteLevelTime = now.withSecond(0).withNano(0);

        // Convert to timestamp
        long timestamp = minuteLevelTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return String.valueOf(timestamp);
    }

    // Test method
    public static void main(String[] args) {
        try {
            // Format date
            Date now = new Date();
            String formattedDate = formatDate(now, DEFAULT_DATE_TIME_FORMAT);
            System.out.println("Formatted Date: " + formattedDate);

            // Parse date
            String dateStr = "2023-10-01 12:34:56";
            Date parsedDate = parseDate(dateStr, DEFAULT_DATE_TIME_FORMAT);
            System.out.println("Parsed Date: " + parsedDate);

            // Get current time
            String currentDateTime = getCurrentDateTime(DEFAULT_DATE_TIME_FORMAT);
            System.out.println("Current Date and Time: " + currentDateTime);

            String dayFormattedDate = formatDate(now, DEFAULT_DATE_FORMAT);
            System.out.println("dayFormattedDate Date: " + dayFormattedDate);

            String currentDateFormatted = currentDateformat(DEFAULT_TIME_FORMAT);
            System.out.println("dayFormattedDate Date: " + currentDateFormatted);

            System.out.println(getCurrentTimeStamp());
            System.out.println(getMinuteLevelTimeStamp());
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static Date permanentDateformat(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            throw new IllegalArgumentException("Date and pattern cannot be null or empty");
        }
        Calendar calendar = Calendar.getInstance();
        // Set date and time
        calendar.set(Calendar.YEAR, 2225);
        calendar.set(Calendar.MONTH, 0); // Month starts from 0, 0 represents January
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Convert to Date object
        Date date = calendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return date;
    }

    /**
     * Get current date and time, accurate to nanoseconds
     * yyyy-MM-dd HH:mm:ss.SSSSSSSSS
     *
     * @return Current date and time string with nanosecond precision
     */
    public static String nanoDate() {
        // Must use this method to get date accurate to nanoseconds, cannot use new Date() method
        Instant instant = Instant.now();
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of("Asia/Shanghai"));
        return zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS"));
    }
}