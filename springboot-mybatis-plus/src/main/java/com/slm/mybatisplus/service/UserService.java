package com.slm.mybatisplus.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.slm.mybatisplus.entity.User;
import com.slm.mybatisplus.model.PageRequest;
import com.slm.mybatisplus.model.UserCreateRequest;
import com.slm.mybatisplus.model.UserUpdateRequest;

public interface UserService extends IService<User> {

    Page<User> pageQuery(String name, Boolean deleted, PageRequest pageRequest);

    Long save(UserCreateRequest request);

    void modify(UserUpdateRequest request);

    void softDelete(Long id);

}
