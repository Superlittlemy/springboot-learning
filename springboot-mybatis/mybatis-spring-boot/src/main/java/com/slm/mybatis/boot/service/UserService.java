package com.slm.mybatis.boot.service;

import com.github.pagehelper.PageHelper;
import com.slm.mybatis.boot.entity.User;
import com.slm.mybatis.boot.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public List<User> query(String name, int page, int size) {
        PageHelper.startPage(page, size);
        return userMapper.query(name);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(User user) {
        userMapper.add(user);
        return user.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void modify(User user) {
        userMapper.update(user);
    }

}
