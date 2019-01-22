package com.alibaba.fescar.spring.test;

import com.alibaba.fescar.rm.datasource.DataSourceProxy;
import com.alibaba.fescar.rm.datasource.plugin.PluginManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

public class MycatBusinessServiceTest {

    @Before
    public void init() {
        JdbcTemplate jdbcTemplate = DataSourceFactory.createRawJdbcTemplate();

        jdbcTemplate.execute("/*!mycat:schema=demo_storage*/ DELETE FROM storage_tbl WHERE commodity_code='C1000'");
        jdbcTemplate.execute("/*!mycat:schema=demo_storage*/ INSERT INTO storage_tbl(id,commodity_code,count) VALUES(10000,'C1000',10000)");

        jdbcTemplate.execute("/*!mycat:schema=demo_account*/ DELETE FROM account_tbl WHERE user_id='U1000'");
        jdbcTemplate.execute("/*!mycat:schema=demo_account*/ INSERT INTO account_tbl(id,user_id,money) VALUES(10000,'U1000',10000)");

        jdbcTemplate.execute("/*!mycat:schema=demo_order*/ DELETE FROM order_tbl WHERE user_id='U1000'");


    }

    @Test
    public void testRollback() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:bean-business.xml");
        MycatBusinessService businessService = (MycatBusinessService) context.getBean("mycatBusinessService");
        try {
            businessService.purchaseRollback("U1000", "C1000", 10);
        } catch (MycatBusinessService.RollbackException ex) {
            System.out.println(ex.getMessage());
        }
        int ret;
        JdbcTemplate jdbcTemplate = DataSourceFactory.createMycatJdbcTemplate();

        ret = jdbcTemplate.queryForInt("/*!mycat:schema=demo_storage*/ SELECT count(1) FROM storage_tbl WHERE commodity_code='C1000' AND count=10000 ");
        Assert.assertTrue("storage_tbl assert failure", ret == 1);

        ret = jdbcTemplate.queryForInt("/*!mycat:schema=demo_account*/ SELECT count(1) FROM account_tbl WHERE user_id='U1000' AND money=10000 ");
        Assert.assertTrue("account_tbl assert failure", ret == 1);

        ret = jdbcTemplate.queryForInt("/*!mycat:schema=demo_order*/ SELECT count(1) FROM order_tbl WHERE user_id='U1000'");
        Assert.assertTrue("order_tbl assert failure", ret == 0);

    }

    @Test
    public void testCommit() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:bean-business.xml");
        MycatBusinessService businessService = (MycatBusinessService) context.getBean("mycatBusinessService");
        businessService.purchase("U1000", "C1000", 10);

        int ret;
        JdbcTemplate jdbcTemplate = DataSourceFactory.createMycatJdbcTemplate();

        ret = jdbcTemplate.queryForInt("/*!mycat:schema=demo_storage*/ SELECT count(1) FROM storage_tbl WHERE commodity_code='C1000' AND count=10000");
        Assert.assertTrue("storage_tbl assert failure", ret == 0);

        ret = jdbcTemplate.queryForInt("/*!mycat:schema=demo_account*/ SELECT count(1) FROM account_tbl WHERE user_id='U1000' AND money=10000 ");
        Assert.assertTrue("account_tbl assert failure", ret == 0);

        ret = jdbcTemplate.queryForInt("/*!mycat:schema=demo_order*/ SELECT count(1) FROM order_tbl WHERE user_id='U1000'");
        Assert.assertTrue("order_tbl assert failure", ret == 1);
    }


}
