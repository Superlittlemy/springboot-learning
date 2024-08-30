package com.slm.mybatis;

import com.slm.mybatis.entity.App;
import com.slm.mybatis.entity.User;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class Q4_AnnotationTest {

    // 注解：查询/resultMap/cache
    @Test
    public void mapperAnnotation() throws IOException {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            App app = sqlSession.selectOne("com.slm.mybatis.mapper.AppMapper.getAppByIdWithAnnotation", 1);
            System.out.println("app " + app);
        }
    }

    // 注解：插入并返回key
    @Test
    public void insert() throws IOException {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            App app = new App();
            app.setName("平多多2");
            User user = new User();
            user.setId(1L);
            app.setOwner(user);
            int num = sqlSession.insert("com.slm.mybatis.mapper.AppMapper.insertApp", app);
            System.out.println("插入成功 " + (num == 1));
            System.out.println("app " + app);
        }
    }

}
