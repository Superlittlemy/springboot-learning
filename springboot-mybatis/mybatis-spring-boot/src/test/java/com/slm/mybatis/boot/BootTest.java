package com.slm.mybatis.boot;

import com.slm.mybatis.boot.entity.User;
import com.slm.mybatis.boot.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class BootTest {

    @Autowired
    UserService userService;

    @Test
    void pageQuery() {
        List<User> query = userService.query("蜡笔小新", 1, 10);
        System.out.println(query);
    }

}
