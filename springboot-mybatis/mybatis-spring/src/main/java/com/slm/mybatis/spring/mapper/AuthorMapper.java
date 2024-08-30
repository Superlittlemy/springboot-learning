package com.slm.mybatis.spring.mapper;

import com.slm.mybatis.spring.entity.Author;

public interface AuthorMapper {

    Author findById(Long id);

}
