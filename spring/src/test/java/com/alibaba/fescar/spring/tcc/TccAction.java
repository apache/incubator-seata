package com.alibaba.fescar.spring.tcc;

import com.alibaba.fescar.rm.tcc.api.BusinessActionContext;
import com.alibaba.fescar.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * @author zhangsen
 */
public interface TccAction {

    @TwoPhaseBusinessAction(name = "tccActionForSpringTest" , commitMethod = "commit", rollbackMethod = "rollback")
    public boolean prepare(BusinessActionContext actionContext, int i);


    public boolean commit(BusinessActionContext actionContext);


    public boolean rollback(BusinessActionContext actionContext);
}
