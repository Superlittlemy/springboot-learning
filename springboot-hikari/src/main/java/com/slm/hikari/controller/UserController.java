package com.slm.hikari.controller;

import com.slm.hikari.entity.User;
import com.slm.hikari.model.ApiResponse;
import com.slm.hikari.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<List<User>> query() {
        return ApiResponse.ok(userService.query());
    }

    @PostMapping
    public ApiResponse<Long> createdUser(@RequestBody User user) {
        return ApiResponse.ok(userService.create(user));
    }

    @PutMapping
    public ApiResponse<?> modifyUser(@RequestBody User user) {
        userService.modify(user);
        return ApiResponse.ok();
    }

}
