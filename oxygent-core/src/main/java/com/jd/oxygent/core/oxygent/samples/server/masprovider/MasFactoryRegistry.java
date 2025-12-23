package com.jd.oxygent.core.oxygent.samples.server.masprovider;

import com.jd.oxygent.core.oxygent.samples.server.masprovider.factory.MasFactory;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.factory.PlatformMasFactory;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.factory.impl.ManualMasFactory;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.factory.impl.platform.spring.ApplicationContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class MasFactoryRegistry {

    private static volatile MasFactory instance;
    private static final List<PlatformMasFactory> factories = new CopyOnWriteArrayList<>();
    private static boolean initialized = false;

    public static void registerFactory(PlatformMasFactory factory) {
        factories.add(factory);
        log.debug("Registered MasFactory: {}", factory.getClass().getSimpleName());
    }

    public static void unregisterFactory(PlatformMasFactory factory) {
        factories.remove(factory);
        log.debug("Unregistered MasFactory: {}", factory.getClass().getSimpleName());
    }

    private static synchronized void autoDiscoverFactories() {
        if (initialized) {
            return;
        }

        try {
            ServiceLoader<PlatformMasFactory> loader = ServiceLoader.load(PlatformMasFactory.class);

            for (PlatformMasFactory factory : loader) {
                registerFactory(factory);
            }

            log.info("Auto-discovered {} MasFactory implementations", factories.size());

        } catch (Exception e) {
            log.error("Failed to auto-discover MasFactory implementations", e);
        }
    }

    private static synchronized void autoDiscoverSpringFactories() {
        if (initialized) {
            return;
        }

        try {
            ApplicationContext context = ApplicationContextHolder.getApplicationContext();
            if (context != null && ApplicationContextHolder.isContextInitialized()) {
                Map<String, PlatformMasFactory> springFactories = context.getBeansOfType(PlatformMasFactory.class);

                for (PlatformMasFactory factory : springFactories.values()) {
                    if (factories.stream().noneMatch(f -> f.getClass().equals(factory.getClass()))) {
                        factories.add(factory);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load factories from Spring container", e);
        }
    }

    // Get appropriate factory instance
    public static MasFactory getFactory() {
        if (instance == null) {
            synchronized (PlatformMasFactory.class) {
                if (instance == null) {
                    instance = createFactory();
                }
            }
        }
        return instance;
    }

    private static MasFactory createFactory() {
        if (!initialized) {
            //load from service loader
            autoDiscoverFactories();
            //load from spring
            autoDiscoverSpringFactories();

            initialized = true;
        }

        // Sort by priority and select the first supported factory
        List<PlatformMasFactory> supportedFactories = factories.stream()
                .filter(PlatformMasFactory::supportsPlatform)
                .sorted(Comparator.comparingInt(PlatformMasFactory::getPriority).reversed())
                .collect(Collectors.toList());

        if (supportedFactories.isEmpty()) {
            log.warn("No supported MasFactory found, using default manual factory");
            return new ManualMasFactory();
        }

        PlatformMasFactory selectedFactory = supportedFactories.get(0);
        log.info("Selected MasFactory: {} for platform: {}",
                selectedFactory.getClass().getSimpleName(),
                selectedFactory.getPlatformName());

        return selectedFactory;
    }

    public static void setFactory(MasFactory factory) {
        synchronized (MasFactoryRegistry.class) {
            instance = factory;
        }
    }

    public static void reset() {
        synchronized (MasFactoryRegistry.class) {
            factories.clear();
            instance = null;
            initialized = false;
        }
    }

//    public static void main(String[] args) {
//        MasFactory factory = MasFactoryRegistry.getFactory();
//        factory.createMas("test");
//        System.out.println(factory.getClass().getName());
//    }
}
