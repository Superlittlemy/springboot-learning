package com.slm.mybatis.spring;

import com.slm.mybatis.spring.entity.Author;
import com.slm.mybatis.spring.entity.Blog;
import com.slm.mybatis.spring.mapper.AuthorMapper;
import com.slm.mybatis.spring.mapper.BlogMapper;
import com.slm.mybatis.spring.service.BlogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.time.LocalDateTime;

@Slf4j
public class MyBatisSpringTest {

    /**
     * Spring 集成 MyBatis
     */
    @Test
    public void loadMyBatis() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
        SqlSessionFactory sqlSessionFactory = (SqlSessionFactory) context.getBean("sqlSessionFactory");
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            BlogMapper mapper = sqlSession.getMapper(BlogMapper.class);
            Blog blog = mapper.findById(1);
            System.out.println("blog = " + blog);
        }
    }

    /**
     * 自动扫描 Mapper Bean
     */
    @Test
    public void scanMapper() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
        BlogMapper blogMapper = context.getBean(BlogMapper.class);
        Blog blog = blogMapper.findById(1);
        System.out.println("blog = " + blog);
    }

    /**
     * 编程式事务
     */
    @Test
    public void programmaticTransaction() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
        SqlSessionFactory sqlSessionFactory = (SqlSessionFactory) context.getBean("sqlSessionFactory");
        PlatformTransactionManager transactionManager = context.getBean(PlatformTransactionManager.class);
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            AuthorMapper authorMapper = sqlSession.getMapper(AuthorMapper.class);
            Author author = authorMapper.findById(1L);

            BlogMapper blogMapper = sqlSession.getMapper(BlogMapper.class);
            Blog blog = new Blog();
            blog.setTitle("Spring 实战");
            blog.setAuthor(author);
            blog.setCreateTime(LocalDateTime.now());
            int flag = blogMapper.insert(blog);

            transactionManager.commit(status);
            log.info("新增成功 {} id = {}", flag == 1, blog.getId());
        } catch (Exception e) {
            transactionManager.rollback(status);
        }
    }

    /**
     * 声明式事务
     */
    @Test
    public void declarativeTransaction() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");

        AuthorMapper authorMapper = context.getBean(AuthorMapper.class);
        Author author = authorMapper.findById(1L);

        BlogService blogService = context.getBean(BlogService.class);
        Blog blog = new Blog();
        blog.setTitle("Spring 实战");
        blog.setAuthor(author);
        blog.setCreateTime(LocalDateTime.now());
        blogService.insert(blog);
    }

}
