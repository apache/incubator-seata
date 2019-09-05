/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.saga.engine.store.db;

import java.util.List;

/**
 * SqlSession Executor
 *
 * @author lorne.cl
 */
public interface SqlSessionExecutor {

    <T> T selectOne(String statement, Object parameter);

    <E> List<E> selectList(String statement, Object parameter);

    int insert(String statement, Object parameter);

    int update(String statement, Object parameter);

    int delete(String statement, Object parameter);
}