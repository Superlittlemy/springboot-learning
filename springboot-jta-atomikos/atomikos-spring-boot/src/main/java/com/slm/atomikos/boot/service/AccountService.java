package com.slm.atomikos.boot.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class AccountService {

    @Resource
    @Qualifier("primaryJdbcTemplate")
    private JdbcTemplate primaryJdbcTemplate;

    @Resource
    @Qualifier("secondaryJdbcTemplate")
    private JdbcTemplate secondaryJdbcTemplate;

    @Transactional(rollbackFor = Exception.class)
    public void success() {
        int money = 10;
        String sql = "update account set balance = balance + ? where id = ?";
        primaryJdbcTemplate.update(sql, new Object[]{-money, 1});
        secondaryJdbcTemplate.update(sql, new Object[]{money, 1});
    }

    @Transactional(rollbackFor = Exception.class)
    public void failure() {
        int money = 10;
        String sql = "update account set balance = balance + ? where id = ?";
        primaryJdbcTemplate.update(sql, new Object[]{-money, 1});
        secondaryJdbcTemplate.update(sql, new Object[]{money, 1});
        throw new RuntimeException();
    }

}
