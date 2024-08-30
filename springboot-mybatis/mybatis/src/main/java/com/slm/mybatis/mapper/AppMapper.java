package com.slm.mybatis.mapper;

import com.slm.mybatis.entity.App;
import com.slm.mybatis.entity.User;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.mapping.FetchType;

import java.util.ArrayList;

@CacheNamespace
public interface AppMapper {

    App getAppById(Integer id);

    @Results(id = "getAppByIdWithAnnotation", value = {
            @Result(id = true, property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "owner", column = "owner", javaType = User.class,
                    one = @One(resultMap = "com.slm.mybatis.mapper.UserMapper.user", columnPrefix = "user_", fetchType = FetchType.LAZY)),
            @Result(property = "users", column = "owner", javaType = ArrayList.class,
                    many = @Many(select = "com.slm.mybatis.mapper.UserMapper.getUserById", fetchType = FetchType.LAZY))
    })
    @Select(" select " +
            "        a.*, " +
            "        u.id as user_id, " +
            "        u.name as user_name, " +
            "        u.birthday as user_birthday " +
            "    from app a " +
            "    left join user u on a.owner = u.id " +
            "    where a.id = #{id} ")
    App getAppByIdWithAnnotation(Integer id);

    @Options(useGeneratedKeys = true, keyProperty = "id")
//    @SelectKey(statement = "select LAST_INSERT_ID();", keyProperty = "id", resultType = int.class, before = false)
    @Insert(" insert into app (name, owner) values (#{name}, #{owner.id}) ")
    int insertApp(App app);

}
