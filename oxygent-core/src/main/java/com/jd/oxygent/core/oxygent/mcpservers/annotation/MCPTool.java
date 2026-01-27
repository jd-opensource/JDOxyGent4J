package com.jd.oxygent.core.oxygent.mcpservers.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MCPTool {

    /** Tool name */
    String name();

    /** Tool description */
    String description();

    /** Tool title (optional) */
    String title() default "";
}