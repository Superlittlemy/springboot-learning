package com.slm.mybatis.spring.service;

import com.slm.mybatis.spring.entity.Blog;
import com.slm.mybatis.spring.event.BlogEvent;
import com.slm.mybatis.spring.mapper.BlogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlogService {

    private final BlogMapper blogMapper;
    @Autowired
    ApplicationContext applicationContext;

    @Transactional(rollbackFor = Exception.class)
    public void insert(Blog blog) {
        int flag = blogMapper.insert(blog);
        log.info("新增成功 {} id = {}", flag == 1, blog.getId());
        applicationContext.publishEvent(new BlogEvent(1));
        applicationContext.getBean(this.getClass()).selectById(1);
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public Blog selectById(int id) {
        Blog byId = blogMapper.findById(id);
        applicationContext.publishEvent(new BlogEvent(2));
        return byId;
    }


}
