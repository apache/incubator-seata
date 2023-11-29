package io.seata.spring.tcc;

import io.seata.rm.tcc.api.BusinessActionContext;

/**
 * @author leezongjie
 * @date 2023/11/29
 */
public class NormalTccActionImpl implements NormalTccAction {
    @Override
    public boolean prepare(BusinessActionContext actionContext) {
        return true;
    }

    @Override
    public boolean prepareWithException(BusinessActionContext actionContext) {
        throw new IllegalArgumentException();
    }

    @Override
    public boolean commit(BusinessActionContext actionContext) {
        return false;
    }

    @Override
    public boolean rollback(BusinessActionContext actionContext) {
        return false;
    }
}
