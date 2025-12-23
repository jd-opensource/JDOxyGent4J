package com.jd.oxygent.core.oxygent.samples.server.masprovider.factory.impl.platform.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
@Slf4j
public class ApplicationContextHolder implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {
    private static ApplicationContext applicationContext;
    private static volatile boolean contextSet = false;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextHolder.applicationContext = applicationContext;
        contextSet=true;
    }

    public static ApplicationContext getApplicationContext() {
        return ApplicationContextHolder.applicationContext;
    }

    public static <T> T getBean(Class<T> clazz) {
        if (applicationContext == null) {
            throw new IllegalStateException("ApplicationContext is not initialized yet");
        }
        return applicationContext.getBean(clazz);
    }

    public static <T> T getBean(String name, Class<T> clazz) {
        if (applicationContext == null) {
            throw new IllegalStateException("ApplicationContext is not initialized yet");
        }
        return applicationContext.getBean(name, clazz);
    }

    public static boolean isContextInitialized() {
        return contextSet;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {}

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (beanFactory instanceof ApplicationContext) {
            ApplicationContextHolder.applicationContext = (ApplicationContext) beanFactory;
            contextSet = true;
            log.info("EarlyApplicationContextHolder initialized in BeanFactoryPostProcessor");
        }
    }
}