package com.jd.oxygent.core.oxygent.samples.server;

import com.jd.oxygent.core.oxygent.samples.server.tomcat.EmbeddedTomcatLauncher;

public final class SimpleServerFactory {

    public static LauncherLifecycle createLauncherServer(String type) {
        return switch (type) {
            case "tomcat" -> new EmbeddedTomcatLauncher();
            case "jetty" -> new EmbeddedTomcatLauncher();
            default -> new EmbeddedTomcatLauncher();
        };
    }
}
