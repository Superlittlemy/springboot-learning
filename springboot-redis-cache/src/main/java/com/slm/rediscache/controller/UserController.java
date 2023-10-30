package com.slm.rediscache.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.slm.rediscache.entity.User;
import com.slm.rediscache.model.ApiResponse;
import com.slm.rediscache.model.UserCreateRequest;
import com.slm.rediscache.model.UserUpdateRequest;
import com.slm.rediscache.service.impl.UserServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "用户接口")
public class UserController {

    private final UserServiceImpl userServiceImpl;

    @GetMapping
    @Operation(summary = "查询用户列表")
    public ApiResponse<List<User>> query() {
        return ApiResponse.ok(userServiceImpl.query());
    }

    @GetMapping("{id}")
    @Operation(summary = "查询用户信息")
    public ApiResponse<User> getUserInfo(@PathVariable Long id) {
        return ApiResponse.ok(userServiceImpl.get(id));
    }

    @PostMapping
    @Operation(summary = "新增用户")
    public ApiResponse<Long> createdUser(@RequestBody @Valid UserCreateRequest request) {
        return ApiResponse.ok(userServiceImpl.create(request).getId());
    }

    @PutMapping
    @Operation(summary = "修改用户信息")
    public ApiResponse<?> modifyUser(@RequestBody @Valid UserUpdateRequest request) {
        userServiceImpl.modify(request);
        return ApiResponse.ok();
    }

    @DeleteMapping("{id}")
    @Operation(summary = "删除用户")
    public ApiResponse<?> deleteUser(@PathVariable Long id) {
        userServiceImpl.delete(id);
        return ApiResponse.ok();
    }

}
