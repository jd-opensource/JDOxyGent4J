package com.jd.oxygent.core.oxygent.mcpservers.annotation;
import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
/**

 * - Transport mode: stdio / sse / streamable-http
 * - Port configuration (for SSE/Streamable HTTP)
 */
public @interface EnableMcpServer {


    /**
     * Startup mode
     * - "stdio": Standard input/output mode
     * - "web": WebSocket/HTTP mode
     */
    String mode() default "stdio";

    /**
     * Localhost address for MCP server
     */
    String localhost() default "127.0.0.1";

    /**
     * Port number for MCP server
     */
    String port() default "8080";

    /**
     * Transport protocol
     * - "sse": Server-Sent Events
     * - "streamable": streamable HTTP
     */
    String transport() default "sse";

    /**
     * Whether to automatically scan tools
     */
    boolean autoScan() default true;

    /**
     * Scan base packages (when empty, default scans the package where the startup class is located and its subpackages)
     */
    String[] scanBasePackages() default {};
}