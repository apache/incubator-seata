package com.alibaba.fescar.rm.datasource.plugin;

import com.alibaba.fescar.rm.datasource.plugin.context.LockKeyBuildAfterContext;
import com.alibaba.fescar.rm.datasource.plugin.context.SqlBuildAfterContext;
import com.alibaba.fescar.rm.datasource.plugin.context.TableMetaBeforeContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PluginManagerTest {

    private PluginManager pluginManager;

    @Before
    public void init() {
        pluginManager = new PluginManager();
    }

    @Test
    public void testSqlBuildAfter() {
        String hint1 = "/*!mycat:schema=wz_buss_001*/";
        List<String> hints = Arrays.asList(hint1);
        String originSql = "select * from buss_user_info";

        String result = pluginManager.execSqlBuildAfter(hints, originSql);
        System.out.println(result);
        Assert.assertTrue(result.equals(originSql));

        pluginManager.addPlugins(new SqlBuildAfterPlugin());
        result = pluginManager.execSqlBuildAfter(hints, originSql);
        System.out.println(result);
        Assert.assertTrue(result.startsWith(hint1));
        Assert.assertTrue(result.endsWith(originSql));
    }

    @Test
    public void testTableMetaBefore() {
        String hint1 = "/*!mycat:schema=wz_buss_001*/";
        List<String> hints = Arrays.asList(hint1);
        String originSql = "select * from buss_user_info limit 1";

        String cacheKey = "mysql://localhost:3306/demo.buss_user_info";
        String metaQuerySql = originSql;

        TableMetaBeforeContext ctx = pluginManager.execTableMetaBefore(hints, "buss_user_info", cacheKey, metaQuerySql);
        System.out.println(cacheKey + " -> " + metaQuerySql);
        Assert.assertTrue(cacheKey.equals(ctx.getResultForCacheKey()));
        Assert.assertTrue(metaQuerySql.equals(ctx.getResultForMetaQuerySql()));

        pluginManager.addPlugins(new TableMetaBeforePlugin());
        ctx = pluginManager.execTableMetaBefore(hints, "buss_user_info", cacheKey, metaQuerySql);
        System.out.println(ctx.getResultForCacheKey() + " -> " + ctx.getResultForMetaQuerySql());
        Assert.assertTrue(ctx.getResultForCacheKey().equals("wz_buss_001." + cacheKey));
        Assert.assertTrue(ctx.getResultForMetaQuerySql().startsWith(hint1) && ctx.getResultForMetaQuerySql().endsWith(originSql));
    }

    @Test
    public void testLockKeyBuildAfter() {
        String hint1 = "/*!mycat:schema=wz_buss_001*/";
        List<String> hints = Arrays.asList(hint1);
        String lockKey = "buss_user_info:1,2,3";

        String result = pluginManager.execSqlBuildAfter(hints, lockKey);
        System.out.println(result);
        Assert.assertTrue(result.equals(lockKey));

        pluginManager.addPlugins(new LockKeyBuildAfterPlugin());
        result = pluginManager.execLockKeyBuildAfter(hints, null, lockKey);
        System.out.println(result);
        Assert.assertTrue(result.equals("wz_buss_001." + lockKey));
    }

    static class LockKeyBuildAfterPlugin implements Plugin {

        @Override
        public List<String> supportedActions() {
            return Arrays.asList(PluginConstants.ACTION_LOCK_KEY_BUILD_AFTER);
        }

        @Override
        public Object proc(PluginContext context) {
            LockKeyBuildAfterContext ctx = (LockKeyBuildAfterContext) context;
            List<String> sqlHints = ctx.getSqlHints();
            String schema = resolveSchema(sqlHints);
            String lockKey = ctx.getResultLockKey();
            if (schema != "") {
                return schema + "." + lockKey;
            } else {
                return lockKey;
            }
        }

        private String resolveSchema(List<String> sqlHints) {
            String schema = "";
            for (String hint : sqlHints) {
                Matcher matcher = pattern.matcher(hint);
                if (matcher.find()) {
                    schema = matcher.group(1);
                }
            }
            return schema;
        }

        private static final Pattern pattern = Pattern.compile("schema=([0-9a-zA-Z_]{1,})");

    }

    static class SqlBuildAfterPlugin implements Plugin {

        @Override
        public List<String> supportedActions() {
            return Arrays.asList(PluginConstants.ACTION_SQL_BUILD_AFTER);
        }

        @Override
        public Object proc(PluginContext context) {
            SqlBuildAfterContext ctx = (SqlBuildAfterContext) context;
            String originSql = ctx.getResultSql();
            List<String> sqlHints = ctx.getSqlHints();
            StringBuilder sqlTxt = new StringBuilder();
            for (Integer i = 0; i < sqlHints.size(); i++) {
                sqlTxt.append(sqlHints.get(i));
            }
            return sqlTxt.toString() + " " + originSql;
        }
    }

    static class TableMetaBeforePlugin implements Plugin {

        @Override
        public List<String> supportedActions() {
            return Arrays.asList(PluginConstants.ACTION_TABLE_META_BEFORE);
        }

        @Override
        public Object proc(PluginContext context) {
            TableMetaBeforeContext ctx = (TableMetaBeforeContext) context;
            String cacheKey = ctx.getResultForCacheKey();
            String metaQuerySql = ctx.getResultForMetaQuerySql();
            List<String> sqlHints = ctx.getSqlHints();

            String schema = resolveSchema(sqlHints);
            StringBuilder sqlTxt = new StringBuilder();
            for (Integer i = 0; i < sqlHints.size(); i++) {
                sqlTxt.append(sqlHints.get(i));
            }
            metaQuerySql = sqlTxt.toString() + " " + metaQuerySql;

            if (schema != "") {
                cacheKey = schema + "." + cacheKey;
            }

            ctx.setResultData(cacheKey, metaQuerySql);
            return ctx.getResult();
        }

        private String resolveSchema(List<String> sqlHints) {
            String schema = "";
            for (String hint : sqlHints) {
                Matcher matcher = pattern.matcher(hint);
                if (matcher.find()) {
                    schema = matcher.group(1);
                }
            }
            return schema;
        }

        private static final Pattern pattern = Pattern.compile("schema=([0-9a-zA-Z_]{1,})");
    }


}
