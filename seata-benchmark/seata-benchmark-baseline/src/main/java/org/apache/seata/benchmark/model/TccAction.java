package org.apache.seata.benchmark.model;

import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction;

public interface TccAction {

    @TwoPhaseBusinessAction(name = "tccAction")
    boolean prepare(BusinessActionContext actionContext);

    boolean commit(BusinessActionContext actionContext);


    boolean rollback(BusinessActionContext actionContext);
}