package com.slm.mybatisplus.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.mybatisplus.entity.User;
import com.slm.mybatisplus.exception.BizException;
import com.slm.mybatisplus.mapper.UserMapper;
import com.slm.mybatisplus.model.UserCreateRequest;
import com.slm.mybatisplus.model.UserUpdateRequest;
import com.slm.mybatisplus.service.UserService;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public Long save(UserCreateRequest request) {
        User user = User.builder().name(request.getName()).build();
        this.save(user);
        return user.getId();
    }

    @Override
    public void modify(UserUpdateRequest request) {
        User user = this.getById(request.getId());
        if (Objects.isNull(user)) {
            throw new BizException("用户不存在");
        }

        user.setName(request.getName());
        this.updateById(user);
    }

    @Override
    public void softDelete(Long id) {
        User user = this.getById(id);
        if (Objects.isNull(user)) {
            throw new BizException("用户不存在");
        }
        if (Boolean.TRUE.equals(user.getDeleted())) {
            throw new RuntimeException("用户已被删除，请刷新");
        }

        user.setDeleted(Boolean.TRUE);
        this.updateById(user);
    }

}
