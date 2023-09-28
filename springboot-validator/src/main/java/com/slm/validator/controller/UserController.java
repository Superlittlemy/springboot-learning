package com.slm.validator.controller;

import com.slm.validator.model.ApiResponse;
import com.slm.validator.request.UserCreateModel;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/users")
public class UserController {

    @PostMapping
    public ApiResponse createUser(@RequestBody @Valid UserCreateModel user) {
        return ApiResponse.ok();
    }

}
