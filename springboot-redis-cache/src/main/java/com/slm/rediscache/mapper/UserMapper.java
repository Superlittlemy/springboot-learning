package com.slm.rediscache.mapper;

import com.slm.rediscache.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {

    List<User> query();

    User get(Long id);

    void add(User user);

    void update(User user);

}