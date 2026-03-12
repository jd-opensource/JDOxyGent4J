package com.jd.oxygent.web.event;

import com.jd.oxygent.core.Config;
import com.jd.oxygent.core.oxygent.samples.server.utils.BrowserOpener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * @author: xiaoailiang
 * @date: 2025/10/28
 */
@Component
public class StartupCheckerListener implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private Environment environment;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (Config.getServer().isAutoOpenWebpage()) {
            String serverPort = environment.getProperty("server.port", "8080");
            String contextPath = environment.getProperty("server.servlet.context-path", "localhost");

            BrowserOpener.open(String.format("http://%s:%s/", contextPath,serverPort));
        }
    }
}