package com.slm.security.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 受保护信息
 */
@RestController
@RequestMapping("/infos")
public class InfoController {

    @GetMapping
    public String get() {
        return "success";
    }

}
