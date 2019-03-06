package com.alibaba.fescar.rm.tcc;

import com.alibaba.fescar.rm.tcc.api.BusinessActionContext;

import java.util.List;

/**
 * @author zhangsen
 */
public class TccActionImpl implements TccAction {

    @Override
    public boolean prepare(BusinessActionContext actionContext,
                           int a,
                           List b,
                           TccParam TccParam  ) {
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
