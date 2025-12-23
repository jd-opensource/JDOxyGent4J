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

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * <h3>Intelligent Text Chunker</h3>
 *
 * <p>This class is a core text processing component in the OxyGent system, providing a variety of intelligent text chunking strategies.
 * Supports intelligently splitting long texts into chunks suitable for LLM processing, according to different document types and processing requirements.</p>
 *
 * <h3>Supported Chunking Strategies:</h3>
 * <ul>
 *   <li><strong>Recursive Separators</strong>: Hierarchical splitting using multiple separators in order of priority</li>
 *   <li><strong>Sentence Boundary</strong>: Sentence boundary detection based on NLP</li>
 *   <li><strong>Fixed Window</strong>: Uniform splitting by fixed character or token count</li>
 *   <li><strong>Paragraph Splitting</strong>: Splitting by document paragraph structure</li>
 *   <li><strong>Markdown Headers</strong>: Structural splitting by Markdown header hierarchy</li>
 *   <li><strong>Java Code</strong>: Splitting by code structure (classes, methods, etc.)</li>
 *   <li><strong>JSON Structure</strong>: Splitting by top-level JSON structure</li>
 *   <li><strong>Token Sliding Window</strong>: Sliding window splitting based on token count</li>
 * </ul>
 *
 * <h3>Core Features:</h3>
 * <ul>
 *   <li><strong>Flexible Configuration</strong>: Supports custom chunk size, overlap, and other parameters</li>
 *   <li><strong>Intelligent Overlap</strong>: Supports smart overlap between chunks to maintain context continuity</li>
 *   <li><strong>Token Control</strong>: Supports precise control based on token count</li>
 *   <li><strong>Type Adaptation</strong>: Provides specialized chunking strategies for different document types</li>
 * </ul>
 *
 * <h3>Usage Scenarios:</h3>
 * <ul>
 *   <li>Document preprocessing in RAG systems</li>
 *   <li>Vectorized indexing of long texts</li>
 *   <li>Structural processing of code documents</li>
 *   <li>Intelligent analysis of multimodal documents</li>
 * </ul>
 *
 * <h3>Builder Pattern Usage Example:</h3>
 * <pre>{@code
 * // Paragraph-based chunking
 * Chunker chunker = Chunker.paragraphs()
 *     .maxChars(800)
 *     .overlap(100)
 *     .build();
 *
 * // Token-controlled chunking
 * Chunker tokenChunker = Chunker.tokenWindow()
 *     .maxTokens(500)
 *     .overlapTokens(50)
 *     .tokenizer(customTokenizer)
 *     .build();
 *
 * List<String> chunks = chunker.split(longText);
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
public final class Chunker {
    public static Builder builder() {
        return new Builder();
    }

    // Static convenient entry (returns a Builder with pre-selected strategy)
    public static Builder recursive() {
        return builder().byRecursiveSeparators();
    }

    public static Builder sentence() {
        return builder().bySentenceBoundary();
    }

    public static Builder fixedWindow() {
        return builder().byFixedWindow();
    }

    public static Builder paragraphs() {
        return builder().byParagraphs();
    }

    public static Builder markdown() {
        return builder().byMarkdownHeaders();
    }

    public static Builder codeJava() {
        return builder().byCodeJava();
    }

    public static Builder jsonTopLevel() {
        return builder().byJsonTopLevel();
    }

    public static Builder tokenWindow() {
        return builder().byTokenWindow();
    }

    public static final class Builder {
        private ChunkingStrategy strategy = new RecursiveSeparatorsStrategy(); // default
        private int maxChars = 800;
        private int overlapChars = 100;
        private int maxTokens = -1;
        private int overlapTokens = -1;
        private Tokenizer tokenizer;
        private boolean attachHeader = false;
        private int headerDepth = 3;

        // Eight strategy selectors
        public Builder byRecursiveSeparators() {
            this.strategy = new RecursiveSeparatorsStrategy();
            return this;
        }

        public Builder bySentenceBoundary() {
            this.strategy = new SentenceStrategy();
            return this;
        }

        public Builder byFixedWindow() {
            this.strategy = new FixedWindowStrategy();
            return this;
        }

        public Builder byParagraphs() {
            this.strategy = new ParagraphStrategy();
            return this;
        }

        public Builder byMarkdownHeaders() {
            this.strategy = new MarkdownHeaderStrategy();
            return this;
        }

        public Builder byCodeJava() {
            this.strategy = new CodeJavaStrategy();
            return this;
        }

        public Builder byJsonTopLevel() {
            this.strategy = new JsonTopLevelStrategy();
            return this;
        }

        public Builder byTokenWindow() {
            this.strategy = new TokenWindowStrategy();
            return this;
        }

        // Common parameters
        public Builder maxChars(int n) {
            this.maxChars = n;
            return this;
        }

        public Builder overlap(int n) {
            this.overlapChars = n;
            return this;
        }

        public Builder tokenizer(Tokenizer tk) {
            this.tokenizer = tk;
            return this;
        }

        public Builder maxTokens(int n) {
            this.maxTokens = n;
            return this;
        }

        public Builder overlapTokens(int n) {
            this.overlapTokens = n;
            return this;
        }

        public Builder attachHeader(boolean b) {
            this.attachHeader = b;
            return this;
        }

        public Builder headerDepth(int d) {
            this.headerDepth = d;
            return this;
        }

        public Chunker build() {
            return new Chunker(strategy, new Cfg(this));
        }
    }

    private final ChunkingStrategy strategy;
    private final Cfg cfg;

    private Chunker(ChunkingStrategy s, Cfg c) {
        this.strategy = s;
        this.cfg = c;
    }

    // Only expose one split method externally
    public List<String> split(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        return strategy.chunk(text, cfg);
    }

    // Internal interface
    interface ChunkingStrategy {
        List<String> chunk(String text, Cfg c);
    }

    // Internal configuration (copied from Builder, not visible externally)
    static final class Cfg {
        final int maxChars, overlapChars, maxTokens, overlapTokens, headerDepth;
        final Tokenizer tokenizer;
        final boolean attachHeader;

        Cfg(Builder b) {
            this.maxChars = b.maxChars;
            this.overlapChars = b.overlapChars;
            this.maxTokens = b.maxTokens;
            this.overlapTokens = b.overlapTokens;
            this.tokenizer = b.tokenizer;
            this.attachHeader = b.attachHeader;
            this.headerDepth = b.headerDepth;
        }

        boolean tokenMode() {
            return tokenizer != null && maxTokens > 0;
        }

        int limit() {
            return tokenMode() ? maxTokens : maxChars;
        }

        int sizeOf(String s) {
            return tokenMode() ? tokenizer.count(s) : s.length();
        }
    }

    // Unified packer (controls size and overlap)
    static final class Repacker {
        static List<String> pack(List<String> units, Cfg c) {
            List<String> out = new ArrayList<>();
            StringBuilder buf = new StringBuilder();
            for (String u : units) {
                if (exceed(buf, u, c)) {
                    out.add(buf.toString());
                    buf = new StringBuilder(overlapTail(out.get(out.size() - 1), c));
                }
                if (buf.length() > 0 && !buf.toString().endsWith("\n")) {
                    buf.append("\n");
                }
                buf.append(u);
            }
            if (buf.length() > 0) {
                out.add(buf.toString());
            }
            return out;
        }

        private static boolean exceed(StringBuilder buf, String add, Cfg c) {
            if (c.tokenMode()) {
                return c.tokenizer.count(buf.toString() + add) > c.maxTokens;
            }
            return buf.length() + add.length() > c.maxChars;
        }

        private static String overlapTail(String chunk, Cfg c) {
            if (chunk.isEmpty()) {
                return "";
            }
            if (c.tokenMode() && c.overlapTokens > 0) {
                int n = Math.min(chunk.length(), 64); // Simplified: can be replaced by actual token-based tail truncation
                return chunk.substring(chunk.length() - n);
            }
            int k = Math.min(c.overlapChars, chunk.length());
            return chunk.substring(chunk.length() - k);
        }
    }

    // 1) Recursive separators
    static final class RecursiveSeparatorsStrategy implements ChunkingStrategy {
        // For English and Chinese punctuation, including full stop, exclamation, question marks in both languages
        private static final List<String> SEPS = List.of("\n\n", ".", "!", "?", "\n");

        @Override
        public List<String> chunk(String text, Cfg c) {
            List<String> units = splitRec(text, SEPS, c);
            return Repacker.pack(units, c);
        }

        private List<String> splitRec(String t, List<String> seps, Cfg c) {
            if (c.sizeOf(t) <= c.limit()) {
                return List.of(t);
            }
            if (seps.isEmpty()) {
                return hardCut(t, c.limit());
            }
            String sep = seps.get(0);
            List<String> rest = seps.subList(1, seps.size());
            List<String> out = new ArrayList<>();
            int i, last = 0;
            while ((i = t.indexOf(sep, last)) >= 0) {
                out.addAll(splitRec(t.substring(last, i), rest, c));
                last = i + sep.length();
            }
            out.addAll(splitRec(t.substring(last), rest, c));
            return out;
        }

        private List<String> hardCut(String t, int limit) {
            List<String> out = new ArrayList<>();
            for (int i = 0; i < t.length(); i += Math.max(1, limit)) {
                out.add(t.substring(i, Math.min(i + limit, t.length())));
            }
            return out;
        }
    }

    // 2) Sentence boundary
    static final class SentenceStrategy implements ChunkingStrategy {
        @Override
        public List<String> chunk(String text, Cfg c) {
            List<String> sentences = new ArrayList<>();
            BreakIterator it = BreakIterator.getSentenceInstance(Locale.CHINA);
            it.setText(text);
            int start = it.first();
            for (int end = it.next(); end != BreakIterator.DONE; start = end, end = it.next()) {
                String s = text.substring(start, end).trim();
                if (!s.isEmpty()) {
                    sentences.add(s);
                }
            }
            return Repacker.pack(sentences, c);
        }
    }

    // 3) Fixed window (character/approximate token)
    static final class FixedWindowStrategy implements ChunkingStrategy {
        @Override
        public List<String> chunk(String text, Cfg c) {
            List<String> out = new ArrayList<>();
            if (c.tokenMode()) {
                int step = Math.max(1, c.maxTokens - Math.max(0, c.overlapTokens));
                for (int i = 0; i < text.length(); i += step) {
                    out.add(text.substring(i, Math.min(i + step, text.length())));
                    if (i + step >= text.length()) {
                        break;
                    }
                }
            } else {
                int step = Math.max(1, c.maxChars - Math.max(0, c.overlapChars));
                for (int i = 0; i < text.length(); i += step) {
                    out.add(text.substring(i, Math.min(i + c.maxChars, text.length())));
                    if (i + c.maxChars >= text.length()) {
                        break;
                    }
                }
            }
            return out;
        }
    }

    // 4) Paragraph
    static final class ParagraphStrategy implements ChunkingStrategy {
        @Override
        public List<String> chunk(String text, Cfg c) {
            String[] paras = text.split("\\n\\s*\\n+");
            List<String> units = Arrays.stream(paras).map(String::trim).filter(s -> !s.isEmpty()).toList();
            return Repacker.pack(units, c);
        }
    }

    // 5) Markdown headers
    static final class MarkdownHeaderStrategy implements ChunkingStrategy {
        @Override
        public List<String> chunk(String text, Cfg c) {
            List<String> units = new ArrayList<>();
            String[] lines = text.split("\\n");
            StringBuilder section = new StringBuilder();
            String currentHeader = "";
            for (String line : lines) {
                int level = headerLevel(line);
                if (level > 0 && level <= c.headerDepth) {
                    if (section.length() > 0) {
                        units.add(prefix(c, currentHeader) + section.toString().trim());
                        section.setLength(0);
                    }
                    currentHeader = line.trim();
                } else {
                    section.append(line).append("\n");
                }
            }
            if (section.length() > 0) {
                units.add(prefix(c, currentHeader) + section.toString().trim());
            }
            return Repacker.pack(units, c);
        }

        private int headerLevel(String line) {
            int i = 0;
            while (i < line.length() && line.charAt(i) == '#') {
                i++;
            }
            return (i > 0 && line.startsWith("#")) ? i : 0;
        }

        private String prefix(Cfg c, String header) {
            return c.attachHeader && !header.isEmpty() ? header + "\n" : "";
        }
    }

    // 6) Java code (simplified)
    static final class CodeJavaStrategy implements ChunkingStrategy {
        private static final Pattern BORDER = Pattern.compile("^\\s*(public|private|protected|class|interface|enum)\\b.*\\{?$");

        @Override
        public List<String> chunk(String text, Cfg c) {
            List<String> units = new ArrayList<>();
            String[] lines = text.split("\\n");
            StringBuilder buf = new StringBuilder();
            for (String line : lines) {
                if (BORDER.matcher(line).find() && buf.length() > 0) {
                    units.add(buf.toString().trim());
                    buf.setLength(0);
                }
                buf.append(line).append("\n");
            }
            if (buf.length() > 0) {
                units.add(buf.toString().trim());
            }
            // Hard cut if too long
            List<String> normalized = new ArrayList<>();
            for (String u : units) {
                if (c.sizeOf(u) <= c.limit()) {
                    normalized.add(u);
                } else {
                    normalized.addAll(new RecursiveSeparatorsStrategy().hardCut(u, c.limit()));
                }
            }
            return Repacker.pack(normalized, c);
        }
    }

    // 7) JSON top level (simplified scanning)
    static final class JsonTopLevelStrategy implements ChunkingStrategy {
        @Override
        public List<String> chunk(String text, Cfg c) {
            List<String> units = new ArrayList<>();
            int depth = 0, start = -1;
            boolean inStr = false;
            char prev = 0;
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                if (ch == '"' && prev != '\\') {
                    inStr = !inStr;
                }
                if (inStr) {
                    prev = ch;
                    continue;
                }
                if (ch == '{' || ch == '[') {
                    if (depth == 0) {
                        start = i;
                    }
                    depth++;
                } else if (ch == '}' || ch == ']') {
                    if (--depth == 0 && start >= 0) {
                        units.add(text.substring(start, i + 1));
                        start = -1;
                    }
                }
                prev = ch;
            }
            if (units.isEmpty()) {
                units = List.of(text);
            }
            return Repacker.pack(units, c);
        }
    }

    // 8) Token sliding window (fallback to FixedWindow when no tokenizer)
    static final class TokenWindowStrategy implements ChunkingStrategy {
        @Override
        public List<String> chunk(String text, Cfg c) {
            if (!c.tokenMode()) {
                return new FixedWindowStrategy().chunk(text, c);
            }
            List<String> out = new ArrayList<>();
            int stepTokens = Math.max(1, c.maxTokens - Math.max(0, c.overlapTokens));
            // Simplified: approximate by character; can be replaced with real token list sliding window in production
            for (int i = 0; i < text.length(); i += stepTokens) {
                int end = Math.min(i + c.maxTokens, text.length());
                out.add(text.substring(i, end));
                if (end == text.length()) {
                    break;
                }
            }
            return out;
        }
    }
}

