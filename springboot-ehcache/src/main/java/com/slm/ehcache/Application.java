package com.slm.ehcache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;

@EnableCaching
@SpringBootApplication
public class Application {

    public static ConfigurableApplicationContext sa;

    public static void main(String[] args) {
        sa = SpringApplication.run(Application.class, args);
    }

}
