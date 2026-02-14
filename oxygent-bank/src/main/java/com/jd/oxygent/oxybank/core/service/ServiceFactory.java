package com.jd.oxygent.oxybank.core.service;

import com.jd.oxygent.oxybank.core.config.ServiceConfig;
import com.jd.oxygent.oxybank.core.service.AnnotationService;
import lombok.extern.slf4j.Slf4j;

import com.jd.oxygent.oxybank.core.storer.docmanager.AnnotationManager;

/**
 * Service Factory - Service factory class
 *
 * Used to create and manage service instances, using singleton pattern
 */
@Slf4j
public class ServiceFactory {

    private static AnnotationService annotationService;

    private static AnnotationManager annotationManager;

    private static final com.jd.oxygent.oxybank.core.config.ServiceConfig settings = com.jd.oxygent.oxybank.core.config.ServiceConfig.getInstance();

    /**
     * Get annotation data manager (singleton)
     *
     * @return AnnotationManager instance
     */
    public static synchronized AnnotationManager getAnnotationManager() {
        if (annotationManager == null) {
            log.info("Initializing AnnotationManager...");
            annotationManager = new AnnotationManager(
                    settings.getEsClient(),
                    settings.getAnnotationConfig().getEsIndexPrefix()
            );
            annotationManager.initialize();
            log.info("AnnotationManager initialized, index: {}", annotationManager.getIndexName());
        }
        return annotationManager;
    }

    /**
     * Get annotation service (singleton)
     *
     * @return AnnotationService instance
     */
    public static synchronized AnnotationService getAnnotationService() {
        if (annotationService == null) {
            log.info("Initializing AnnotationService...");
            AnnotationManager manager = getAnnotationManager();
            ServiceConfig.AnnotationConfig config = settings.getAnnotationConfig();
            annotationService = new AnnotationService(manager, config);
            log.info("AnnotationService initialized");
        }
        return annotationService;
    }

    /**
     * Reset all services (mainly for testing)
     *
     * Note: Use with caution in production
     */
    public static synchronized void resetAnnotationServices() {
        annotationService = null;
        annotationManager = null;
        log.info("Annotation services reset");
    }
}