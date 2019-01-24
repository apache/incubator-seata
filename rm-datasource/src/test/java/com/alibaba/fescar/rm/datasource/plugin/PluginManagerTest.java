package com.alibaba.fescar.rm.datasource.plugin;

import com.alibaba.fescar.rm.datasource.plugin.context.AttrResolveContext;
import com.alibaba.fescar.rm.datasource.plugin.context.TableMetaBeforeContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class PluginManagerTest {

    private PluginManager pluginManager;

    @Before
    public void init() {
        pluginManager = new PluginManager();
        pluginManager.addPlugins(new MycatPlugins.TableMetaBeforePlugin());
        pluginManager.addPlugins(new MycatPlugins.SqlBuildAfterPlugin());
        pluginManager.addPlugins(new MycatPlugins.LockKeyBuildAfterPlugin());
        pluginManager.addPlugins(new MycatPlugins.AttrResolvePlugin());
    }

    @Test
    public void testSqlBuildAfter() {
        String hint1 = "!mycat:schema=wz_buss_001";
        List<String> hints = Arrays.asList(hint1);
        String originSql = "select * from buss_user_info";

        String result = pluginManager.execSqlBuildAfter(hints, originSql);
        System.out.println(result);
        Assert.assertTrue(result.startsWith("/*" + hint1 + "*/"));
        Assert.assertTrue(result.endsWith(originSql));
    }

    @Test
    public void testTableMetaBefore() {
        String hint1 = "!mycat:schema=wz_buss_001";
        List<String> hints = Arrays.asList(hint1);
        String originSql = "select * from buss_user_info limit 1";

        String cacheKey = "mysql://localhost:3306/demo.buss_user_info";
        String metaQuerySql = originSql;

        TableMetaBeforeContext ctx = pluginManager.execTableMetaBefore(hints, "buss_user_info", cacheKey, metaQuerySql);
        System.out.println(ctx.getResultForCacheKey() + " -> " + ctx.getResultForMetaQuerySql());
        Assert.assertTrue(ctx.getResultForCacheKey().equals("wz_buss_001." + cacheKey));
        Assert.assertTrue(ctx.getResultForMetaQuerySql().startsWith("/*" + hint1 + "*/") && ctx.getResultForMetaQuerySql().endsWith(originSql));
    }

    @Test
    public void testLockKeyBuildAfter() {
        String hint1 = "!mycat:schema=wz_buss_001";
        List<String> hints = Arrays.asList(hint1);
        String lockKey = "buss_user_info:1,2,3";

        String result = pluginManager.execLockKeyBuildAfter(hints, null, lockKey);
        System.out.println(result);
        Assert.assertTrue(result.equals("wz_buss_001." + lockKey));
    }

    @Test
    public void testAttrResolvePlugin() {
        String hint1 = "!mycat:schema=wz_buss_001";
        List<String> hints = Arrays.asList(hint1);
        String sqlText = "select * from buss_user_info";

        AttrResolveContext result = pluginManager.execAttrResolve(hints, sqlText);
        Assert.assertFalse(result.getAttrForSavepointSupport());
    }


}
