package com.slm.mybatisplus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.slm.mybatisplus.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.Objects;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    default Page<User> selectPages(Page<User> page, String name, Boolean deleted) {
        return this.selectPage(page,
                Wrappers.<User>lambdaQuery()
                        .and(StringUtils.isNotBlank(name), i -> i.like(User::getName, name))
                        .and(Objects.nonNull(deleted), i -> i.eq(User::getDeleted, deleted))
        );
    }

}