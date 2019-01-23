package com.alibaba.fescar.rm.datasource.plugin;

import com.alibaba.fescar.rm.datasource.plugin.context.LockKeyBuildAfterContext;
import com.alibaba.fescar.rm.datasource.plugin.context.SqlBuildAfterContext;
import com.alibaba.fescar.rm.datasource.plugin.context.TableMetaBeforeContext;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MycatPlugins {
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
                sqlTxt.append("/*" + sqlHints.get(i) + "*/");
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
                sqlTxt.append("/*" + sqlHints.get(i) + "*/");
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
