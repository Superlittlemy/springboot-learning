package com.slm.mybatisplus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.slm.mybatisplus.entity.User;
import com.slm.mybatisplus.model.UserCreateRequest;
import com.slm.mybatisplus.model.UserUpdateRequest;

public interface UserService extends IService<User> {

    Long save(UserCreateRequest request);

    void modify(UserUpdateRequest request);

    void softDelete(Long id);

}
