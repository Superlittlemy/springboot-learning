package com.slm.properties.controller;

import com.slm.properties.config.Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private Properties properties;
    @Value("${spring.application.name}")
    private String name;

    @GetMapping
    public String status() {
        return "hello " + properties.getName() + " this is " + name;
    }

}
