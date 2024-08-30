package com.slm.mybatis;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.slm.mybatis.entity.User;
import com.slm.mybatis.mapper.UserMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Q3_PageQuery {

    /**
     * 基于 RowBounds 实现（逻辑分页）
     *
     * @throws IOException
     */
    @Test
    public void rowBoundsPage() throws IOException {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            UserMapper mapper = sqlSession.getMapper(UserMapper.class);
            List<User> users = mapper.queryPage(new RowBounds(1, 1));
            System.out.println("user " + users);
        }
    }

    /**
     * 基于 PageHelper 插件实现（物理分页）
     *
     * @throws IOException
     */
    @Test
    public void pagehelperQuery() throws IOException {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            UserMapper mapper = sqlSession.getMapper(UserMapper.class);
            // mapper 接口方法调用（推荐）
            PageHelper.startPage(1, 1);
            Page<User> users1 = mapper.queryPageByPlugin();
            System.out.println("user1 " + users1);
            // ISelect 方法调用（推荐）
            Page<User> users = PageHelper.startPage(1, 1).doSelectPage(mapper::queryPageByPlugin);
            System.out.println("user " + users);
        }
    }

}
