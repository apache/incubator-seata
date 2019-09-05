/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.saga.engine.store.db;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Transactional SqlSession Executor
 *
 * @author lorne.cl
 */
public class TransactionalSqlSessionExecutor implements SqlSessionExecutor {

    private TransactionTemplate transactionTemplate;
    private SqlSessionTemplate  sqlSessionTemplate;

    @Override
    public <T> T selectOne(String statement, Object parameter) {
        return sqlSessionTemplate.selectOne(statement, parameter);
    }

    @Override
    public <E> List<E> selectList(String statement, Object parameter) {
        return sqlSessionTemplate.selectList(statement, parameter);
    }

    @Override
    public int insert(String statement, Object parameter) {
        return transactionTemplate.execute(new TransactionCallback<Integer>() {
            @Override
            public Integer doInTransaction(TransactionStatus status) {
                return sqlSessionTemplate.insert(statement, parameter);
            }
        });
    }

    @Override
    public int update(String statement, Object parameter) {
        return transactionTemplate.execute(new TransactionCallback<Integer>() {
            @Override
            public Integer doInTransaction(TransactionStatus status) {
                return sqlSessionTemplate.update(statement, parameter);
            }
        });
    }

    @Override
    public int delete(String statement, Object parameter) {
        return transactionTemplate.execute(new TransactionCallback<Integer>() {
            @Override
            public Integer doInTransaction(TransactionStatus status) {
                return sqlSessionTemplate.delete(statement, parameter);
            }
        });
    }

    public TransactionTemplate getTransactionTemplate() {
        return transactionTemplate;
    }

    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public SqlSessionTemplate getSqlSessionTemplate() {
        return sqlSessionTemplate;
    }

    public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }
}