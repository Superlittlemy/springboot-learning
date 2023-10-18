package com.slm.security.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开信息
 */
@RestController
@RequestMapping("/common")
public class CommonController {

    @GetMapping
    public String get() {
        return "success";
    }

}
