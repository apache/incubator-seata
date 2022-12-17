package io.seata.commonapi.fence;

import io.seata.common.executor.Callback;

import java.lang.reflect.Method;
import java.util.Date;

/**
 * @author leezongjie
 * @date 2022/12/17
 */
public class DefaultCommonFenceHandler implements FenceHandler {

    private FenceHandler fenceHandler;

    private static class SingletonHolder {
        private static final DefaultCommonFenceHandler INSTANCE = new DefaultCommonFenceHandler();
    }

    public static DefaultCommonFenceHandler get() {
        return DefaultCommonFenceHandler.SingletonHolder.INSTANCE;
    }

    public void setFenceHandler(FenceHandler fenceHandler) {
        this.fenceHandler = fenceHandler;
    }

    private void check() {
        if (fenceHandler == null) {
            throw new RuntimeException("fenceHandler is null, need to set a fenceHandler implement");
        }
    }

    @Override
    public Object prepareFence(String xid, Long branchId, String actionName, Callback<Object> targetCallback) {
        check();
        return fenceHandler.prepareFence(xid, branchId, actionName, targetCallback);
    }

    @Override
    public boolean commitFence(Method commitMethod, Object targetTCCBean, String xid, Long branchId, Object[] args) {
        check();
        return fenceHandler.commitFence(commitMethod, targetTCCBean, xid, branchId, args);
    }

    @Override
    public boolean rollbackFence(Method rollbackMethod, Object targetTCCBean, String xid, Long branchId, Object[] args, String actionName) {
        check();
        return fenceHandler.rollbackFence(rollbackMethod, targetTCCBean, xid, branchId, args, actionName);
    }

    @Override
    public int deleteFenceByDate(Date datetime) {
        check();
        return fenceHandler.deleteFenceByDate(datetime);
    }
}
