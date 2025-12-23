package com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation;

import java.lang.annotation.*;

/**
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OxySpaceBean {
    String name() default "";

    String value() default "";

    String query() default "";

    boolean defaultStart() default false;

    int order() default 0;

    boolean enabled() default true;
}