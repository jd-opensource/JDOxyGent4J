package com.jd.oxygent.core.oxygent.samples.server.masprovider.factory.impl.platform.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
/**
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
public class InitOrderConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ApplicationContextHolder applicationContextHolder() {
        return new ApplicationContextHolder();
    }
}