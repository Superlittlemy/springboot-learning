package com.slm.atomikos.spring;

import com.slm.atomikos.spring.service.BusinessService;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.sql.SQLException;

public class AtomikosSpringTest {

    @Test
    public void test() throws SQLException {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
        BusinessService businessService = context.getBean(BusinessService.class);
        businessService.transfer();
    }

}
