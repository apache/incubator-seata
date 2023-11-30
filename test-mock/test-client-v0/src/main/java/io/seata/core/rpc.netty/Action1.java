package io.seata.core.rpc.netty;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

import java.util.Map;

@LocalTCC
public interface Action1 {

    @TwoPhaseBusinessAction(name = "mock-action", commitMethod = "commitTcc", rollbackMethod = "cancel"
//            , useTCCFence = true
    )
    String insert(@BusinessActionContextParameter Long reqId,
            @BusinessActionContextParameter(paramName = "params") Map<String, String> params
    );


    boolean commitTcc(BusinessActionContext actionContext);


    boolean cancel(BusinessActionContext actionContext);
}
