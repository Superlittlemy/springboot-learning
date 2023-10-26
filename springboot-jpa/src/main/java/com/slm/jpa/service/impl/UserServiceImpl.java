package com.slm.jpa.service.impl;

import com.querydsl.core.types.Predicate;
import com.slm.jpa.entity.User;
import com.slm.jpa.exception.BizException;
import com.slm.jpa.model.UserCreateRequest;
import com.slm.jpa.model.UserUpdateRequest;
import com.slm.jpa.repository.UserRepository;
import com.slm.jpa.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public User get(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new BizException("用户不存在"));
    }

    @Override
    public Page<User> pageQuery(Predicate predicate, Pageable pageable) {
        return userRepository.findAll(predicate, pageable);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(UserCreateRequest request) {
        User user = User.builder().name(request.getName()).build();
        userRepository.save(user);
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modify(UserUpdateRequest request) {
        userRepository.findById(request.getId()).ifPresentOrElse(user -> {
            user.setName(request.getName());
            userRepository.save(user);
        }, () -> {
            throw new BizException("用户不存在");
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        userRepository.findById(id).ifPresentOrElse(userRepository::delete, () -> {
            throw new BizException("用户不存在");
        });
    }

}
