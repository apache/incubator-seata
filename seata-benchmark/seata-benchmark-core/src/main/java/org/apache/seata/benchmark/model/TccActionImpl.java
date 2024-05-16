package org.apache.seata.benchmark.model;

import org.apache.seata.rm.tcc.api.BusinessActionContext;

public class TccActionImpl implements TccAction{

    @Override
    public boolean prepare(BusinessActionContext actionContext) {
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