package com.jd.oxygent.core.oxygent.mcpservers;

import com.jd.oxygent.core.oxygent.mcpservers.annotation.EnableMcpServer;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.MCPTool;
import com.jd.oxygent.core.oxygent.mcpservers.annotation.ToolParam;
import com.jd.oxygent.core.oxygent.mcpservers.engine.McpServer;
import io.github.whitemagic2014.tts.TTS;
import io.github.whitemagic2014.tts.TTSVoice;
import io.github.whitemagic2014.tts.bean.TransRecord;
import io.github.whitemagic2014.tts.bean.Voice;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Java TTS tool class providing text-to-speech functionality
 * Corresponding to the Python version tts_tools.py functionality, using the correct io.github.whitemagic2014.tts library
 * Supports MCP protocol service
 */
@Slf4j
public class TtsTools {

    // Fixed configuration parameters
    private static final String FIXED_AUDIO_DIR = "./tts_audio_cache";
    private static final int FIXED_CHUNK_SIZE = 1200;    // Fixed chunk size
    private static final int MIN_CHUNK_SIZE = 50;        // Minimum chunk size
    private static final int MAX_CACHE_FILES = 50;       // Maximum number of cache files
    private static final int CACHE_RETENTION_HOURS = 168; // Cache retention time (hours)
    private static final int MAX_RETRIES = 3;            // Maximum number of retries
    private static final long INITIAL_RETRY_DELAY_MS = 1000L; // Initial retry delay (milliseconds)

    // Static initialization of cache directory
    static {
        new File(FIXED_AUDIO_DIR).mkdirs();
    }

    /**
     * Cache entry class
     */
    public static class CacheEntry {
        public String fileId;         // Unique identifier for the cache entry
        public String textHash;       // Hash of the text content
        public String voice;          // Voice name used for synthesis
        public String filePath;       // Path to the cached audio file
        public LocalDateTime createdAt; // Timestamp when the entry was created
        public long fileSize;         // Size of the audio file in bytes
        public String textPreview;    // Preview of the text content
        public int playbackCount = 0; // Number of times the audio has been played
        public LocalDateTime lastPlayed; // Timestamp when the audio was last played

        public CacheEntry(String fileId, String textHash, String voice, String filePath,
                          LocalDateTime createdAt, long fileSize, String textPreview) {
            this.fileId = fileId;
            this.textHash = textHash;
            this.voice = voice;
            this.filePath = filePath;
            this.createdAt = createdAt;
            this.fileSize = fileSize;
            this.textPreview = textPreview;
        }
    }

    /**
     * Audio cache management class
     */
    public static class AudioCache {
        private Map<String, CacheEntry> entries = new HashMap<>(); // Map of file IDs to cache entries
        private String cacheIndexFile = FIXED_AUDIO_DIR + "/cache_index.json"; // Path to cache index file

        public AudioCache() {
            loadCacheIndex();
        }

        private void loadCacheIndex() {
            Path indexPath = Paths.get(cacheIndexFile);
            if (Files.exists(indexPath)) {
                try (FileReader reader = new FileReader(cacheIndexFile)) {
                    // Gson or other JSON library can be used here for parsing, but we'll skip it for simplicity
                    // In production, it's recommended to use a JSON library for cache index handling
                } catch (IOException e) {
                    log.error("Error loading cache index: {}", e.getMessage());
                }
            }
        }

        private void saveCacheIndex() {
            try (FileWriter writer = new FileWriter(cacheIndexFile)) {
                // Gson or other JSON library can be used here for serialization, but we'll skip it for simplicity
                // In production, it's recommended to use a JSON library for cache index handling
            } catch (IOException e) {
                log.error("Error saving cache index: {}", e.getMessage());
            }
        }

        private String generateTextHash(String text, String voice) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                String content = text + ":" + voice;
                byte[] hashBytes = md.digest(content.getBytes("UTF-8"));
                StringBuilder sb = new StringBuilder();
                for (byte b : hashBytes) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (Exception e) {
                return UUID.randomUUID().toString();
            }
        }

        private void cleanupExpiredEntries() {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(CACHE_RETENTION_HOURS);
            Iterator<Map.Entry<String, CacheEntry>> iterator = entries.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, CacheEntry> entry = iterator.next();
                if (entry.getValue().createdAt.isBefore(cutoffTime)) {
                    // Delete corresponding file
                    try {
                        Files.deleteIfExists(Paths.get(entry.getValue().filePath));
                    } catch (IOException e) {
                        log.error("Error deleting cached file: {}", e.getMessage());
                    }
                    iterator.remove();
                }
            }
        }

        private void cleanupExcessFiles() {
            if (entries.size() <= MAX_CACHE_FILES) {
                return;
            }

            // Sort by creation time, delete oldest entries
            List<Map.Entry<String, CacheEntry>> sortedEntries = new ArrayList<>(entries.entrySet());
            sortedEntries.sort((a, b) -> a.getValue().createdAt.compareTo(b.getValue().createdAt));

            int excessCount = entries.size() - MAX_CACHE_FILES;
            for (int i = 0; i < excessCount; i++) {
                Map.Entry<String, CacheEntry> entry = sortedEntries.get(i);
                try {
                    Files.deleteIfExists(Paths.get(entry.getValue().filePath));
                } catch (IOException e) {
                    log.error("Error deleting cached file: {}", e.getMessage());
                }
                entries.remove(entry.getKey());
            }
        }

        public CacheEntry findCachedAudio(String text, String voice) {
            cleanupExpiredEntries();
            String textHash = generateTextHash(text, voice);

            for (CacheEntry entry : entries.values()) {
                if (entry.textHash.equals(textHash) && Files.exists(Paths.get(entry.filePath))) {
                    return entry;
                }
            }
            return null;
        }

        public String addToCache(String text, String voice, String filePath) {
            cleanupExpiredEntries();
            cleanupExcessFiles();

            String fileId = UUID.randomUUID().toString();
            String textHash = generateTextHash(text, voice);

            // Copy file to cache directory
            String cachedFilePath = FIXED_AUDIO_DIR + "/" + fileId + ".mp3";
            try {
                Files.copy(Paths.get(filePath), Paths.get(cachedFilePath));
            } catch (IOException e) {
                log.error("Error copying file to cache: {}", e.getMessage());
                return null;
            }

            CacheEntry entry = new CacheEntry(
                    fileId,
                    textHash,
                    voice,
                    cachedFilePath,
                    LocalDateTime.now(),
                    new File(cachedFilePath).length(),
                    text.length() > 50 ? text.substring(0, 50) + "..." : text
            );

            entries.put(fileId, entry);
            saveCacheIndex();
            return fileId;
        }

        public void updatePlaybackStats(String fileId) {
            CacheEntry entry = entries.get(fileId);
            if (entry != null) {
                entry.playbackCount++;
                entry.lastPlayed = LocalDateTime.now();
                saveCacheIndex();
            }
        }
    }

    // Global cache instance
    private static final AudioCache audioCache = new AudioCache();

    /**
     * Convert text to speech and save to file (with caching functionality)
     *
     * @param text Text to convert
     * @param voiceName Voice name (e.g., zh-CN-XiaoyiNeural)
     * @param outputFile Output file path
     * @return Whether conversion was successful
     */
    public static boolean textToSpeech(String text, String voiceName, String outputFile) {
        // First check cache
        CacheEntry cachedEntry = audioCache.findCachedAudio(text, voiceName);
        if (cachedEntry != null) {
            log.info("Found cached audio: {}", cachedEntry.fileId);
            // Update playback statistics
            audioCache.updatePlaybackStats(cachedEntry.fileId);

            // Copy cached file to target location
            try {
                Files.copy(Paths.get(cachedEntry.filePath), Paths.get(outputFile));
                log.info("Playing cached audio");
                return true;
            } catch (IOException e) {
                log.error("Error using cached audio: {}", e.getMessage());
            }
        }

        // Cache not found, perform new conversion
        return synthesizeTextToSpeech(text, voiceName, outputFile);
    }

    /**
     * Perform text-to-speech conversion (without cache check)
     */
    private static boolean synthesizeTextToSpeech(String text, String voiceName, String outputFile) {
        try {
            // Find voice
            Optional<Voice> voiceOptional = getVoiceByName(voiceName);

            if (!voiceOptional.isPresent()) {
                throw new IllegalStateException("Voice not found: " + voiceName);
            }

            Voice voice = voiceOptional.get();

            // Check text length, process in chunks if too long
            if (text.length() <= FIXED_CHUNK_SIZE) {
                // Process short text directly
                return synthesizeSingleChunk(voice, text, outputFile);
            } else {
                // Process long text with chunks
                boolean result = textToSpeechWithChunks(text, voice, outputFile);
                if (result) {
                    // Add to cache
                    audioCache.addToCache(text, voiceName, outputFile);
                }
                return result;
            }
        } catch (Exception e) {
            log.error("Error in text to speech conversion: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get voice by specified name
     */
    private static Optional<Voice> getVoiceByName(String voiceName) {
        return TTSVoice.provides()
                .stream()
                .filter(v -> voiceName.equals(v.getShortName()))
                .findFirst();
    }

    /**
     * Synthesize single text chunk
     */
    private static boolean synthesizeSingleChunk(Voice voice, String text, String outputFile) {
        TTS tts = new TTS(voice, text)
                .isRateLimited(true)
                .fileName(getFileNameWithoutExtension(outputFile))
                .overwrite(true)
                .formatMp3();

        tts.trans();

        // Add to cache
        String voiceName = voice.getShortName();
        audioCache.addToCache(text, voiceName, outputFile);
        return true;
    }

    /**
     * Extract file name from path (without extension)
     */
    private static String getFileNameWithoutExtension(String filePath) {
        int lastSlashIndex = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex > lastSlashIndex) {
            return filePath.substring(lastSlashIndex + 1, lastDotIndex);
        } else {
            return filePath.substring(lastSlashIndex + 1);
        }
    }

    /**
     * Smart text splitting method (similar to Python version)
     *
     * @param text Text to split
     * @return List of split text chunks
     */
    private static List<String> smartTextSplit(String text) {
        List<String> chunks = new ArrayList<>();

        if (text.length() <= FIXED_CHUNK_SIZE) {
            chunks.add(text);
            return chunks;
        }

        // Split sentences using regular expression
        Pattern sentencePattern = Pattern.compile("([。！？.!\\?])");
        String[] parts = sentencePattern.split(text);
        String[] punctuations = text.split("[^。！？.!\\?]+");

        List<String> sentenceList = buildSentencesWithPunctuation(parts, punctuations);

        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentenceList) {
            if ((currentChunk.length() + sentence.length()) <= FIXED_CHUNK_SIZE) {
                currentChunk.append(sentence);
            } else {
                // Current chunk is full, save it
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                }

                // 如果单个句子太长，按逗号分割
                if (sentence.length() > FIXED_CHUNK_SIZE) {
                    List<String> subChunks = splitLongSentence(sentence);
                    for (String subChunk : subChunks) {
                        chunks.add(subChunk);
                    }
                } else {
                    currentChunk.append(sentence);
                }
            }
        }

        // Add last chunk
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        return filterShortChunks(chunks);
    }

    /**
     * Build list of sentences with punctuation
     */
    private static List<String> buildSentencesWithPunctuation(String[] parts, String[] punctuations) {
        List<String> sentenceList = new ArrayList<>();

        int partIdx = 0;
        int punctIdx = 0;

        while (partIdx < parts.length) {
            String part = parts[partIdx].trim();
            if (!part.isEmpty()) {
                String sentence = part;

                // Try to add next punctuation
                if (punctIdx < punctuations.length - 1) {
                    punctIdx++; // Move to next punctuation (first one is empty string)
                    if (punctIdx < punctuations.length) {
                        sentence += punctuations[punctIdx];
                    }
                }

                sentenceList.add(sentence);
            }
            partIdx++;
        }

        return sentenceList;
    }

    /**
     * Split overly long sentences
     */
    private static List<String> splitLongSentence(String sentence) {
        List<String> subChunks = new ArrayList<>();

        // Split by commas, semicolons, etc.
        String[] commaParts = sentence.split("([，；,;])");
        StringBuilder tempChunk = new StringBuilder();

        for (String part : commaParts) {
            if ((tempChunk.length() + part.length()) <= FIXED_CHUNK_SIZE) {
                if (tempChunk.length() > 0) {
                    tempChunk.append(","); // Add separator
                }
                tempChunk.append(part);
            } else {
                if (tempChunk.length() > 0) {
                    subChunks.add(tempChunk.toString());
                }
                tempChunk = new StringBuilder(part);

                // If single part is still too long, force split by characters
                if (tempChunk.length() > FIXED_CHUNK_SIZE) {
                    List<String> charChunks = forceSplitByCharacters(tempChunk.toString());
                    subChunks.addAll(charChunks);
                    tempChunk = new StringBuilder(); // Clear, as it's been processed
                }
            }
        }

        if (tempChunk.length() > 0) {
            subChunks.add(tempChunk.toString());
        }

        return subChunks;
    }

    /**
     * Force split extra-long text by characters
     */
    private static List<String> forceSplitByCharacters(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + FIXED_CHUNK_SIZE, text.length());
            chunks.add(text.substring(start, end));
            start = end;
        }

        return chunks;
    }

    /**
     * Filter out short chunks (except the last one)
     */
    private static List<String> filterShortChunks(List<String> chunks) {
        List<String> filteredChunks = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i).trim();
            if (chunk.length() >= MIN_CHUNK_SIZE || i == chunks.size() - 1) {
                filteredChunks.add(chunk);
            } else if (!filteredChunks.isEmpty()) {
                // Merge short chunk to previous chunk
                String lastChunk = filteredChunks.get(filteredChunks.size() - 1);
                filteredChunks.set(filteredChunks.size() - 1, lastChunk + " " + chunk);
            } else {
                filteredChunks.add(chunk);
            }
        }

        return filteredChunks;
    }

    /**
     * Audio synthesis with retry mechanism
     */
    private static boolean synthesizeChunkWithRetry(Voice voice, String text, String outputFile, int chunkIndex) {
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                // Clean up text
                String cleanedText = text.trim();
                if (cleanedText.isEmpty()) {
                    log.warn("Warning: Empty text, skipping synthesis");
                    return false;
                }

                TTS tts = new TTS(voice, cleanedText)
                        .isRateLimited(true)
                        .fileName(outputFile.replace(".mp3", ""))
                        .overwrite(true)
                        .formatMp3();

                tts.trans();

                // Verify file was created successfully
                File audioFile = new File(outputFile);
                if (audioFile.exists() && audioFile.length() > 0) {
                    log.info("Chunk {} completed", chunkIndex);
                    return true;
                } else {
                    throw new IOException("Generated audio file is empty or does not exist");
                }
            } catch (Exception e) {
                if (attempt < MAX_RETRIES) {
                    log.warn("Attempt {} failed for chunk {}: {}", attempt + 1, chunkIndex, e.getMessage());
                    log.warn("Retrying in a moment...");
                    try {
                        Thread.sleep(INITIAL_RETRY_DELAY_MS * (attempt + 1)); // Increasing delay
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                } else {
                    log.error("Synthesis failed for chunk {} after {} retries: {}", chunkIndex, MAX_RETRIES, e.getMessage());
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Process long text with chunking
     */
    private static boolean textToSpeechWithChunks(String text, Voice voice, String outputFile) {
        try {
            List<String> chunks = smartTextSplit(text);
            log.info("Split into {} chunks for processing (fixed chunk size: {})", chunks.size(), FIXED_CHUNK_SIZE);

            if (chunks.size() == 1) {
                // If only one chunk, process directly
                return synthesizeChunkWithRetry(voice, chunks.get(0), outputFile, 1);
            }

            // Create temporary directory to store chunk files
            String tempDir = FIXED_AUDIO_DIR + "/temp_" + System.currentTimeMillis();
            new File(tempDir).mkdirs();

            List<String> tempFiles = new ArrayList<>();
            List<CompletableFuture<Boolean>> futures = new ArrayList<>();
            ExecutorService executor = Executors.newFixedThreadPool(Math.min(chunks.size(), 4)); // Maximum 4 concurrent

            try {
                // Process each chunk in parallel
                for (int i = 0; i < chunks.size(); i++) {
                    final int index = i;
                    String chunk = chunks.get(i);

                    CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                        String tempFile = tempDir + "/chunk_" + String.format("%03d", index + 1) + ".mp3";
                        return synthesizeChunkWithRetry(voice, chunk, tempFile, index + 1);
                    }, executor);

                    futures.add(future);
                }

                // Wait for all tasks to complete and collect results
                List<Boolean> results = new ArrayList<>();
                for (CompletableFuture<Boolean> f : futures) {
                    results.add(f.join());
                }

                // Collect successful files
                for (int i = 0; i < results.size(); i++) {
                    if (results.get(i)) {
                        String tempFile = tempDir + "/chunk_" + String.format("%03d", i + 1) + ".mp3";
                        if (new File(tempFile).exists()) {
                            tempFiles.add(tempFile);
                        }
                    }
                }

                // Check if there are failed tasks
                if (tempFiles.size() != chunks.size()) {
                    log.error("Some chunks failed to process. Success: {}, Expected: {}", tempFiles.size(), chunks.size());
                    return false;
                }

                // Merge audio files
                return mergeAudioFiles(tempFiles, outputFile);

            } finally {
                executor.shutdown();
                // Clean up temporary files
                cleanupTempFiles(tempDir);
            }
        } catch (Exception e) {
            log.error("Error processing long text: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Merge multiple audio files
     */
    private static boolean mergeAudioFiles(List<String> audioFiles, String outputFile) {
        try {
            try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                for (String audioFile : audioFiles) {
                    if (new File(audioFile).exists()) {
                        try (FileInputStream inputStream = new FileInputStream(audioFile)) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                        }
                    }
                }
            }

            log.info("Audio files merged successfully to: {}", outputFile);
            return true;
        } catch (IOException e) {
            log.error("Error merging audio files: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Clean up temporary files
     */
    private static void cleanupTempFiles(String tempDir) {
        try {
            File tempDirFile = new File(tempDir);
            if (tempDirFile.exists()) {
                File[] files = tempDirFile.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            file.delete();
                        }
                    }
                }
                tempDirFile.delete();
            }
        } catch (Exception e) {
            log.error("Error cleaning up temp files: {}", e.getMessage(), e);
        }
    }

    /**
     * Get list of available voices
     */
    public static List<Voice> getAvailableVoices(String languageFilter) {
        List<Voice> allVoices = TTSVoice.provides();

        if (languageFilter == null || languageFilter.isEmpty()) {
            return allVoices;
        }

        List<Voice> filteredVoices = new ArrayList<>();
        for (Voice voice : allVoices) {
            if (voice.getLocale().toLowerCase().contains(languageFilter.toLowerCase())) {
                filteredVoices.add(voice);
            }
        }

        return filteredVoices;
    }

    /**
     * Get list of Chinese voices
     */
    public static List<Voice> getChineseVoices() {
        return getAvailableVoices("zh");
    }

    /**
     * Get list of English voices
     */
    public static List<Voice> getEnglishVoices() {
        return getAvailableVoices("en");
    }

    /**
     * Batch process multiple text records
     */
    public static boolean batchTextToSpeech(List<TransRecord> records, String voiceName, String storeDir) {
        try {
            Optional<Voice> voiceOptional = getVoiceByName(voiceName);

            if (!voiceOptional.isPresent()) {
                throw new IllegalStateException("Voice not found: " + voiceName);
            }

            Voice voice = voiceOptional.get();

            // 创建批量TTS实例
            new TTS(voice)
                    .findHeadHook()
                    .isRateLimited(true)
                    .overwrite(true)
                    .batch(records)
                    .parallel(4) // 4个并行线程
                    .storage(storeDir)
                    .formatMp3()
                    .batchTrans(); // 执行批量转换

            // 验证文件是否都生成成功
            for (TransRecord record : records) {
                Path path = Paths.get(storeDir + "/" + record.getFilename() + ".mp3");
                if (!Files.exists(path)) {
                    log.error("Missing output file: {}", path.toString());
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("Error in batch text to speech: {}", e.getMessage(), e);
            return false;
        }
    }

    // MCP service related methods
    /**
     * Convert text to speech via MCP service
     *
     * @param text Text to convert
     * @param voice Voice name
     * @return Operation result
     */
    @MCPTool(name = "text_to_speech", description = "Play text as speech with automatic caching")
    public Map<String, Object> textToSpeechViaMCP(
            @ToolParam(description = "Text to convert to speech and play") String text,
            @ToolParam(description = "Voice ID (e.g., 'zh-CN-XiaoxiaoNeural', 'en-US-AriaNeural')", defaultValue = "zh-CN-XiaoxiaoNeural") String voice) {
        String outputFile = FIXED_AUDIO_DIR + "/" + UUID.randomUUID() + ".mp3";
        boolean success = textToSpeech(text, voice, outputFile);

        Map<String, Object> result = new HashMap<>();
        if (success) {
            result.put("status", "success");
            result.put("output_file", outputFile);
            result.put("message", "Text to speech conversion completed successfully");
        } else {
            result.put("status", "error");
            result.put("message", "Text to speech conversion failed");
        }

        return result;
    }

    /**
     * Get available voices via MCP service
     *
     * @param languageFilter Language filter
     * @return Voice list
     */
    @MCPTool(name = "get_available_voices", description = "Get available TTS voices with optional language filtering")
    public Map<String, Object> getAvailableVoicesViaMCP(
            @ToolParam(description = "Filter voices by language (e.g., 'zh', 'en', 'zh-CN'). Leave empty to show all voices.", required = false) String languageFilter) {
        if (languageFilter == null) {
            languageFilter = "";
        }
        List<Voice> voices = getAvailableVoices(languageFilter);

        List<Map<String, Object>> voiceList = new ArrayList<>();
        for (Voice v : voices) {
            Map<String, Object> voiceInfo = new HashMap<>();
            voiceInfo.put("short_name", v.getShortName());
            voiceInfo.put("locale", v.getLocale());
            voiceInfo.put("name", v.getShortName()); // 使用short name作为name
            voiceList.add(voiceInfo);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("voices", voiceList);
        result.put("count", voiceList.size());
        result.put("language_filter", languageFilter);

        return result;
    }

    /**
     * Get Chinese voices via MCP
     */
    @MCPTool(name = "get_chinese_voices", description = "Get available Chinese TTS voices")
    public Map<String, Object> getChineseVoicesViaMCP() {
        return getAvailableVoicesViaMCP("");
    }

    /**
     * Get English voices via MCP
     */
    @MCPTool(name = "get_english_voices", description = "Get available English TTS voices")
    public Map<String, Object> getEnglishVoicesViaMCP() {
        return getAvailableVoicesViaMCP("");
    }

    /**
     * Main method to start MCP server
     *
     * @param args Command line arguments
     */
    @EnableMcpServer(mode = "stdio")
    public static void main(String[] args) {
        McpServer.start();
    }

//    // Main method for testing
//    public static void main(String[] args) {
//        // Example: Convert text to speech
//        String text = "你好，这是一条测试语音。今天天气不错，适合学习新技术。";
//        String voiceName = "zh-CN-XiaoyiNeural";
//        String outputFile = "./output_test.mp3";
//
//        log.info("Starting text to speech conversion...");
//        boolean success = textToSpeech(text, voiceName, outputFile);
//
//        if (success) {
//            log.info("Speech conversion successful! File saved to: {}", outputFile);
//        } else {
//            log.info("Speech conversion failed!");
//        }
//
//        // Example: Get Chinese voices
//        log.info("\nAvailable Chinese voices:");
//        List<Voice> chineseVoices = getChineseVoices();
//        for (int i = 0; i < Math.min(5, chineseVoices.size()); i++) { // Show only first 5
//            Voice v = chineseVoices.get(i);
//            log.info("- {}: {} ({})", v.getShortName(), v.getShortName(), v.getLocale());
//        }
//
//        // Example: Batch processing
//        log.info("\nExecuting batch processing example...");
//        List<TransRecord> records = new ArrayList<>();
//        for (int i = 0; i < 3; i++) {
//            TransRecord record = new TransRecord();
//            record.setContent(i + ", 你好，这是一条测试语音，第" + (i+1) + "次。");
//            record.setFilename(i + "_test_batch");
//            records.add(record);
//        }
//
//        boolean batchSuccess = batchTextToSpeech(records, voiceName, FIXED_AUDIO_DIR);
//        if (batchSuccess) {
//            log.info("Batch processing successful!");
//        } else {
//            log.info("Batch processing failed!");
//        }
//    }

}