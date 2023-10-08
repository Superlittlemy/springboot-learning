package com.slm.mybatis.controller;

import com.slm.mybatis.entity.User;
import com.slm.mybatis.model.ApiResponse;
import com.slm.mybatis.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ApiResponse<List<User>> query() {
        return ApiResponse.ok(userService.query());
    }

    @PostMapping
    public ApiResponse<Integer> createdUser(@RequestBody User user) {
        return ApiResponse.ok(userService.create(user));
    }

    @PutMapping
    public ApiResponse<Integer> modifyUser(@RequestBody User user) {
        userService.modify(user);
        return ApiResponse.ok();
    }

}
