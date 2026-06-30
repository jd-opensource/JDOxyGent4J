package com.jd.oxygent.core.oxygent.samples.server;

import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.utils.StringUtils;
import org.apache.logging.log4j.util.Strings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ServerConstants {

    public static final int DEFAULT_PORT = getPort(Config.getServer().getPort(),8888);

    public static final String DEFAULT_HOST_NAME = "localhost";

    public static final String DEFAULT_TOMCAT_BASE_TMP_DIR = "tomcat_oxygent";

    public static String DEFAULT_API_ENDPOINT_SCANNER_PATH =getOrDefaultValue(Config.getApp().getScanApiEndpointPath(),"");

    public static final String DEFAULT_CONTEXT_PATH = "";

    public static final String DEFAULT_FILE_STORE_TEMP_DIR = "java.io.tmpdir";// Temporary directory

    public static final int DEFAULT_MEMORY_SIZE_THRESHOLD = 1024 * 1024;// 1MB memory threshold

    public static final int DEFAULT_UPLOAD_FILE_MAX_SIZE_THRESHOLD = 10 * 1024 * 1024;// 10MB file size limit

    public static final int DEFAULT_UPLOAD_ALL_FILE_MAX_SIZE_THRESHOLD = 50 * 1024 * 1024;// 50MB total request size limit

    public static final Set<String> RESTRICTED_HEADERS = Set.of(
            "connection",
            "keep-alive",
            "accept-encoding",
            "content-length",
            "host",
            "expect",
            "upgrade",
            "proxy-authenticate",
            "te",
            "trailer",
            "transfer-encoding"
    );

    public static final List<String> listners = List.of(
            "com.jd.oxygent.core.oxygent.samples.server.tomcat.listener.StartupCheckerListener"
    );

    public static final List<String> WELCOME_PAGE_LISTS = List.of(
            "index.html",
            "index.htm",
            "default.html"
    );

    public static final Map<String, String> ROUTE_MAPPING = new HashMap<>() {{
        this.put("/", "RouteServlet");
        this.put("/check_alive", "RouteServlet");
        this.put("/get_organization", "RouteServlet");
        this.put("/get_first_query", "RouteServlet");
        this.put("/get_welcome_message", "RouteServlet");
        this.put("/get_description", "RouteServlet");
        this.put("/list_script", "RouteServlet");
        this.put("/save_script", "RouteServlet");
        this.put("/load_script", "RouteServlet");
        this.put("/chat", "RouteServlet");
        this.put("/sse/chat", "RouteServlet");
        this.put("/async/chat", "RouteServlet");
        this.put("/node", "RouteServlet");
        this.put("/view", "RouteServlet");
        this.put("/call", "RouteServlet");
        this.put("/upload", "RouteServlet");
        this.put("/feedback", "RouteServlet");
        this.put("/api/prompts/*", "RouteServlet");
        this.put("/get_agents", "RouteServlet");
        this.put("/rating/*", "RouteServlet");
        this.put("/history_with_ratings", "RouteServlet");
        this.put("/analytics/ratings", "RouteServlet");
        this.put("/a2a/*", "RouteServlet");
        this.put("/a2a", "RouteServlet");
    }};
    public static final List<String> WEBAPP_RESOURCE_DIR_PATHS = List.of(
            "webapp",
            "src/main/resource/webapp",
            "src/main/webapp",
            "src/webapp",
            "../webapp",
            "../../webapp",
            "./src/main/webapp");
    public static final List<String> WEB_CONFIG_PATHS = List.of(
//                "webapp/WEB-INF/web.xml",
            "src/main/resources/webapp/WEB-INF/web.xml",
            "src/main/webapp/WEB-INF/web.xml",
            "./src/main/webapp/WEB-INF/web.xml",
            "webapp/WEB-INF/web.xml",
            "src/WEB-INF/web.xml",
            "./web.xml",
            "web.xml"
    );
    public static final List<String> STATIC_RESOURCE_PATHS = List.of(
            "static/web/",
            "webapp",
            "src/main/resources/static/web/",
            "src/main/static/web/"
    );

    public static int getPort(int port, int defaultValue) {
        if(port<1024){
            return defaultValue;
        }else{
            return port;
        }
    }

    public static String getOrDefaultValue(String value,String defaultValue){
        if(Strings.isEmpty(value)){
            return defaultValue;
        }else{
            return value;
        }
    }
}