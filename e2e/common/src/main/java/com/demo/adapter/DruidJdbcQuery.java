package com.demo.adapter;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Used to query the data in the database
 */
@Slf4j
@Data
public class DruidJdbcQuery {

    private JdbcTemplate jdbcTemplateObject;
    private DataSource ds;

    DruidJdbcQuery(Properties pro) throws Exception {
        this.ds = DruidDataSourceFactory.createDataSource(pro);
        this.jdbcTemplateObject = new JdbcTemplate(ds);
    }

    DruidJdbcQuery(Map map) throws Exception {
        this.ds = DruidDataSourceFactory.createDataSource(map);
        this.jdbcTemplateObject = new JdbcTemplate(ds);
    }

    public void initByFile(Properties pro) throws Exception {
        ds = DruidDataSourceFactory.createDataSource(pro);
        this.jdbcTemplateObject = new JdbcTemplate(ds);
    }

    public void initByMap(Map map) throws Exception {
        ds = DruidDataSourceFactory.createDataSource(map);
        this.jdbcTemplateObject = new JdbcTemplate(ds);
    }

    public <T> T queryForOneObject(String sql, Class<T> requiredType) {
        RowMapper<T> rowMapper = new BeanPropertyRowMapper<T>(requiredType);
        T res = this.jdbcTemplateObject.queryForObject(sql, rowMapper);
        return res;
    }

    public <T> T queryForOneValue(String sql, Class<T> requiredType) {
        T res = this.jdbcTemplateObject.queryForObject(sql, requiredType);
        return res;
    }

    public <T> List<T> queryForList(String sql, Class<T> requiredType) {
        RowMapper<T> rowMapper = new BeanPropertyRowMapper<T>(requiredType);
        List<T> res = this.jdbcTemplateObject.query(sql, rowMapper);
        return res;
    }

}
