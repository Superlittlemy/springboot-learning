package com.slm.seata.storage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class StorageServer {

    public static ApplicationContext ac;

    public static void main(String[] args) {
        ac = SpringApplication.run(StorageServer.class, args);
    }

}
