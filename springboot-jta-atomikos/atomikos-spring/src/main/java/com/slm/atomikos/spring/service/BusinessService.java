package com.slm.atomikos.spring.service;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Service
public class BusinessService {

    @Autowired
    @Qualifier("dataSource1")
    private AtomikosDataSourceBean dataSource1;
    @Autowired
    @Qualifier("dataSource2")
    private AtomikosDataSourceBean dataSource2;

    @Transactional(rollbackFor = Exception.class)
    public void transfer() throws SQLException {
        // 执行DB1的数据更新
        this.invokeSql(dataSource1, "update account set balance = balance + 10 where id = 1");
        // 执行DB2的数据更新
        this.invokeSql(dataSource2, "update account set balance = balance - 10 where id = 1");
    }

    private void invokeSql(DataSource dataSource, String sql) throws SQLException {
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        statement.execute(sql);
        statement.close();
        connection.close();
    }

}
