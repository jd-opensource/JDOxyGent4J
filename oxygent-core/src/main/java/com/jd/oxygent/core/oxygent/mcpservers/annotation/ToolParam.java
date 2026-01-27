package com.jd.oxygent.core.oxygent.mcpservers.annotation;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ToolParam {

    /** Parameter description */
    String description();

    /** Whether required */
    boolean required() default true;

    /** Default value (JSON string format) */
    String defaultValue() default "";

    /** Parameter type */
    String type() default "string";

    /** Enum values (JSON array format) */
    String enumValues() default "";
}