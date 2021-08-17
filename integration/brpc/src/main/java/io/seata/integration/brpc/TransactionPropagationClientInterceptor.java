package io.seata.integration.brpc;

import com.baidu.brpc.RpcContext;
import com.baidu.brpc.interceptor.AbstractInterceptor;
import com.baidu.brpc.interceptor.InterceptorChain;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import io.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * load SEATA xid for brpc request
 *
 * @author mxz0828@163.com
 * @date 2021/8/12
 */
public class TransactionPropagationClientInterceptor extends AbstractInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionPropagationServerInterceptor.class);

    @Override
    public boolean handleRequest(Request brpcRequest) {

        if (!RootContext.inGlobalTransaction()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("SEATA-BRPC[{}] is not in globalTransaction, handle other interceptor request", RootContext.getBranchType());
            }
            return Boolean.TRUE;
        }

        String xid = RootContext.getXID();
        String rpcXid = getRpcXid();
        Map<String, Object> kvAttachment = brpcRequest.getKvAttachment();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("SEATA-BRPC[{}]: xid in RootContext[{}] xid in RpcContext[{}]", RootContext.getBranchType(), xid, rpcXid);
        }

        if (Objects.nonNull(xid)) {
            if (Objects.isNull(kvAttachment)) {
                kvAttachment = new HashMap<>();
            }
            kvAttachment.put(RootContext.KEY_XID, xid);
            kvAttachment.put(RootContext.KEY_BRANCH_TYPE, RootContext.getBranchType().name());
            brpcRequest.setKvAttachment(kvAttachment);
        }

        return super.handleRequest(brpcRequest);
    }

    @Override
    public void handleResponse(Response response) {
        super.handleResponse(response);
    }

    @Override
    public void aroundProcess(Request brpcRequest, Response brpcResponse, InterceptorChain chain) throws Exception {
        chain.intercept(brpcRequest, brpcResponse);
    }

    private String getRpcXid() {
        RpcContext context = RpcContext.getContext();
        Map<String, Object> requestKvAttachmentMap = context.getRequestKvAttachment();
        if (Objects.isNull(requestKvAttachmentMap)) {
            return null;
        }
        String rpcXid = (String) requestKvAttachmentMap.get(RootContext.KEY_XID);
        if (Objects.isNull(rpcXid)) {
            rpcXid = (String) requestKvAttachmentMap.get(RootContext.KEY_XID);
        }
        return rpcXid;
    }
}
