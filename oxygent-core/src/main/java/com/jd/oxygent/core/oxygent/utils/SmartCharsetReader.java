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

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * A Reader implementation that automatically detects the character set of an InputStream
 * using the ICU4J library.
 */
public class SmartCharsetReader extends Reader {
    private final Reader internalReader;
    private final String detectedCharset;

    /**
     * Constructs a new SmartCharsetReader by analyzing the first few bytes of the stream.
     *
     * @param is The source InputStream to wrap.
     * @throws IOException If an I/O error occurs during charset detection or stream reset.
     */
    public SmartCharsetReader(InputStream is) throws IOException {
        // 1. Use BufferedInputStream to support mark/reset, preventing the pre-read from consuming the stream
        BufferedInputStream bis = new BufferedInputStream(is);
        // Pre-read up to 8KB of data for heuristic analysis
        bis.mark(8192);

        CharsetDetector detector = new CharsetDetector();
        detector.setText(bis);

        // 2. Retrieve the best match based on statistical analysis
        CharsetMatch match = detector.detect();

        // Fallback to UTF-8 if detection confidence is too low (threshold: 20%)
        if (match != null && match.getConfidence() > 20) {
            this.detectedCharset = match.getName();
        } else {
            this.detectedCharset = StandardCharsets.UTF_8.name();
        }

        // 3. Reset the stream back to the beginning so no data is lost
        bis.reset();

        // 4. Initialize the actual bridge between byte stream and character stream
        this.internalReader = new InputStreamReader(bis, detectedCharset);
    }

    /**
     * @return The name of the charset detected by ICU4J.
     */
    public String getDetectedCharset() {
        return detectedCharset;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        return internalReader.read(cbuf, off, len);
    }

    @Override
    public void close() throws IOException {
        internalReader.close();
    }
}