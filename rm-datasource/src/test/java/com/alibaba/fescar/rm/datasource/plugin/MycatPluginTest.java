package com.alibaba.fescar.rm.datasource.plugin;


import com.alibaba.fescar.core.context.RootContext;
import com.alibaba.fescar.rm.datasource.DataSourceFactory;
import com.alibaba.fescar.rm.datasource.DataSourceProxy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

public class MycatPluginTest {

    static final Logger LOGGER = LoggerFactory.getLogger(MycatPluginTest.class);

    DataSourceProxy dataSourceProxy;

    @Before
    public void init() throws SQLException {
        String xid = "localhost:8091:19288939";
        dataSourceProxy = DataSourceFactory.createDataSourceProxy();
        dataSourceProxy.getConnection().bind(xid);

        PluginManager pluginManager = dataSourceProxy.getPluginManager();
        pluginManager.addPlugins(new MycatPlugins.TableMetaBeforePlugin());
        pluginManager.addPlugins(new MycatPlugins.SqlBuildAfterPlugin());
        pluginManager.addPlugins(new MycatPlugins.LockKeyBuildAfterPlugin());
        pluginManager.addPlugins(new MycatPlugins.AttrResolvePlugin());

        String sql = "/*!mycat:schema=demo_order*/" +
                "INSERT INTO order_tbl(id,user_id,commodity_code,COUNT,money)\n" +
                "    SELECT 0,'U_MycatPluginTest','C1000',10,2000\n" +
                "    FROM dual\n" +
                "    WHERE NOT exists(\n" +
                "        SELECT 1 FROM order_tbl WHERE user_id='U_MycatPluginTest'\n" +
                "    );";
        CallableStatement statement = dataSourceProxy.getConnection().prepareCall(sql);
        statement.execute();

        RootContext.bind(xid);

    }

    @Test
    public void testDelete() throws SQLException {

        String sql = "/*!mycat:schema=demo_order*/DELETE FROM order_tbl WHERE user_id='U_MycatPluginTest'";
        PreparedStatement statement = dataSourceProxy.getConnection().prepareStatement(sql);
        statement.execute();

        sql = "/*!mycat:schema=demo_order*/SELECT count(1) FROM order_tbl WHERE user_id='U_MycatPluginTest'";
        statement = dataSourceProxy.getConnection().prepareStatement(sql);
        ResultSet rs = statement.executeQuery();
        Assert.assertTrue(rs.next());
        Assert.assertTrue(rs.getInt(1) == 0);
    }

    @Test
    public void testUpdate() throws SQLException {
        Integer money = Math.abs(new Random(System.currentTimeMillis()).nextInt());
        LOGGER.info("set money={}", money);

        String sql = "/*!mycat:schema=demo_order*/UPDATE order_tbl SET money=? WHERE user_id='U_MycatPluginTest'";
        PreparedStatement statement = dataSourceProxy.getConnection().prepareStatement(sql);
        statement.setInt(1, money);
        statement.execute();

        sql = "/*!mycat:schema=demo_order*/SELECT count(1) FROM order_tbl WHERE money=? AND user_id='U_MycatPluginTest'";
        statement = dataSourceProxy.getConnection().prepareStatement(sql);
        statement.setInt(1, money);
        ResultSet rs = statement.executeQuery();
        Assert.assertTrue(rs.next());
        Assert.assertTrue(rs.getInt(1) > 0);
    }

    @Test
    public void testSelect() throws SQLException {
        String sql = "/*!mycat:schema=demo_order*/SELECT count(1) FROM order_tbl WHERE user_id='U_MycatPluginTest'";
        PreparedStatement statement = dataSourceProxy.getConnection().prepareStatement(sql);
        ResultSet rs = statement.executeQuery();
        Assert.assertTrue(rs.next());
        int c = rs.getInt(1);
        Assert.assertTrue(c >= 1);
    }

    @Test
    public void testSelectForUpdate() throws SQLException {
        String sql = "/*!mycat:schema=demo_order*/SELECT count(1) FROM order_tbl WHERE user_id='U_MycatPluginTest' FOR UPDATE";
        PreparedStatement statement = dataSourceProxy.getConnection().prepareStatement(sql);
        ResultSet rs = statement.executeQuery();
        Assert.assertTrue(rs.next());
        int c = rs.getInt(1);
        Assert.assertTrue(c >= 1);
    }
}
