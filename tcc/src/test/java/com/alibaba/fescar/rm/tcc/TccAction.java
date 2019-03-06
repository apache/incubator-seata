package com.alibaba.fescar.rm.tcc;

import com.alibaba.fescar.rm.tcc.api.BusinessActionContext;
import com.alibaba.fescar.rm.tcc.api.BusinessActionContextParameter;
import com.alibaba.fescar.rm.tcc.api.LocalTCC;
import com.alibaba.fescar.rm.tcc.api.TwoPhaseBusinessAction;

import java.util.List;

/**
 * @author zhangsen
 */
@LocalTCC
public interface TccAction {

    @TwoPhaseBusinessAction(name = "tccActionForTest" , commitMethod = "commit", rollbackMethod = "rollback")
    public boolean prepare(BusinessActionContext actionContext,
                           @BusinessActionContextParameter(paramName = "a") int a,
                           @BusinessActionContextParameter(paramName = "b", index = 0) List b,
                           @BusinessActionContextParameter(isParamInProperty = true) TccParam tccParam);


    public boolean commit(BusinessActionContext actionContext);


    public boolean rollback(BusinessActionContext actionContext);
}
