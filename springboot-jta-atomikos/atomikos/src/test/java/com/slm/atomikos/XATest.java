package com.slm.atomikos;

import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.jdbc.AtomikosDataSourceBean;
import org.junit.Test;

import javax.sql.DataSource;
import javax.transaction.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class XATest {

    @Test
    public void success() throws SystemException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        UserTransaction ut = new UserTransactionImp();
        AtomikosDataSourceBean dataSource1 = this.generateAtomikosDataSource("dataSource1", "root", "123456", "jdbc:mysql://localhost:3306/db_01");
        AtomikosDataSourceBean dataSource2 = this.generateAtomikosDataSource("dataSource2", "root", "123456", "jdbc:mysql://localhost:3306/db_02");
        boolean rollback = false;
        try {
            // Atomikos 开启全局事务
            ut.begin();
            // 执行DB1的数据更新
            this.invokeSql(dataSource1, "update account set balance = balance + 10 where id = 1");
            // 执行DB2的数据更新
            this.invokeSql(dataSource2, "update account set balance = balance - 10 where id = 1");
        } catch (Exception e) {
            rollback = true;
        } finally {
            // Commit/Rollback 提交阶段
            if (rollback) {
                ut.rollback();
            } else {
                ut.commit();
            }
        }
    }

    public AtomikosDataSourceBean generateAtomikosDataSource(String resourceName, String user, String password, String url) {
        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);
        properties.setProperty("url", url);
        AtomikosDataSourceBean dataSource = new AtomikosDataSourceBean();
        dataSource.setUniqueResourceName(resourceName);
        dataSource.setXaDataSourceClassName("com.mysql.cj.jdbc.MysqlXADataSource");
        dataSource.setXaProperties(properties);
        return dataSource;
    }

    public void invokeSql(DataSource dataSource, String sql) throws SQLException {
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        statement.execute(sql);
        statement.close();
        connection.close();
    }

}
