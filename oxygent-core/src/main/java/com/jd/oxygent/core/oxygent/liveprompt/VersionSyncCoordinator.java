package com.jd.oxygent.core.oxygent.liveprompt;

import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.infra.databases.BaseEs;
import com.jd.oxygent.core.oxygent.infra.impl.databases.es.LocalEs;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.factory.impl.platform.spring.ApplicationContextHolder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Coordinates version synchronization across multiple instances.
 *
 *     This class provides ES polling mechanism for maintaining cache consistency
 *     across multiple instances by periodically checking version updates in ES.
 *
 *     Attributes:
 *         prompt_manager: The PromptManager instance to synchronize
 *         use_es_polling: Whether to use ES polling
 *         polling_task: Async task for ES polling (if enabled)
 *         polling_interval: Interval in seconds for ES polling
 *
 * @author OxyGent Team
 * @version 1.0.10.4
 * @since 1.0.10.4
 */
@Slf4j
public class VersionSyncCoordinator {
    private static VersionSyncCoordinator instance;

    private PromptManager promptManager;
    private BaseEs esClient;

    private int pollingInterval = Config.getLivePrompt().getEsPollingInterval();
    private boolean active = false;
    private ScheduledExecutorService pollingExecutor;

    private boolean isRunning = false;
    private Map<String, Integer> localVersions = new ConcurrentHashMap<>();  // Track local versions
    private Map<String, Set<Integer>> pendingUpdates = new ConcurrentHashMap<>();  // Track pending updates: {promptKey: {version, ...}}

    public VersionSyncCoordinator(PromptManager promptManager, Integer pollingInterval) {
        this.promptManager = promptManager;
        esClient = ApplicationContextHolder.getBean(BaseEs.class);
        if (esClient == null) {
            esClient = new LocalEs();
        }
        // Read polling interval from config, or use default value
        if (pollingInterval != null) {
            this.pollingInterval = pollingInterval;
        }
        this.detectSyncMechanisms();
    }

    private void detectSyncMechanisms() {
        if (esClient instanceof LocalEs) {
            log.info("Local ES detected, polling disabled for multi-instance sync");
        } else {
            this.active = Config.getLivePrompt().isActive();
            log.info("ES polling enabled for remote hosts");
        }
    }

    public void start() {
        if (isRunning) {
            log.warn("Version sync coordinator is already running");
            return;
        }

        isRunning = true;

        // Initialize local versions from current cache
        initializeLocalVersions();

        // Start ES polling if enabled
        if (active && pollingInterval > 0) {
            startEsPolling();
        }
        log.info("Version sync coordinator started");
    }

    public void stop() {
        if (!isRunning) {
            return;
        }

        isRunning = false;

        // Stop ES polling
        if (active && pollingExecutor != null) {
            stopEsPolling();
        }

        log.info("Version sync coordinator stopped");
    }

    private void initializeLocalVersions() {
        // Initialize local version tracking from current cache
        Map<String, Map<String, Object>> cacheSnapshot = promptManager.getPromptCache();
        for (Map.Entry<String, Map<String, Object>> entry : cacheSnapshot.entrySet()) {
            String promptKey = entry.getKey();
            Map<String, Object> promptData = entry.getValue();
            int version = Integer.parseInt(promptData.getOrDefault("version", "1").toString());
            localVersions.put(promptKey, version);
        }
    }

    private void startEsPolling() {
        log.info(
                "Starting ES polling with {}s interval "
                        + "(configured in live_prompt.es_polling_interval)",
                pollingInterval
        );

        pollingExecutor = new ScheduledThreadPoolExecutor(1);
        pollingExecutor.scheduleAtFixedRate(
                this::esPoller,
                0,
                pollingInterval,
                TimeUnit.SECONDS
        );
    }

    private void stopEsPolling() {
        if (pollingExecutor != null) {
            pollingExecutor.shutdownNow();
            try {
                if (!pollingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("ES polling executor did not terminate properly");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            log.info("ES polling stopped");
        }
    }

    private void esPoller() {
        if (isRunning && active) {
            try {
                checkEsVersions();
            } catch (Exception e) {
                log.error("ES polling error: {}", e);
            }
        }
    }

    private void checkEsVersions() {
        try {
            // Get all prompts from ES
            Map<String, Object> searchBody = Map.of(
                    "query", Map.of("match_all", Map.of()),
                    "size", 1000,
                    "_source", List.of("prompt_key", "version", "updated_at")
            );

            Map<String, Object> response = promptManager.getEsClient().search(promptManager.getIndexName(), searchBody);

            if (response == null) {
                return;
            }

            Map<String, Object> hits = (Map<String, Object>) response.get("hits");
            if (hits != null) {
                List<Map<String, Object>> hitList = (List<Map<String, Object>>) hits.get("hits");
                for (Map<String, Object> hit : hitList) {
                    Map<String, Object> source = (Map<String, Object>) hit.get("_source");
                    if (source != null) {
                        String promptKey = (String) source.get("prompt_key");
                        int remoteVersion = Integer.parseInt(source.getOrDefault("version", "1").toString());

                        // Check if local version is behind
                        int localVersion = localVersions.getOrDefault(promptKey, 0);
                        if (remoteVersion > localVersion) {
                            handleVersionUpdate(promptKey, remoteVersion);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error checking ES versions: {}", e);
        }
    }

    /**
     * Handle a version update for a prompt with concurrency control.
     *
     *         Prevents duplicate updates, version rollback, and out-of-order updates.
     *
     * @param promptKey The prompt key to update
     * @param newVersion The new version number
     */
    public void handleVersionUpdate(String promptKey, int newVersion) {
        try {
            // Prevent duplicate updates for the same version
            synchronized (pendingUpdates) {
                Set<Integer> versions = pendingUpdates.getOrDefault(promptKey, new HashSet<>());
                if (versions.contains(newVersion)) {
                    log.debug("Skipping duplicate update for {} v{}", promptKey, newVersion);
                    return;
                }

                // Prevent version rollback: only accept if newVersion > current version
                int currentVersion = localVersions.getOrDefault(promptKey, 0);
                if (newVersion <= currentVersion) {
                    log.debug(
                            "Ignoring old version for {}: new={}, current={}",
                            promptKey, newVersion, currentVersion
                    );
                    return;
                }

                // Mark as pending to prevent concurrent updates
                versions.add(newVersion);
                pendingUpdates.put(promptKey, versions);
            }

            try {
                // Fetch from ES with retry logic
                fetchFromEsWithRetry(promptKey, newVersion, 3);
            } finally {
                // Remove from pending after processing (with cleanup of old versions)
                synchronized (pendingUpdates) {
                    Set<Integer> versions = pendingUpdates.get(promptKey);
                    if (versions != null) {
                        versions.remove(newVersion);
                        // Clean up old versions to prevent memory leak (keep last 10)
                        if (versions.size() > 10) {
                            // Remove oldest versions (keep higher version numbers)
                            List<Integer> versionsList = versions.stream().sorted().toList();
                            List<Integer> versionsToRemove = versionsList.subList(0, versionsList.size() - 10);
                            for (int v : versionsToRemove) {
                                versions.remove(v);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to handle version update for {}: {}", promptKey, e);
        }
    }

    private void fetchFromEsWithRetry(String promptKey, int newVersion, int maxRetries) {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                // Force fetch from ES (bypass cache)
                Map<String, Object> promptData = promptManager.getPrompt(promptKey, false);

                if (promptData != null) {
                    int actualVersion = Integer.parseInt(promptData.getOrDefault("version", "1").toString());
                    if (actualVersion == newVersion) {
                        // Update local version tracker
                        localVersions.put(promptKey, newVersion);

                        log.debug(
                                "Cache updated for {} from ES "
                                        + "(version {}, attempt {}/{})",
                                promptKey, newVersion, attempt + 1, maxRetries
                        );

                        // Trigger hot-reload for agents using this prompt
                        DynamicAgentManager dynamicAgentManager = DynamicAgentManager.getInstance();
                        dynamicAgentManager.updatePromptByKey(promptKey);
                        return;
                    } else {
                        log.debug(
                                "Version mismatch for {} on attempt {}: "
                                        + "expected={}, got={}",
                                promptKey, attempt + 1, newVersion, actualVersion
                        );
                        if (actualVersion > newVersion) {
                            // Newer version already exists, update our tracker and skip
                            localVersions.put(promptKey, actualVersion);
                            log.info("Found newer version {} for {}, skipping update", actualVersion, promptKey);
                            return;
                        }
                        // If actualVersion < newVersion, continue retrying
                    }
                } else {
                    log.debug(
                            "Prompt {} not found in ES during version sync "
                                    + "(attempt {}/{})",
                            promptKey, attempt + 1, maxRetries
                    );
                }

            } catch (Exception e) {
                log.error(
                        "Error fetching {} from ES (attempt {}): {}",
                        promptKey, attempt + 1, e
                );
            }

            // Wait before retry (exponential backoff: 0.5s, 1s, 2s)
            if (attempt < maxRetries - 1) {
                long waitTime = (long) (0.5 * Math.pow(2, attempt));
                log.debug("Retrying {} after {}s...", promptKey, waitTime);
                try {
                    Thread.sleep(waitTime * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        // All retries failed, log warning
        log.warn(
                "Failed to fetch {} v{} from ES after {} attempts. "
                        + "Will sync on next ES polling cycle.",
                promptKey, newVersion, maxRetries
        );
    }

    public void updateLocalVersion(String promptKey, int version) {
        localVersions.put(promptKey, version);
        log.debug("Updated local version tracker: {} v{}", promptKey, version);
    }

    // Static methods for global instance management
    public static VersionSyncCoordinator getInstance(PromptManager promptManager) {
        synchronized (VersionSyncCoordinator.class) {
            if (instance == null) {
                instance = new VersionSyncCoordinator(promptManager, null);
            }
            return instance;
        }
    }

    public static VersionSyncCoordinator getInstance() {
        synchronized (VersionSyncCoordinator.class) {
            if (instance == null) {
                PromptManager promptManager = PromptManager.getInstance();
                instance = new VersionSyncCoordinator(promptManager, null);
            }
            return instance;
        }
    }
}