package com.jd.oxygent.web;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.jd.oxygent", "com.jd.oxygent.web"})
public class OpenOxySpringBootApplication {
    public static void main(String[] args) throws Exception {
        SpringApplication.run(OpenOxySpringBootApplication.class, args);
    }
}