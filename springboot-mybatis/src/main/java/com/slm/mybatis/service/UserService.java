package com.slm.mybatis.service;

import com.slm.mybatis.entity.User;
import com.slm.mybatis.mapper.UserMapper;
import com.slm.mybatis.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    public PageResult<User> query(String name, int page, int size) {
        int start = (page - 1) * size;
        List<User> data = userMapper.query(name, size, start);
        Long total = userMapper.count(name);
        return PageResult.<User>builder().total(total).data(data).build();
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
