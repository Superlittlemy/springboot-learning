package com.slm.validator.controller;

import com.slm.validator.model.ApiResponse;
import com.slm.validator.request.UserModel;
import com.slm.validator.request.group.UserCreate;
import com.slm.validator.request.group.UserModify;
import com.slm.validator.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @GetMapping("{id}")
    public ApiResponse<?> getUser(@PathVariable String id, @NotBlank(message = "用户状态不能为空") String status) {
        return ApiResponse.ok();
    }

    @PostMapping
    public ApiResponse<?> createUser(@RequestBody @Validated(value = UserCreate.class) UserModel user) {
        userService.createUser(user);
        return ApiResponse.ok();
    }

    @PutMapping
    public ApiResponse<?> modifyUser(@RequestBody @Validated(value = UserModify.class) UserModel user) {
        userService.modifyUser(user);
        return ApiResponse.ok();
    }

}
