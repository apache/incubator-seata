/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2021 All Rights Reserved.
 */
package io.seata.rm.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author guocheng
 * @version : RMFilterChain.java, v 0.1 2021年06月29日 下午3:12 guocheng Exp $
 */
public class RMFilterChain {

    private static final Logger LOGGER = LoggerFactory.getLogger(RMFilterChain.class);

    protected RMFilterChain(List<RMFilter> filters, RMFilter lastOne) {
        this.filters = filters;
        this.lastOne = lastOne;
    }

    private final RMFilter lastOne;

    private final List<RMFilter> filters;

    private int index = 0;

    /**
     * 获取下一个filter
     *
     * @return
     */
    public RMFilter next() {
        RMFilter filter;
        if (filters.size() > index) {
            filter = filters.get(index);
        } else {
            filter = lastOne;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("load dispatch filter {} with index {} ", filter.getClass(), index);
        }
        index++;
        return filter;
    }

}
