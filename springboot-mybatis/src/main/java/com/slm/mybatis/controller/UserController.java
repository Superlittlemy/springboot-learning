package com.slm.mybatis.controller;

import com.slm.mybatis.entity.User;
import com.slm.mybatis.model.ApiResponse;
import com.slm.mybatis.model.PageResult;
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
    public ApiResponse<PageResult<User>> query(@RequestParam(required = false) String name,
                                               @RequestParam Integer page,
                                               @RequestParam Integer size) {
        return ApiResponse.ok(userService.query(name, page, size));
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
