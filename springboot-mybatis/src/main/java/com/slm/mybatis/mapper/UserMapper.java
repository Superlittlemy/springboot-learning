package com.slm.mybatis.mapper;

import com.slm.mybatis.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {

    void add(User user);

    List<User> query();

    void update(User user);

}