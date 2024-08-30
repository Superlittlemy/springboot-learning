package com.slm.mybatis.mapper;

import com.github.pagehelper.Page;
import com.slm.mybatis.entity.User;
import com.slm.mybatis.model.Type;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

public interface UserMapper {

    List<User> queryPage(RowBounds rowBounds);

    Page<User> queryPageByPlugin();

    User getUserById(@Param("id") Long id, @Param("type") Type type);

    User insertUser(User user);

    int updateUser(User user);

    int deleteUserById(Long id);

}