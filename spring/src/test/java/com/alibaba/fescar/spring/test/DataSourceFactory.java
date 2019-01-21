package com.alibaba.fescar.spring.test;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fescar.rm.datasource.DataSourceProxy;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;


public class DataSourceFactory {

    public static DataSource createDataSource() {
        Properties properties = new Properties();
        try {
            InputStream in = DataSourceFactory.class.getClassLoader().getResourceAsStream("datasource.properties");
            properties.load(in);
            InputStream localIn = DataSourceFactory.class.getClassLoader().getResourceAsStream("datasource-local.properties");
            if (localIn != null) {
                properties.load(localIn);
            }
        } catch (IOException e) {
            throw new RuntimeException("read datasource.properties error", e);
        }

        String dbUrl = properties.getProperty("url");
        String username = properties.getProperty("username");
        String password = properties.getProperty("password");
        String driverClassName = properties.getProperty("driverClassName");
        int initialSize = Integer.parseInt(properties.getProperty("initialSize"));
        int minIdle = Integer.parseInt(properties.getProperty("minIdle"));
        int maxActive = Integer.parseInt(properties.getProperty("maxActive"));
        int maxWait = Integer.parseInt(properties.getProperty("maxWait"));
        int timeBetweenEvictionRunsMillis = Integer.parseInt(properties.getProperty("timeBetweenEvictionRunsMillis"));
        int minEvictableIdleTimeMillis = Integer.parseInt(properties.getProperty("minEvictableIdleTimeMillis"));
        String validationQuery = properties.getProperty("validationQuery");
        boolean testWhileIdle = Boolean.parseBoolean(properties.getProperty("testWhileIdle"));
        boolean testOnBorrow = Boolean.parseBoolean(properties.getProperty("testOnBorrow"));
        boolean testOnReturn = Boolean.parseBoolean(properties.getProperty("testOnReturn"));
        String filters = properties.getProperty("filters");

        DruidDataSource datasource = new DruidDataSource();
        datasource.setUrl(dbUrl);
        datasource.setUsername(username);
        datasource.setPassword(password);
        datasource.setDriverClassName(driverClassName);
        datasource.setInitialSize(initialSize);
        datasource.setMinIdle(minIdle);
        datasource.setMaxActive(maxActive);
        datasource.setMaxWait(maxWait);
        datasource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        datasource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        datasource.setValidationQuery(validationQuery);
        datasource.setTestWhileIdle(testWhileIdle);
        datasource.setTestOnBorrow(testOnBorrow);
        datasource.setTestOnReturn(testOnReturn);
        try {
            datasource.setFilters(filters);
        } catch (SQLException e) {
            throw new RuntimeException("druid configuration initialization filter", e);
        }
        return datasource;
    }


    public static DataSourceProxy createDataSourceProxy() {
        Properties properties = new Properties();
        try {
            InputStream in = DataSourceFactory.class.getClassLoader().getResourceAsStream("datasource.properties");
            properties.load(in);
            InputStream localIn = DataSourceFactory.class.getClassLoader().getResourceAsStream("datasource-local.properties");
            if (localIn != null) {
                properties.load(localIn);
            }
        } catch (IOException e) {
            throw new RuntimeException("read datasource.properties error", e);
        }

        String dbUrl = properties.getProperty("url");
        String username = properties.getProperty("username");
        String password = properties.getProperty("password");
        String driverClassName = properties.getProperty("driverClassName");
        int initialSize = Integer.parseInt(properties.getProperty("initialSize"));
        int minIdle = Integer.parseInt(properties.getProperty("minIdle"));
        int maxActive = Integer.parseInt(properties.getProperty("maxActive"));
        int maxWait = Integer.parseInt(properties.getProperty("maxWait"));
        int timeBetweenEvictionRunsMillis = Integer.parseInt(properties.getProperty("timeBetweenEvictionRunsMillis"));
        int minEvictableIdleTimeMillis = Integer.parseInt(properties.getProperty("minEvictableIdleTimeMillis"));
        String validationQuery = properties.getProperty("validationQuery");
        boolean testWhileIdle = Boolean.parseBoolean(properties.getProperty("testWhileIdle"));
        boolean testOnBorrow = Boolean.parseBoolean(properties.getProperty("testOnBorrow"));
        boolean testOnReturn = Boolean.parseBoolean(properties.getProperty("testOnReturn"));
        String filters = properties.getProperty("filters");

        DruidDataSource datasource = new DruidDataSource();
        datasource.setUrl(dbUrl);
        datasource.setUsername(username);
        datasource.setPassword(password);
        datasource.setDriverClassName(driverClassName);
        datasource.setInitialSize(initialSize);
        datasource.setMinIdle(minIdle);
        datasource.setMaxActive(maxActive);
        datasource.setMaxWait(maxWait);
        datasource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        datasource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        datasource.setValidationQuery(validationQuery);
        datasource.setTestWhileIdle(testWhileIdle);
        datasource.setTestOnBorrow(testOnBorrow);
        datasource.setTestOnReturn(testOnReturn);
        try {
            datasource.setFilters(filters);
        } catch (SQLException e) {
            throw new RuntimeException("druid configuration initialization filter", e);
        }
        DataSourceProxy dataSourceProxy = new DataSourceProxy(datasource);
        return dataSourceProxy;
    }


    public static JdbcTemplate createJdbcTemplate() {
        DataSourceProxy dataSourceProxy = createDataSourceProxy();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSourceProxy);
        return jdbcTemplate;
    }

    public static JdbcTemplate createRawJdbcTemplate() {
        DataSource dataSource = createDataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return jdbcTemplate;
    }

}