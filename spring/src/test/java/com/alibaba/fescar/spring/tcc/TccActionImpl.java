package com.alibaba.fescar.spring.tcc;

import com.alibaba.fescar.rm.tcc.api.BusinessActionContext;

/**
 * @author zhangsen
 */
public class TccActionImpl implements TccAction {

    @Override
    public boolean prepare(BusinessActionContext actionContext, int i) {
        return true;
    }

    @Override
    public boolean commit(BusinessActionContext actionContext) {
        return true;
    }

    @Override
    public boolean rollback(BusinessActionContext actionContext) {
        return true;
    }
}
