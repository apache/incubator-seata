/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.rm.datasource.exec;

import co.faao.plugin.starter.dubbo.util.ThreadLocalTools;
import co.faao.plugin.starter.seata.util.DataTraceLogUtil;
import co.faao.plugin.starter.seata.util.ElasticsearchUtil;
import co.faao.plugin.starter.seata.util.SeataXidWorker;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import io.seata.core.constants.Seata;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.StringUtils;
import io.seata.common.util.CollectionUtils;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.rm.datasource.StatementProxy;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLType;
import io.seata.rm.datasource.sql.SQLVisitorFactory;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.BranchUndoLog;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.sqlparser.druid.SQLOperateRecognizerHolder;
import io.seata.sqlparser.druid.SQLOperateRecognizerHolderFactory;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.lang.time.DateFormatUtils;
import java.util.List;

/**
 * The type Execute template.
 *
 * @author sharajava
 */
public class ExecuteTemplate {

    /**
     * Execute t.
     *
     * @param <T>               the type parameter
     * @param <S>               the type parameter
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param args              the args
     * @return the t
     * @throws SQLException the sql exception
     */
    public static <T, S extends Statement> T execute(StatementProxy<S> statementProxy,
                                                     StatementCallback<T, S> statementCallback,
                                                     Object... args) throws SQLException {
        return execute(null, statementProxy, statementCallback, args);
    }

    /**
     * Execute t.
     *
     * @param <T>               the type parameter
     * @param <S>               the type parameter
     * @param sqlRecognizers    the sql recognizer list
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param args              the args
     * @return the t
     * @throws SQLException the sql exception
     */
    public static <T, S extends Statement> T execute(List<SQLRecognizer> sqlRecognizers,
                                                     StatementProxy<S> statementProxy,
                                                     StatementCallback<T, S> statementCallback,
                                                     Object... args) throws SQLException {
        if (!ElasticsearchUtil.isStarted) {//在启动过程中执行的sql,直接执行。类似flyway执行
            // Just work as original statement
            return statementCallback.execute(statementProxy.getTargetStatement(), args);
        }
        //没开启seata事务，没开启数据追踪
        else if ((!Seata.EWELL_SEATA_STATE_IS_ON || (!RootContext.requireGlobalLock() && !StringUtils.equals(BranchType.AT.name(), RootContext.getBranchType()))) && !"true".equals(System.getProperty("dataTrace"))) {
            // Just work as original statement
            return statementCallback.execute(statementProxy.getTargetStatement(), args);
        }

        String dbType = statementProxy.getConnectionProxy().getDbType();
        if (CollectionUtils.isEmpty(sqlRecognizers)) {
            sqlRecognizers = SQLVisitorFactory.get(
                statementProxy.getTargetSQL(),
                dbType);
        }
        Executor<T> executor = null;
        if (CollectionUtils.isEmpty(sqlRecognizers)) {
            if(isSelectSql(statementProxy.getTargetSQL(),dbType)) {
                return statementCallback.execute(statementProxy.getTargetStatement(), args);
            }
            //只开启数据追踪，没开启seata事务，放行执行，但追踪日志输出不支持的sql
            else if("true".equals(System.getProperty("dataTrace")) && !Seata.EWELL_SEATA_STATE_IS_ON ) {
                DataTraceLogUtil.trace("Unsupported SQL: " + statementProxy.getTargetSQL());
                return statementCallback.execute(statementProxy.getTargetStatement(), args);
            } else if( Seata.EWELL_SEATA_STATE_IS_ON ) {
                throw new UnsupportedOperationException("seata 事务 Unsupported SQL: " + statementProxy.getTargetSQL());
//            seata 不支持的sql直接抛出异常，否则执行后不能回滚
//            return statementCallback.execute(statementProxy.getTargetStatement(), args);
            }
        } else {
            if (sqlRecognizers.size() == 1) {
                SQLRecognizer sqlRecognizer = sqlRecognizers.get(0);
                switch (sqlRecognizer.getSQLType()) {
                    case INSERT:
                        executor = EnhancedServiceLoader.load(InsertExecutor.class, dbType,
                            new Class[]{StatementProxy.class, StatementCallback.class, SQLRecognizer.class},
                            new Object[]{statementProxy, statementCallback, sqlRecognizer});
                        break;
                    case UPDATE:
                        executor = new UpdateExecutor<>(statementProxy, statementCallback, sqlRecognizer);
                        break;
                    case DELETE:
                        executor = new DeleteExecutor<>(statementProxy, statementCallback, sqlRecognizer);
                        break;
                    case SELECT_FOR_UPDATE:
                        executor = new SelectForUpdateExecutor<>(statementProxy, statementCallback, sqlRecognizer);
                        break;
                    default:
                        executor = new PlainExecutor<>(statementProxy, statementCallback);
                        break;
                }
            } else {
                executor = new MultiExecutor<>(statementProxy, statementCallback, sqlRecognizers);
            }
        }
        T rs;
        //开启seata分布式事务
        if(Seata.EWELL_SEATA_STATE_IS_ON  && (RootContext.requireGlobalLock() || StringUtils.equals(BranchType.AT.name(), RootContext.getBranchType()))) {
            try {
                rs = executor.execute(args);
            } catch (Throwable ex) {
                if (!(ex instanceof SQLException)) {
                    // Turn other exception into SQLException
                    ex = new SQLException(ex);
                }
                throw (SQLException) ex;
            }

        }
        //数据追踪
        else {
            rs =  doTrake(sqlRecognizers,executor,statementProxy,statementCallback,args);
        }
        return rs;
    }

    protected static SQLUndoLog buildUndoItem(SQLRecognizer sqlRecognizer, TableRecords beforeImage,TableRecords afterImage) {
        SQLType sqlType = sqlRecognizer.getSQLType();
        String tableName = sqlRecognizer.getTableName();
        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(sqlType);
        sqlUndoLog.setTableName(tableName);
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);
        return sqlUndoLog;
    }

    private static <T,S extends Statement> T doTrake(List<SQLRecognizer> sqlRecognizers,Executor<T> executor,StatementProxy<S> statementProxy,StatementCallback<T, S> statementCallback,Object... args) throws SQLException {
        //保存数据前后镜像
        TableRecords beforeImage = null;
        try {
            if(!(executor instanceof SelectForUpdateExecutor)) {
                beforeImage = executor.beforeImage();
            }
        } catch (Throwable e) {

        }
        T result = statementCallback.execute(statementProxy.getTargetStatement(), args);

        try {
            if(!(executor instanceof SelectForUpdateExecutor)) {
                TableRecords afterImage = executor.afterImage(beforeImage);
                SQLUndoLog sQLUndoLog = buildUndoItem(sqlRecognizers.get(0), beforeImage, afterImage);
                //业务数据操作前后插入到es数据库
                String xid = String.valueOf(SeataXidWorker.xidWorker.getId());
                BranchUndoLog branchUndoLog = new BranchUndoLog();
                branchUndoLog.setXid(xid);
                branchUndoLog.setSqlUndoLogs(new ArrayList<SQLUndoLog>(Arrays.asList(sQLUndoLog)));
                branchUndoLog.setUserName(ThreadLocalTools.stringThreadLocal.get());
                branchUndoLog.setExecuteDate(DateFormatUtils.format(new java.util.Date(), "yyyy-MM-dd HH:mm:ss"));
                ElasticsearchUtil.addData(branchUndoLog);

                if (statementProxy.getConnection().getAutoCommit()) {// 如果不是事务直接提交
                    ElasticsearchUtil.commitData();
                }
            }
        } catch (Throwable e) {

        }
        return result;
    }

    private static boolean isSelectSql(String sql, String dbType) {
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, dbType);
        if (CollectionUtils.isEmpty(asts)) {
            return true;
        }
        for (SQLStatement ast : asts) {
            if (ast instanceof SQLSelectStatement) {
                return true;
            }
        }
        return false;
    }

}
