package io.seata.core.rpc.netty;


import io.seata.rm.tcc.api.BusinessActionContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;


@Service
public class Action1Impl implements Action1 {

    @Override
    public String insert(Long reqId,Map<String, String> params) {
        System.out.println("prepare");
        return "prepare";
    }


    @Override
    public boolean commitTcc(BusinessActionContext actionContext) {
        System.out.println("commitTcc");
        return true;
    }

    @Override
    public boolean cancel(BusinessActionContext actionContext) {
        System.out.println("cancel");
        return true;
    }


}
