package com.jd.oxygent.web;

import com.jd.oxygent.core.Config;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {"com.jd.oxygent", "com.jd.oxygent.web"})
@EnableConfigurationProperties(Config.class)
public class OpenOxySpringBootApplication {
    public static void main(String[] args) throws Exception {
        SpringApplication.run(OpenOxySpringBootApplication.class, args);
    }
}