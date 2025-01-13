package org.apache.seata.spring.tcc;

import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.LocalTCC;
import org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 *
 * @date 2025年01月12日23:19
 */

@LocalTCC
public class EasyTccActionImpl implements EasyTccAction {

    @Override
    @TwoPhaseBusinessAction(name = "easyActionForTest", commitMethod = "commit", rollbackMethod = "rollback")
    public boolean prepare(BusinessActionContext actionContext) {
        return false;
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
