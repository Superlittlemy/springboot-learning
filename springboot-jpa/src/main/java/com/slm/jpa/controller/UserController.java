package com.slm.jpa.controller;

import com.querydsl.core.types.Predicate;
import com.slm.jpa.entity.User;
import com.slm.jpa.model.ApiResponse;
import com.slm.jpa.model.UserCreateRequest;
import com.slm.jpa.model.UserUpdateRequest;
import com.slm.jpa.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "用户接口")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "查询用户列表")
    public ApiResponse<Page<User>> query(@QuerydslPredicate(root = User.class) Predicate predicate,
                                         Pageable pageable) {
        return ApiResponse.ok(userService.pageQuery(predicate, pageable));
    }

    @GetMapping("{id}")
    @Operation(summary = "查询用户信息")
    public ApiResponse<User> getUserInfo(@PathVariable Long id) {
        return ApiResponse.ok(userService.get(id));
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
        userService.delete(id);
        return ApiResponse.ok();
    }

}
