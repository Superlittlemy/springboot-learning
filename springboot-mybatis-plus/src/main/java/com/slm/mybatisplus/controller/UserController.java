package com.slm.mybatisplus.controller;

import com.slm.mybatisplus.entity.User;
import com.slm.mybatisplus.model.ApiResponse;
import com.slm.mybatisplus.model.UserCreateRequest;
import com.slm.mybatisplus.model.UserUpdateRequest;
import com.slm.mybatisplus.service.UserService;
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

    private final UserService userService;

    @GetMapping
    @Operation(summary = "查询用户列表")
    public ApiResponse<List<User>> query() {
        return ApiResponse.ok(userService.list());
    }

    @GetMapping("{id}")
    @Operation(summary = "查询用户信息")
    public ApiResponse<User> getUserInfo(@PathVariable Long id) {
        return ApiResponse.ok(userService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增用户")
    public ApiResponse<Long> createdUser(@RequestBody @Valid UserCreateRequest request) {
        return ApiResponse.ok(userService.save(request));
    }

    @PutMapping
    @Operation(summary = "修改用户信息")
    public ApiResponse<?> modifyUser(@RequestBody @Valid UserUpdateRequest request) {
        userService.modify(request);
        return ApiResponse.ok();
    }

    @DeleteMapping("{id}")
    @Operation(summary = "删除用户")
    public ApiResponse<?> deleteUser(@PathVariable Long id) {
        userService.softDelete(id);
        return ApiResponse.ok();
    }

}
