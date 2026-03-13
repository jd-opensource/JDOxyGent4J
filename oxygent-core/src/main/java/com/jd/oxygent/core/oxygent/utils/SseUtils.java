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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Parse an HTTP response body as a Server-Sent Events (SSE) stream.
 *
 * <p>Yields maps:
 *     <pre>
 *         {
 *             "event": String | null,
 *             "data": String,
 *             "id": String | null,
 *             "retry": Integer | null
 *         }
 *     </pre>
 * </p>
 *
 * <p>Security/robustness hardening:
 * <ul>
 *     <li>Caps buffer size to mitigate memory/CPU DoS if delimiters never arrive.</li>
 *     <li>Caps per-event size and per-event data size.</li>
 *     <li>Best-effort flush of the final event on EOF (optional).</li>
 *     <li>Ignores `id` values containing NUL (\x00).</li>
 *     <li>Accepts `retry` only if non-negative int.</li>
 * </ul>
 * </p>
 */
public class SseUtils {

    /**
     * Parse an HTTP response body as a Server-Sent Events (SSE) stream.
     *
     * @param inputStream the input stream to read from
     * @param eventConsumer the consumer to handle parsed events
     * @param chunkSize size of chunks to read (default: 8 * 1024)
     * @param maxBufferBytes maximum total buffered bytes (default: 2 * 1024 * 1024)
     * @param maxEventBytes maximum bytes per event block (default: 512 * 1024)
     * @param maxDataBytes maximum bytes of accumulated "data:" per event (default: 512 * 1024)
     * @param allowPartialFinalEvent whether to flush remaining bytes on EOF (default: true)
     * @throws IOException if an I/O error occurs
     */
    public static void iterSseEvents(
            InputStream inputStream,
            Consumer<Map<String, Object>> eventConsumer,
            int chunkSize,
            int maxBufferBytes,
            int maxEventBytes,
            int maxDataBytes,
            boolean allowPartialFinalEvent
    ) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        if (eventConsumer == null) {
            throw new IllegalArgumentException("Event consumer cannot be null");
        }

        // Buffer to accumulate bytes
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[chunkSize];
        int bytesRead;

        // Read chunks from input stream
        while ((bytesRead = inputStream.read(chunk)) != -1) {
            if (bytesRead == 0) {
                continue;
            }

            // Create actual chunk with correct size
            byte[] actualChunk = new byte[bytesRead];
            System.arraycopy(chunk, 0, actualChunk, 0, bytesRead);

            // Normalize newlines to LF ("\n") safely across chunk boundaries.
            if (buf.size() > 0) {
                byte[] bufArray = buf.toByteArray();
                if (bufArray[bufArray.length - 1] == 0x0D) { // '\r'
                    if (actualChunk.length > 0 && actualChunk[0] == 0x0A) { // '\n'
                        // Replace '\r' with '\n' and remove the following '\n'
                        bufArray[bufArray.length - 1] = 0x0A;
                        buf.reset();
                        buf.write(bufArray);
                        // Remove the leading '\n' from the chunk
                        byte[] newChunk = new byte[actualChunk.length - 1];
                        System.arraycopy(actualChunk, 1, newChunk, 0, newChunk.length);
                        actualChunk = newChunk;
                    } else {
                        // Replace trailing '\r' with '\n'
                        bufArray[bufArray.length - 1] = 0x0A;
                        buf.reset();
                        buf.write(bufArray);
                    }
                }
            }

            // Normalize inside this chunk.
            actualChunk = normalizeNewlines(actualChunk);
            buf.write(actualChunk);

            // Total buffer cap (anti-DoS).
            if (buf.size() > maxBufferBytes) {
                throw new IOException(
                        String.format("SSE buffer too large (> %d bytes); delimiter not found", maxBufferBytes)
                );
            }

            // Process complete events
            while (true) {
                // After normalization, SSE events are separated by "\n\n".
                byte[] bufArray = buf.toByteArray();
                int sep = findSeparator(bufArray);
                if (sep == -1) {
                    break;
                }

                // Per-event cap before copying bytes out.
                if (sep > maxEventBytes) {
                    throw new IOException(String.format("SSE event too large (> %d bytes)", maxEventBytes));
                }

                // Extract event block
                byte[] raw = new byte[sep];
                System.arraycopy(bufArray, 0, raw, 0, sep);

                // Remove processed event and separator from buffer
                byte[] remaining = new byte[bufArray.length - sep - 2];
                System.arraycopy(bufArray, sep + 2, remaining, 0, remaining.length);
                buf.reset();
                buf.write(remaining);

                // Parse and process event
                Map<String, Object> evt = parseEventBlock(raw, maxDataBytes);
                if (evt != null) {
                    eventConsumer.accept(evt);
                }
            }
        }

        // EOF flush: normalize trailing '\r' that never got paired.
        byte[] bufArray = buf.toByteArray();
        if (bufArray.length > 0 && bufArray[bufArray.length - 1] == 0x0D) {
            bufArray[bufArray.length - 1] = 0x0A;
            buf.reset();
            buf.write(bufArray);
        }

        // Flush remaining bytes if allowed
        if (allowPartialFinalEvent && buf.size() > 0) {
            if (buf.size() > maxEventBytes) {
                throw new IOException(String.format("SSE final event too large (> %d bytes)", maxEventBytes));
            }
            Map<String, Object> evt = parseEventBlock(buf.toByteArray(), maxDataBytes);
            if (evt != null) {
                eventConsumer.accept(evt);
            }
        }
    }

    /**
     * Overloaded method with default parameters.
     */
    public static void iterSseEvents(
            InputStream inputStream,
            Consumer<Map<String, Object>> eventConsumer
    ) throws IOException {
        iterSseEvents(
                inputStream,
                eventConsumer,
                8 * 1024,  // chunkSize
                2 * 1024 * 1024,  // maxBufferBytes
                512 * 1024,  // maxEventBytes
                512 * 1024,  // maxDataBytes
                true  // allowPartialFinalEvent
        );
    }

    /**
     * Normalizes all newlines in the byte array to LF ("\n").
     */
    private static byte[] normalizeNewlines(byte[] bytes) {
        String str = new String(bytes, StandardCharsets.UTF_8);
        str = str.replaceAll("\\r\\n", "\\n").replaceAll("\\r", "\\n");
        return str.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Finds the index of "\n\n" separator in the byte array.
     */
    private static int findSeparator(byte[] bytes) {
        for (int i = 0; i < bytes.length - 1; i++) {
            if (bytes[i] == 0x0A && bytes[i + 1] == 0x0A) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Parses a raw event block into a map.
     */
    private static Map<String, Object> parseEventBlock(byte[] raw, int maxDataBytes) {
        // Trim leading/trailing newlines within the event block.
        String rawStr = new String(raw, StandardCharsets.UTF_8).trim();
        if (rawStr.isEmpty()) {
            return null;
        }

        String eventType = null;
        String eventId = null;
        Integer retry = null;
        List<String> dataLines = new ArrayList<>();
        int dataBytes = 0;

        String[] lines = rawStr.split("\\n");
        for (String line : lines) {
            // Comment/heartbeat lines start with ":" and should be ignored.
            if (line.startsWith(":")) {
                continue;
            }

            // Field parsing:
            // - "field:value"
            // - "field:" (empty value)
            // - "field" (empty value)
            String fieldB;
            String valueB;
            if (line.contains(":")) {
                int colonIndex = line.indexOf(':');
                fieldB = line.substring(0, colonIndex);
                valueB = line.substring(colonIndex + 1);
                if (valueB.startsWith(" ")) {
                    valueB = valueB.substring(1);
                }
            } else {
                fieldB = line;
                valueB = "";
            }

            // Empty field names are ignored in practice.
            if (fieldB.isEmpty()) {
                continue;
            }

            String field = fieldB;

            if (field.equals("event")) {
                eventType = valueB;
            } else if (field.equals("data")) {
                // Cap total event data bytes (prevents huge multi-line data DoS).
                dataBytes += valueB.getBytes(StandardCharsets.UTF_8).length;
                if (dataBytes > maxDataBytes) {
                    throw new IllegalArgumentException(
                            String.format("SSE event data too large (> %d bytes)", maxDataBytes)
                    );
                }
                dataLines.add(valueB);
            } else if (field.equals("id")) {
                // Ignore IDs containing NUL (common interoperability/safety behavior).
                if (!valueB.contains("\u0000")) {
                    eventId = valueB;
                }
            } else if (field.equals("retry")) {
                try {
                    int n = Integer.parseInt(valueB);
                    if (n >= 0) {
                        retry = n;
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid retry values
                }
            }
        }

        // Skip blocks that contain no meaningful fields (e.g., only comments).
        if (eventType == null && eventId == null && retry == null && dataLines.isEmpty()) {
            return null;
        }

        // Create and return event map
        Map<String, Object> event = new HashMap<>();
        event.put("event", eventType);
        event.put("data", String.join("\n", dataLines));
        event.put("id", eventId);
        event.put("retry", retry);
        return event;
    }
}