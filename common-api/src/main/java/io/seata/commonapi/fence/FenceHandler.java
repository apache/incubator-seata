package io.seata.commonapi.fence;

import io.seata.common.executor.Callback;

import java.lang.reflect.Method;
import java.util.Date;

/**
 * @author leezongjie
 * @date 2022/12/17
 */
public interface FenceHandler {

    Object prepareFence(String xid, Long branchId, String actionName, Callback<Object> targetCallback);

    boolean commitFence(Method commitMethod, Object targetTCCBean, String xid, Long branchId, Object[] args);

    boolean rollbackFence(Method rollbackMethod, Object targetTCCBean, String xid, Long branchId, Object[] args, String actionName);

    int deleteFenceByDate(Date datetime);

}
