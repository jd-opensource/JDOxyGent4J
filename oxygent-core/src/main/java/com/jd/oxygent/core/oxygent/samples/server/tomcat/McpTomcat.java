package com.jd.oxygent.core.oxygent.samples.server.tomcat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jd.oxygent.core.oxygent.samples.server.LauncherLifecycle;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static com.jd.oxygent.core.oxygent.samples.server.ServerConstants.DEFAULT_HOST_NAME;
import static com.jd.oxygent.core.oxygent.samples.server.ServerConstants.DEFAULT_TOMCAT_BASE_TMP_DIR;

/**
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Builder
public class McpTomcat implements LauncherLifecycle {

    private Tomcat tomcat = null;
    private Servlet servlet = null;
    private int port =8080;
    private String addresses ="0.0.0.0";
    private String contextPath = "";

    @Override
    public void launch(String[] args) {
        try {
            log.info("Starting embedded Tomcat server...");
            log.info("Port: {}", port);
            log.info("Context path: {}", contextPath);

            // Create and configure Tomcat
            tomcat = createTomcat();

            // Start server
            tomcat.start();

            switch (args[0]) {
                case "sse":
                    log.info("Send requests to: POST http://{}:{}/mcp/message", addresses,port);
                    log.info("Listen for events: SSE http://{}:{}/mcp/sse", addresses,port);
                    break;
                case "streamable":
                    log.info("Endpoint: POST  http://{}:{}/mcp/sse", addresses,port);
                    break;
            }


            // Keep server running
            tomcat.getServer().await();

        } catch (Exception e) {
            log.error("Failed to start embedded Tomcat", e);
            System.exit(1);
        }
    }

    @Override
    public void stop() {
        try {
            tomcat.stop();
        } catch (LifecycleException e) {
            log.error("Failed to stop embedded Tomcat", e);
            System.exit(1);
        }
    }

    /**
     * Create and configure Tomcat instance
     */
    private Tomcat createTomcat() throws ServletException, IOException {
        // Set base directory
        Path tempDir = Files.createTempDirectory(DEFAULT_TOMCAT_BASE_TMP_DIR);
        // Configure port
        Connector connector = new Connector();
        connector.setPort(port);
        connector.setProperty("address", addresses); // Listen on all addresses

        Tomcat tomcat = new Tomcat();
        tomcat.setConnector(connector);
        tomcat.setHostname(DEFAULT_HOST_NAME);
        tomcat.setBaseDir(tempDir.toString());

        // Create web application context
        Context context = tomcat.addContext(contextPath, null);

        // Configure servlets
        configureServlets(context,servlet);

        return tomcat;
    }

    /**
     * Configure Servlets
     */
    private void configureServlets(Context context, Servlet servlet) {
        try {
            // Add McpServlet
            Wrapper servletWrapper = context.createWrapper();
            servletWrapper.setName("McpServlet");
//            servletWrapper.setServletClass("com.jd.oxygent.core.oxygent.samples.server.servlet.RouteServlet");
            servletWrapper.setServlet(servlet);
            servletWrapper.setAsyncSupported(true);
            servletWrapper.setLoadOnStartup(1);
            context.addChild(servletWrapper);
            context.addServletMappingDecoded("/*", "McpServlet");

            log.info("McpServlet configuration completed");
        } catch (Exception e) {
            log.error("Failed to configure Servlets", e);
        }
    }
}