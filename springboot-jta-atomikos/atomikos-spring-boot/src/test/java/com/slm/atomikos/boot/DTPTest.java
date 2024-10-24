package com.slm.atomikos.boot;

import com.slm.atomikos.boot.service.AccountService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@SpringBootTest
@RunWith(SpringRunner.class)
public class DTPTest {

    @Resource
    private AccountService accountService;

    // 正常执行
    @Test
    public void test1() {
        accountService.success();
    }

    // 异常回滚
    @Test
    public void test2() {
        accountService.failure();
    }

}
