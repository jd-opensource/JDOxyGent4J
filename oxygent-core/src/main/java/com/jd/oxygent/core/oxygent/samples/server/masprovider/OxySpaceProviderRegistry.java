package com.jd.oxygent.core.oxygent.samples.server.masprovider;

import com.jd.oxygent.core.oxygent.samples.server.masprovider.oxyspace.OxySpaceProvider;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.oxyspace.impl.DefaultOxySpaceProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class OxySpaceProviderRegistry {

    private static volatile OxySpaceProvider instance;
    private static final List<OxySpaceProvider> providers = new CopyOnWriteArrayList<>();
    private static boolean initialized = false;

    // Register provider
    public static void registerOxySpaceProvider(OxySpaceProvider provider) {
        providers.add(provider);
        log.debug("Registered OxySpaceProvider: {}", provider.getClass());
    }

    // Unregister provider
    public static void unregisterOxySpaceProvider(OxySpaceProvider provider) {
        providers.remove(provider);
        log.debug("Unregistered OxySpaceProvider: {}", provider.getClass());
    }

    // Auto-discover and register providers
    public static synchronized void autoDiscoverOxySpaceProviders() {
        if (initialized) {
            return;
        }

        try {
            ServiceLoader<OxySpaceProvider> loader = ServiceLoader.load(OxySpaceProvider.class);

            for (OxySpaceProvider provider : loader) {
                registerOxySpaceProvider(provider);
            }

            initialized = true;
            log.info("Auto-discovered {} OxySpaceProvider implementations", providers.size());

        } catch (Exception e) {
            log.error("Failed to auto-discover OxySpaceProvider implementations", e);
        }
    }

    public static OxySpaceProvider getOxySpaceProvider() {
        if (instance == null) {
            synchronized (OxySpaceProvider.class) {
                if (instance == null) {
                    instance = createProvider();
                }
            }
        }
        return instance;
    }

    // Get provider for specified platform
    public static OxySpaceProvider getOxySpaceProvider(String oxySpaceName) {
        if (!initialized) {
            autoDiscoverOxySpaceProviders();
        }

        return providers.stream()
                .filter(provider -> provider.getOxySpaceName().equalsIgnoreCase(oxySpaceName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No OxySpaceProvider found for platform: " + oxySpaceName));
    }

    public static List<String> getAvailablePlatforms() {
        if (!initialized) {
            autoDiscoverOxySpaceProviders();
        }

        return providers.stream()
                .map(OxySpaceProvider::getOxySpaceName)
                .collect(Collectors.toList());
    }

    private static OxySpaceProvider createProvider() {
        if (!initialized) {
            autoDiscoverOxySpaceProviders();
        }

        if (providers.isEmpty()) {
            log.warn("No OxySpaceProvider found, using default manual provider");
            return createDefaultManualProvider();
        }

        // If there are multiple providers, select based on some strategy
        // Here simply return the first one, in practice can select based on priority or other strategies
        OxySpaceProvider selectedOxySpaceProvider = providers.get(0);
        log.info("Selected OxySpaceProvider: {}", selectedOxySpaceProvider.getClass());

        return selectedOxySpaceProvider;
    }

    private static OxySpaceProvider createDefaultManualProvider() {
        return new DefaultOxySpaceProvider();
    }

    public static void setProvider(OxySpaceProvider provider) {
        synchronized (OxySpaceProvider.class) {
            instance = provider;
        }
    }

    public static void reset() {
        synchronized (OxySpaceProvider.class) {
            providers.clear();
            instance = null;
            initialized = false;
        }
    }
}
