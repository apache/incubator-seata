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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 一个工具类 控制RM二阶段调用的filter的。类似servlet filter
 *
 * @author guocheng
 * @version : RMFilterManager.java, v 0.1 2021年06月29日 下午1:44 guocheng Exp $
 */
public class RMFilterManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RMFilterManager.class);

    private static final LinkedList<RMFilter> FILTERS = new LinkedList<>();

    private RMFilterManager() {}

    /**
     * 从调用链最开始加入一个过滤器，除非涉及通信链路的，否则不建议从这里加入
     *
     * @param filter
     */
    public static void addFirstFilter(RMFilter filter) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("add first seata rm two phrase filter : {}", filter.getClass());
        }
        FILTERS.addFirst(filter);
    }

    /**
     * 从调用链最后加入一个过滤器，正常过滤器请从这里插入
     *
     * @param filter
     */
    public static void addLastFilter(RMFilter filter) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("add last seata rm two phrase filter : {}", filter.getClass());
        }
        FILTERS.addLast(filter);
    }

    /**
     * 移除一个过滤器，这个方法请不要使用，是拿来用于一些调试场景的，容易触发一些比较麻烦的事情
     *
     * @param filter
     */
    public static void removeFilter(RMFilter filter) {
        LOGGER.warn("remove seata rm two phrase filter : {}", filter.getClass());
        FILTERS.remove(filter);
    }

    /**
     * 查看当前的filters情况，这里不包含dispatch的filter，dispatcher会在执行时被加入到chain里，而不由manager管控
     *
     * @return
     */
    public static List<RMFilter> watchFilters() {
        LOGGER.warn("some one look up filters");
        return Collections.unmodifiableList(FILTERS);
    }

    /**
     * 消费commit filter
     *
     * @param request  请求
     * @param response 相应
     * @param lastOne  最后一步dispatch执行内容
     * @throws TransactionException
     */
    public static void dispatchCommit(BranchCommitRequest request, BranchCommitResponse response, RMFilter lastOne)
            throws TransactionException {
        RMFilterChain chain = new RMFilterChain(FILTERS, lastOne);
        chain.next().commitFilter(request, response, chain);
    }

    /**
     * 消费rollback filter
     *
     * @param request  请求
     * @param response 相应
     * @param lastOne  最后一步dispatch执行内容
     * @throws TransactionException
     */
    public static void dispatchRollback(BranchRollbackRequest request, BranchRollbackResponse response, RMFilter lastOne)
            throws TransactionException {
        RMFilterChain chain = new RMFilterChain(FILTERS, lastOne);
        chain.next().rollbackFilter(request, response, chain);
    }

}
