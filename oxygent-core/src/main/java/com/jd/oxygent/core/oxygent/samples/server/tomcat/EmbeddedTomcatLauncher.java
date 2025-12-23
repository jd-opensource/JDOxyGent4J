package com.jd.oxygent.core.oxygent.samples.server.tomcat;

import com.jd.oxygent.core.oxygent.samples.server.LauncherLifecycle;
import com.jd.oxygent.core.oxygent.samples.server.ServerConstants;
import com.jd.oxygent.core.oxygent.samples.server.filter.MimeTypeFilter;
import com.jd.oxygent.core.oxygent.samples.server.filter.RouteFilter;
import jakarta.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.jd.oxygent.core.oxygent.samples.server.ServerConstants.*;

/**
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class EmbeddedTomcatLauncher implements LauncherLifecycle {

    private Tomcat tomcat = null;

    @Override
    public void launch(String[] args) {
        try {
            // Parse command line arguments
            int port = parsePort(args);
            String contextPath = parseContextPath(args);

            log.info("Starting embedded Tomcat server...");
            log.info("Port: {}", port);
            log.info("Context path: {}", contextPath);

            // Create and configure Tomcat
            tomcat = createTomcat(port, contextPath);

            // Start server
            tomcat.start();

            log.info("Embedded Tomcat started successfully!");
            log.info("Application access URL: http://localhost:{}{}/index.html", port, contextPath);

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
    private Tomcat createTomcat(int port, String contextPath) throws ServletException, IOException {
        // Set base directory
        Path tempDir = Files.createTempDirectory(DEFAULT_TOMCAT_BASE_TMP_DIR);
        // Configure port
        Connector connector = new Connector();
        connector.setPort(port);
        connector.setProperty("address", "0.0.0.0"); // Listen on all addresses

        Tomcat tomcat = new Tomcat();
        tomcat.setConnector(connector);
        tomcat.setHostname(DEFAULT_HOST_NAME);
        tomcat.setBaseDir(tempDir.toString());

        // Create web application context
        Context context = createWebContext(tomcat, contextPath);

        // Configure servlets
        configureServlets(context);

        // Configure filters
        configureFilters(context);

        // Configure static resources
        configureStaticResources(context);

        // Configure listeners
        configureListeners(context);

        return tomcat;
    }

    /**
     * Create web application context
     */
    private Context createWebContext(Tomcat tomcat, String contextPath) throws IOException {
        // Get webapp directory path
        String webappDir = findWebappDirectory();

        if (webappDir != null && new File(webappDir).exists()) {
            log.info("Using webapp directory: {}", webappDir);
            return tomcat.addWebapp(contextPath, webappDir);
        } else {
            log.info("Using in-memory web application context");
            Context context = tomcat.addContext(contextPath, null);

            // Load web.xml configuration
            loadWebXmlConfiguration(context);

            return context;
        }
    }

    /**
     * Find webapp directory
     */
    private String findWebappDirectory() {
        for (String path : WEBAPP_RESOURCE_DIR_PATHS) {
            Resource resource = new ClassPathResource(path);
            File dir = null;
            try {
                dir = resource.getFile();
            } catch (IOException e) {
                continue;
            }
            if (dir.exists() && dir.isDirectory()) {
                log.info("Found webapp directory: {}", dir.getAbsolutePath());
                return dir.getAbsolutePath();
            }
        }
        log.warn("Webapp directory not found, will use in-memory web application context");
        return null;
    }

    /**
     * Load web.xml configuration
     */
    private void loadWebXmlConfiguration(Context context) {
        try {
            // Find web.xml file
            String webXmlPath = findWebXmlPath();

            if (webXmlPath != null) {
                log.info("Loading web.xml configuration: {}", webXmlPath);
                context.setConfigFile(new File(webXmlPath).toURI().toURL());
            } else {
                log.warn("web.xml file not found, using annotation configuration");
            }
        } catch (Exception e) {
            log.error("Failed to load web.xml configuration", e);
        }
    }

    /**
     * Find web.xml file path
     */
    private String findWebXmlPath() {
        for (String path : WEB_CONFIG_PATHS) {
            Resource resource = new ClassPathResource(path);
            File file = null;
            try {
                file = resource.getFile();
            } catch (IOException e) {
                continue;
            }
            if (file.exists() && file.isFile()) {
                log.info("Found web.xml file: {}", file.getAbsolutePath());
                return file.getAbsolutePath();
            }
        }

        log.warn("web.xml file not found");
        return null;
    }

    /**
     * Configure Servlets
     */
    private void configureServlets(Context context) {
        try {
            // Add RouteServlet
            Wrapper servletWrapper = context.createWrapper();
            servletWrapper.setName("RouteServlet");
            servletWrapper.setServletClass("com.jd.oxygent.core.oxygent.samples.server.servlet.RouteServlet");
            servletWrapper.setLoadOnStartup(1);

            context.addChild(servletWrapper);
            ServerConstants.ROUTE_MAPPING.forEach(context::addServletMappingDecoded);
            log.info("RouteServlet configuration completed");
        } catch (Exception e) {
            log.error("Failed to configure Servlets", e);
        }
    }

    /**
     * Configure Filters
     */
    private void configureFilters(Context context) {
        // Add route Filter
        FilterDef filterDef = new FilterDef();
        filterDef.setFilterName("routeFilter");
        filterDef.setFilterClass(RouteFilter.class.getName());
        context.addFilterDef(filterDef);

        // Map Filter to all requests
        FilterMap filterMap = new FilterMap();
        filterMap.setFilterName("routeFilter");
        filterMap.addURLPattern("/*");
        context.addFilterMap(filterMap);

        // Add MIME type Filter
        FilterDef mimeTypefilterDef = new FilterDef();
        mimeTypefilterDef.setFilterName("mimeTypeFilter");
        mimeTypefilterDef.setFilterClass(MimeTypeFilter.class.getName());
        context.addFilterDef(mimeTypefilterDef);

        // Map Filter to image requests
        FilterMap mimeTypefilterMap = new FilterMap();
        mimeTypefilterMap.setFilterName("mimeTypeFilter");
        mimeTypefilterMap.addURLPattern("/image/*");
        context.addFilterMap(mimeTypefilterMap);
    }

    /**
     * Configure static resources
     */
    private void configureStaticResources(Context context) {
        try {
            String staticDir = this.findStaticResourceDirectory();
            if (staticDir != null) {
                log.info("Configuring static resource directory: {}", staticDir);
                WebResourceRoot resources = new StandardRoot(context);
                if (staticDir.startsWith("jar:")) {
                    URL jarUrl = new URL(staticDir);
                    JarURLConnection jarConn = (JarURLConnection) jarUrl.openConnection();
                    String jarFilePath = jarConn.getJarFileURL().getFile();
                    String entryPath = jarConn.getEntryName();
                    String entryDirectory = entryPath.substring(0, entryPath.lastIndexOf('/'));
                    resources.addJarResources(new JarResourceSet(
                            resources,
                            "/",
                            jarFilePath,
                            "/" + entryDirectory
                    ));
                    log.info("Adding JarResourceSet - jar: {}, entry: {}", jarFilePath, entryDirectory);
                } else {
                    resources.addPreResources(new DirResourceSet(
                            resources, "/", staticDir, "/"
                    ));
                }
                context.setResources(resources);
                Tomcat.addServlet(context, "default", new DefaultServlet());
                context.addServletMappingDecoded("/*", "default");
                ServerConstants.WELCOME_PAGE_LISTS.forEach(context::addWelcomeFile);
            }
        } catch (Exception e) {
            log.error("Failed to configure static resources", e);
        }
    }

    /**
     * Find static resource directory
     */
    private String findStaticResourceDirectory() throws IOException {
        for (String path : STATIC_RESOURCE_PATHS) {
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                continue;
            }
            URL url = resource.getURL();
            String protocol = url.getProtocol();
            if ("file".equals(protocol)) {
                try {
                    File dir = resource.getFile();
                    if (dir.exists() && dir.isDirectory()) {
                        return dir.getAbsolutePath();
                    }
                } catch (FileNotFoundException e) {
                    continue;
                }
            } else if ("jar".equals(protocol)) {
                String normalizedPath = path.endsWith("/") ? path : path + "/";
                ClassPathResource dirResource = new ClassPathResource(normalizedPath);
                if (dirResource.exists()) {
                    // return JAR URL，format：jar:file:/path/to.jar!/static/web/
                    URL dirUrl = dirResource.getURL();
                    return dirUrl.toString();
                }
            }
        }
        return null;
    }

    /**
     * Configure listeners
     */
    private void configureListeners(Context context) {
        try {
            // Add ServletConfig listeners
            listners.forEach(context::addApplicationListener);

            log.info("ServletConfig listeners configuration completed");
        } catch (Exception e) {
            log.error("Failed to configure listeners", e);
        }
    }

    /**
     * Parse port parameter
     */
    private int parsePort(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--port".equals(args[i]) || "-p".equals(args[i])) {
                try {
                    return Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException e) {
                    log.warn("Invalid port number: {}, using default port {}", args[i + 1], DEFAULT_PORT);
                }
            }
        }
        return DEFAULT_PORT;
    }

    /**
     * Parse context path parameter
     */
    private String parseContextPath(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--context".equals(args[i]) || "-c".equals(args[i])) {
                String contextPath = args[i + 1];
                // Ensure context path starts with /
                if (!contextPath.startsWith("/")) {
                    contextPath = "/" + contextPath;
                }
                return contextPath;
            }
        }
        return DEFAULT_CONTEXT_PATH;
    }
}
