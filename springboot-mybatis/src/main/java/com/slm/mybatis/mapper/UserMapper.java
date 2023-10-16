package com.slm.mybatis.mapper;

import com.slm.mybatis.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {

    Long count(String name);

    void add(User user);

    List<User> query(String name, int size, int start);

    void update(User user);

}