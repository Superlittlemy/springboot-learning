package com.slm.jpa.service;

import com.querydsl.core.types.Predicate;
import com.slm.jpa.entity.User;
import com.slm.jpa.model.UserCreateRequest;
import com.slm.jpa.model.UserUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    User get(Long id);

    Page<User> pageQuery(Predicate predicate, Pageable pageable);

    Long save(UserCreateRequest request);

    void modify(UserUpdateRequest request);

    void delete(Long id);

}
