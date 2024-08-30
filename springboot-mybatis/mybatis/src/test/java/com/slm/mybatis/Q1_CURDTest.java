package com.slm.mybatis;

import com.slm.mybatis.entity.User;
import com.slm.mybatis.model.Type;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class Q1_CURDTest {

    @Test
    public void select() throws IOException {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            Map<String, Object> params = new HashMap<>();
            params.put("id", 1);
            params.put("type", Type.ORDER);
            User user = sqlSession.selectOne("com.slm.mybatis.mapper.UserMapper.getUserById", params);
            System.out.println("user " + user);
        }
    }

    @Test
    public void insert() throws IOException {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            User user = new User();
            user.setName("删除");
            user.setBirthday(LocalDate.of(2024, 1, 1));
            user.setType(Type.ORDER);
            sqlSession.insert("com.slm.mybatis.mapper.UserMapper.insertUser", user);
            System.out.println(user.getId());
        }
    }

    @Test
    public void update() throws IOException {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            User user = new User();
            user.setId(1L);
            user.setName("蜡笔小新");
            int num = sqlSession.update("com.slm.mybatis.mapper.UserMapper.updateUser", user);
            System.out.println("更新成功 " + (num == 1));
        }
    }

    @Test
    public void delete() throws IOException {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            int num = sqlSession.delete("com.slm.mybatis.mapper.UserMapper.deleteUserById", 5);
            System.out.println("删除成功 " + (num == 1));
        }
    }

}
