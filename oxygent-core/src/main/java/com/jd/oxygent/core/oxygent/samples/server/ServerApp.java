package com.jd.oxygent.core.oxygent.samples.server;

import com.jd.oxygent.core.oxygent.samples.server.tomcat.EmbeddedTomcatLauncher;

public class ServerApp {

    public static void main(String[] args) {
        LauncherLifecycle launcherLifecycle = SimpleServerFactory.createLauncherServer("tomcat");
                          launcherLifecycle.launch(args);
    }
}