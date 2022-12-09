package io.seata.rm.tcc;

import io.seata.commonapi.rm.tcc.api.BusinessActionContext;

import java.util.List;

/**
 * @author leezongjie
 * @date 2022/12/9
 */
public class NormalTccActionImpl implements NormalTccAction {

    @Override
    public boolean prepare(BusinessActionContext actionContext, int a, List b, TccParam tccParam) {
        return false;
    }

    @Override
    public boolean commit(BusinessActionContext actionContext, TccParam param) {
        return false;
    }

    @Override
    public boolean rollback(BusinessActionContext actionContext, TccParam param) {
        return false;
    }
}
