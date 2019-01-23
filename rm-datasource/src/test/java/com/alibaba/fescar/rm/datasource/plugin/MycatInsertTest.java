package com.alibaba.fescar.rm.datasource.plugin;


import com.alibaba.fescar.core.context.RootContext;
import com.alibaba.fescar.rm.datasource.support.DataSourceFactory;
import com.alibaba.fescar.rm.datasource.DataSourceProxy;
import org.junit.Assert;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MycatInsertTest {
    @Test
    public void test1() throws SQLException {
        String xid = "localhost:8091:19288939";
        RootContext.bind(xid);
        DataSourceProxy dataSourceProxy = DataSourceFactory.createDataSourceProxy();
        dataSourceProxy.getConnection().bind(xid);

        PluginManager pluginManager = dataSourceProxy.getPluginManager();
        pluginManager.addPlugins(new MycatPlugins.TableMetaBeforePlugin());
        pluginManager.addPlugins(new MycatPlugins.SqlBuildAfterPlugin());
        pluginManager.addPlugins(new MycatPlugins.LockKeyBuildAfterPlugin());

        String sql = "/*!mycat:schema=demo_order*/SELECT count(1) FROM order_tbl WHERE id=17";
        PreparedStatement statement = dataSourceProxy.getConnection().prepareStatement(sql);
        ResultSet rs = statement.executeQuery();
        Assert.assertTrue(rs.next());
        int c = rs.getInt(1);
        Assert.assertTrue(c == 1);
    }
}
