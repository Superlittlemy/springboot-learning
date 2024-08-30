package com.slm.mybatis.spring.mapper;

import com.slm.mybatis.spring.entity.Blog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

public interface BlogMapper {

    Blog findById(int id);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert(" insert into blog (title, author, create_time) values (#{title}, #{author.id}, #{createTime}) ")
    int insert(Blog blog);

}
