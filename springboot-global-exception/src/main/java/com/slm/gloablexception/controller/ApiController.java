package com.slm.gloablexception.controller;

import com.slm.gloablexception.model.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("success")
    public ApiResponse<String> success() {
        return ApiResponse.ok("success");
    }

    @GetMapping("failure")
    public ApiResponse<?> failure() {
        throw new RuntimeException("系统业务异常");
    }

}
