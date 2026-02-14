package com.jd.oxygent.oxybank;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.jd.oxygent"})
public class OxyBankSpringBootApplication {
    public static void main(String[] args) throws Exception {
        SpringApplication.run(OxyBankSpringBootApplication.class, args);
    }
}