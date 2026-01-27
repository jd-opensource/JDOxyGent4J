package com.jd.oxygent.core.oxygent.mcpservers.annotation;
import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableMcpServer {
    /**
     * Startup mode
     * - "stdio": Standard input/output mode
     * - "web": WebSocket/HTTP mode
     */
    String mode() default "stdio";

    /**
     * Whether to automatically scan tools
     */
    boolean autoScan() default true;

    /**
     * Scan base packages (when empty, default scans the package where the startup class is located and its subpackages)
     */
    String[] scanBasePackages() default {};
}