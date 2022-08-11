/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2021 All Rights Reserved.
 */
package io.seata.rm.filter;

import io.seata.core.exception.TransactionException;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import io.seata.core.protocol.transaction.BranchRollbackRequest;
import io.seata.core.protocol.transaction.BranchRollbackResponse;

/**
 * 用于客户端 RM dispatch 时的过滤器
 *
 * @author guocheng
 * @version : RMFilter.java, v 0.1 2021年06月29日 下午1:44 guocheng Exp $
 */
public interface RMFilter {

    /**
     * 提交时的过滤器
     *
     * @param request 请求参数
     * @param response 相应结果
     * @param chain 调用链，获取下一个执行即可，注意在执行下一个方法时，调用提交的接口
     */
    default void commitFilter(BranchCommitRequest request, BranchCommitResponse response, RMFilterChain chain) throws TransactionException {
        chain.next().commitFilter(request, response, chain);
    }

    /**
     * 回滚时过滤器
     *
     * @param request 请求参数
     * @param response 相应结果
     * @param chain 调用链，获取下一个执行即可，注意在执行下一个方法时，调用回滚的接口
     */
    default void rollbackFilter(BranchRollbackRequest request, BranchRollbackResponse response, RMFilterChain chain)
            throws TransactionException {
        chain.next().rollbackFilter(request, response, chain);
    }

}
