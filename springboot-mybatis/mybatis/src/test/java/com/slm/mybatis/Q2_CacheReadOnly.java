package com.slm.mybatis;

import com.slm.mybatis.entity.User;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * 二级缓存 readOnly 模式的问题模拟
 */
public class Q2_CacheReadOnly {

    @Test
    public void test() throws IOException {
        // userMapper.xml 开启二级缓存并配置readOnly=true
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        // 查询两次user信息，后者信息来自前者二级缓存的<实例>数据
        User user1;
        User user2;
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            user1 = sqlSession.selectOne("com.slm.mybatis.mapper.UserMapper.getUserById", 1);
            System.out.println("user1 " + user1);
        }
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            user2 = sqlSession.selectOne("com.slm.mybatis.mapper.UserMapper.getUserById", 1);
            System.out.println("user2 " + user2);
        }
        // 修改user1里的数据
        user1.setName("change");
        System.out.println("user1 " + user1);
        System.out.println("user2 " + user2);
        // 打印发现user2的数据也被修改了，这就是readOnly带来的坏处
        // 只读模式缓存的是查询实例，取出的缓存是被引用而不是硬拷贝，所以数据上是共享的，一处修改N处联动
        // 如果非只读数据被用于操作，可能出现更新了不该更新的数据等问题
    }

}
