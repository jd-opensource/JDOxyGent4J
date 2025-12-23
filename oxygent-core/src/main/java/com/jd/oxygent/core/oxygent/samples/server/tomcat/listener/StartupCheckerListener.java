package com.jd.oxygent.core.oxygent.samples.server.tomcat.listener;

import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.samples.server.ServerConstants;
import com.jd.oxygent.core.oxygent.samples.server.utils.BrowserOpener;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import lombok.extern.slf4j.Slf4j;

/**
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@WebListener
public class StartupCheckerListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        if (Config.getServer().isAutoOpenWebpage()) {
            BrowserOpener.open(String.format("http://%s:%s/index.html", ServerConstants.DEFAULT_HOST_NAME, ServerConstants.DEFAULT_PORT));
        }
    }
}
