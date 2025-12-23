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
package com.jd.oxygent.core.oxygent.tools;

import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;
import com.jd.oxygent.core.oxygent.utils.StringUtils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Time processing tool class providing cross-timezone time query and conversion functionality.
 * <p>
 * This tool class is based on Java 8+ modern time API (java.time package), providing 
 * core functions such as getting current time and timezone conversion. Supports major 
 * global timezones, specially optimized for Chinese users, using "Asia/Shanghai" as 
 * the default local timezone.
 * </p>
 *
 * <p><strong>Main Features:</strong></p>
 * <ul>
 *   <li>Get current time in specified timezone</li>
 *   <li>Time conversion between timezones</li>
 *   <li>Intelligent timezone recognition and default value handling</li>
 *   <li>Comprehensive error handling and fault tolerance mechanism</li>
 * </ul>
 *
 * <p><strong>Supported Timezone Formats:</strong></p>
 * <ul>
 *   <li>IANA timezone standard format (e.g.: Asia/Shanghai, America/New_York, Europe/London)</li>
 *   <li>Automatic fault tolerance for invalid timezones, fallback to default timezone</li>
 *   <li>Support for major cities and regions worldwide</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * TimeTool timeTool = new TimeTool();
 *
 * // Get current Beijing time
 * String beijingTime = timeTool.call("get_current_time", "Asia/Shanghai");
 *
 * // Convert Beijing time 15:30 to New York time
 * String nyTime = timeTool.call("convert_time", "Asia/Shanghai", "15:30", "America/New_York");
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 * @see FunctionHub Tool execution framework base class
 * @see ZonedDateTime Java timezone time handling class
 */
public class TimeTool extends FunctionHub {

    /** Default timezone: China Shanghai timezone */
    private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    /** Standard time formatter */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    /** Time formatter (hour and minute only) */
    private static final DateTimeFormatter HOUR_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Constructor to initialize time tool.
     * <p>
     * Sets tool name to "time_tools" and provides basic tool description information.
     * </p>
     */
    public TimeTool() {
        super("time_tools");
        this.setDesc("Tool set providing cross-timezone time query and conversion functionality, supports major global timezones");
    }

    /**
     * Get current time in specified timezone.
     * <p>
     * Gets current time based on specified IANA timezone identifier and returns it in 
     * standard format. If timezone identifier is invalid or empty, automatically uses 
     * Asia/Shanghai timezone as default.
     * </p>
     *
     * <p><strong>Supported Timezone Examples:</strong></p>
     * <ul>
     *   <li>Asia/Shanghai - China Standard Time</li>
     *   <li>America/New_York - US Eastern Time</li>
     *   <li>Europe/London - UK Standard Time</li>
     *   <li>Asia/Tokyo - Japan Standard Time</li>
     * </ul>
     *
     * @param timezone Timezone identifier, supports IANA standard format, uses default timezone when null or invalid
     * @return Formatted current time string, format "yyyy-MM-dd HH:mm:ss z"
     */
    @Tool(
        name = "get_current_time",
        description = "Get current time in specified timezone. Supports IANA standard timezone format like Asia/Shanghai, America/New_York etc. Invalid timezones will automatically use Shanghai timezone.",
        paramMetas = {
            @ParamMetaAuto(
                name = "timezone",
                type = "String",
                description = "IANA timezone identifier (e.g.: Asia/Shanghai, America/New_York, Europe/London). Uses Asia/Shanghai timezone when empty.",
                defaultValue = DEFAULT_TIMEZONE
            )
        }
    )
    public String getCurrentTime(String timezone) {
        var targetZone = parseTimezone(timezone).orElse(DEFAULT_TIMEZONE);

        try {
            var zoneId = ZoneId.of(targetZone);
            var now = ZonedDateTime.now(zoneId);
            return now.format(TIME_FORMATTER);
        } catch (Exception e) {
            // Fault tolerance: use system timezone when even default timezone fails
            var now = ZonedDateTime.now();
            return String.format("Timezone parsing failed, using system timezone: %s (%s)",
                    now.format(TIME_FORMATTER), e.getMessage());
        }
    }

    /**
     * Time conversion between timezones.
     * <p>
     * Converts specified time from source timezone to target timezone. Supports 24-hour 
     * format time input (HH:mm), performs conversion calculation based on current date. 
     * If timezone parameters are empty or invalid, uses default Shanghai timezone.
     * </p>
     *
     * <p><strong>Conversion Logic:</strong></p>
     * <ul>
     *   <li>Construct complete time point by combining current date with input time</li>
     *   <li>Locate absolute moment of that time point in source timezone</li>
     *   <li>Convert that moment to local time in target timezone</li>
     *   <li>Return corresponding time in target timezone (hour and minute only)</li>
     * </ul>
     *
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li>Time format must be 24-hour format (e.g.: 15:30, 09:00)</li>
     *   <li>Conversion based on current date, pay attention to cross-day situations</li>
     *   <li>Daylight saving time changes are handled automatically</li>
     * </ul>
     *
     * @param sourceTimezone Source timezone identifier, uses default timezone when null or empty
     * @param time           Time to convert, format HH:mm (24-hour format)
     * @param targetTimezone Target timezone identifier, uses default timezone when null or empty
     * @return Converted target timezone time (HH:mm format), returns error message on conversion failure
     */
    @Tool(
        name = "convert_time",
        description = "Time conversion between timezones. Converts specified time from source timezone to target timezone, supports major global timezones, automatically handles daylight saving time changes.",
        paramMetas = {
            @ParamMetaAuto(
                name = "source_timezone",
                type = "String",
                description = "Source timezone IANA identifier (e.g.: Asia/Shanghai, America/New_York). Uses Shanghai timezone when empty.",
                defaultValue = DEFAULT_TIMEZONE
            ),
            @ParamMetaAuto(
                name = "time",
                type = "String",
                description = "Time to convert, 24-hour format (HH:mm), e.g.: 15:30, 09:00",
                defaultValue = ""
            ),
            @ParamMetaAuto(
                name = "target_timezone",
                type = "String",
                description = "Target timezone IANA identifier (e.g.: Asia/Tokyo, America/Los_Angeles). Uses Shanghai timezone when empty.",
                defaultValue = DEFAULT_TIMEZONE
            )
        }
    )
    public String convertTime(String sourceTimezone, String time, String targetTimezone) {
        // Parameter validation and preprocessing
        if (StringUtils.isEmpty(time)) {
            return "Error: Time parameter cannot be empty, please provide time in HH:mm format";
        }

        var sourceZone = parseTimezone(sourceTimezone).orElse(DEFAULT_TIMEZONE);
        var targetZone = parseTimezone(targetTimezone).orElse(DEFAULT_TIMEZONE);

        try {
            // Parse input time
            var localTime = LocalTime.parse(time.trim(), HOUR_MINUTE_FORMATTER);

            // Use current date to construct complete datetime
            var today = LocalDate.now();
            var localDateTime = LocalDateTime.of(today, localTime);

            // Locate that time point in source timezone
            var sourceZoneId = ZoneId.of(sourceZone);
            var sourceZonedDateTime = ZonedDateTime.of(localDateTime, sourceZoneId);

            // Convert to target timezone
            var targetZoneId = ZoneId.of(targetZone);
            var targetZonedDateTime = sourceZonedDateTime.withZoneSameInstant(targetZoneId);

            // Format output, return hour and minute only
            return targetZonedDateTime.format(HOUR_MINUTE_FORMATTER);

        } catch (DateTimeParseException e) {
            return "Error: Invalid time format, please use HH:mm format (e.g.: 15:30)";
        } catch (Exception e) {
            return String.format("Timezone conversion failed: %s", e.getMessage());
        }
    }

    /**
     * Parse timezone string.
     * <p>
     * Validates and normalizes timezone identifier, supports fault tolerance. If input 
     * is empty or null, returns empty Optional; if timezone format is valid, returns 
     * Optional containing timezone identifier.
     * </p>
     *
     * @param timezone Timezone identifier string
     * @return Optional wrapped timezone identifier, returns empty Optional when invalid
     */
    private Optional<String> parseTimezone(String timezone) {
        if (StringUtils.isEmpty(timezone)) {
            return Optional.empty();
        }

        var trimmedTimezone = timezone.trim();
        try {
            // Validate timezone identifier validity
            ZoneId.of(trimmedTimezone);
            return Optional.of(trimmedTimezone);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // ========== Test Methods ==========

    /**
     * Test method demonstrating basic functionality of TimeTool.
     * <p>
     * Tests timezone time query and conversion functions, including normal and 
     * exceptional case handling, to verify tool correctness and robustness.
     * </p>
     *
     * @param args Command line arguments (unused)
     */
    public static void main(String[] args) {
        var timeTool = new TimeTool();

        System.out.println("=== Time Tool Test ===");

        // Test getting current time
        System.out.println("1. Get current time test:");
        var currentBeijing = timeTool.call("get_current_time", "Asia/Shanghai");
        System.out.println("   Beijing current time: " + currentBeijing);

        var currentNY = timeTool.call("get_current_time", "America/New_York");
        System.out.println("   New York current time: " + currentNY);

        var currentLondon = timeTool.call("get_current_time", "Europe/London");
        System.out.println("   London current time: " + currentLondon);

        // Test timezone conversion
        System.out.println("\n2. Timezone conversion test:");
        var beijingToNY = timeTool.call("convert_time", "Asia/Shanghai", "15:30", "America/New_York");
        System.out.println("   Beijing 15:30 -> New York time: " + beijingToNY);

        var nyToTokyo = timeTool.call("convert_time", "America/New_York", "09:00", "Asia/Tokyo");
        System.out.println("   New York 09:00 -> Tokyo time: " + nyToTokyo);

        var londonToSydney = timeTool.call("convert_time", "Europe/London", "18:45", "Australia/Sydney");
        System.out.println("   London 18:45 -> Sydney time: " + londonToSydney);

        // Test error handling
        System.out.println("\n3. Error handling test:");
        var invalidTimezone = timeTool.call("get_current_time", "Invalid/Timezone");
        System.out.println("   Invalid timezone handling: " + invalidTimezone);

        var invalidTime = timeTool.call("convert_time", "Asia/Shanghai", "25:70", "America/New_York");
        System.out.println("   Invalid time format: " + invalidTime);

        var emptyTime = timeTool.call("convert_time", "Asia/Shanghai", "", "America/New_York");
        System.out.println("   Empty time parameter: " + emptyTime);

        // Test default value handling
        System.out.println("\n4. Default value test:");
        var defaultTimezone = timeTool.call("get_current_time", null);
        System.out.println("   Null timezone handling: " + defaultTimezone);

        var defaultSourceZone = timeTool.call("convert_time", "", "12:00", "America/New_York");
        System.out.println("   Default source timezone: " + defaultSourceZone);
    }
}
