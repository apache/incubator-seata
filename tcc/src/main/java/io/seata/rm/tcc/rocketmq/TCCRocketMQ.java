package io.seata.rm.tcc.rocketmq;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

import java.util.Map;

@LocalTCC
public interface TCCRocketMQ {
    @TwoPhaseBusinessAction(name = "tccRocketMQ", commitMethod = "commit", rollbackMethod = "rollback")
    boolean prepare(BusinessActionContext context, Map<String, Object> params);

    boolean commit(BusinessActionContext context);

    boolean rollback(BusinessActionContext context);
}
