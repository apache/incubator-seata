package io.seata.integration.brpc.server.impl;

import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.integration.brpc.dto.EchoRequest;
import io.seata.integration.brpc.dto.EchoResponse;
import io.seata.integration.brpc.server.EchoService;

import java.util.Objects;

/**
 * @author mxz0828@163.com
 * @date 2021/8/16
 */
public class EchoServiceImpl implements EchoService {
    @Override
    public EchoResponse echo(EchoRequest request) {
        EchoResponse response = new EchoResponse();
        response.setReqMsg(request.getReqMsg());
        response.setXid(RootContext.getXID());
        BranchType branchType = RootContext.getBranchType();
        if (Objects.nonNull(branchType)) {
            response.setBranchType(branchType.name());
        }
        return response;
    }

}
