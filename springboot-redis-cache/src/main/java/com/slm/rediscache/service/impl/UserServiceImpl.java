package com.slm.rediscache.service.impl;

import com.slm.rediscache.entity.User;
import com.slm.rediscache.mapper.UserMapper;
import com.slm.rediscache.model.UserCreateRequest;
import com.slm.rediscache.model.UserUpdateRequest;
import com.slm.rediscache.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final static String REDIS_USER_INFO_KEY= "USER-INFO";

    private final UserMapper userMapper;

    @Override
    public List<User> query() {
        return userMapper.query();
    }

    @Override
    @Cacheable(value = REDIS_USER_INFO_KEY, key = "#id", unless = "#result==null")
    public User get(Long id) {
        return userMapper.get(id);
    }

    @Override
    @CachePut(value = REDIS_USER_INFO_KEY, key = "#result.id", unless = "#result==null")
    @Transactional(rollbackFor = Exception.class)
    public User create(UserCreateRequest request) {
        User user = User.builder().name(request.getName()).build();
        userMapper.add(user);
        return user;
    }

    @Override
    @CacheEvict(value = REDIS_USER_INFO_KEY, key = "#request.id")
    @Transactional(rollbackFor = Exception.class)
    public void modify(UserUpdateRequest request) {
        User user = userMapper.get(request.getId());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不存在");
        }

        user.setName(request.getName());
        userMapper.update(user);
    }

    @Override
    @CacheEvict(value = REDIS_USER_INFO_KEY, key = "#id")
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        User user = userMapper.get(id);
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不存在");
        }
        if (Boolean.TRUE.equals(user.getDeleted())) {
            throw new RuntimeException("用户已被删除，请刷新");
        }

        user.setDeleted(Boolean.TRUE);
        userMapper.update(user);
    }

}
