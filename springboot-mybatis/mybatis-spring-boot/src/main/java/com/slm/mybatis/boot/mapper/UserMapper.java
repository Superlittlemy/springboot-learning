package com.slm.mybatis.boot.mapper;

import com.slm.mybatis.boot.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper {

    User getUserById(Long id);

    Long count(String name);

    void add(User user);

    @Select("select * from user where name like concat('%', #{name}, '%')")
    List<User> query(String name);

    void update(User user);

}