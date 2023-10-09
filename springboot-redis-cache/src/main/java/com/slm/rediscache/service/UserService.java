package com.slm.rediscache.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.slm.rediscache.entity.User;
import com.slm.rediscache.model.UserCreateRequest;
import com.slm.rediscache.model.UserUpdateRequest;

import java.util.List;

public interface UserService {

    List<User> query();

    User get(Long id);

    User create(UserCreateRequest request);

    void modify(UserUpdateRequest request);

    void delete(Long id);

}
