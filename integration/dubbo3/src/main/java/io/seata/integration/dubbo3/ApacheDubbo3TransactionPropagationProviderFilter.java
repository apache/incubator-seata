package io.seata.integration.dubbo3;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.BaseFilter;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import io.seata.common.util.StringUtils;
import io.seata.core.constants.DubboConstants;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Transaction propagation provider filter.
 *
 * @author albumenk
 */
@Activate(group = DubboConstants.PROVIDER)
public class ApacheDubbo3TransactionPropagationProviderFilter implements Filter, BaseFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApacheDubbo3TransactionPropagationProviderFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String rpcXid = invocation.getAttachment(RootContext.KEY_XID);
        if (rpcXid == null) {
            rpcXid = invocation.getAttachment(RootContext.KEY_XID.toLowerCase());
        }
        String rpcBranchType = invocation.getAttachment(RootContext.KEY_BRANCH_TYPE);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Server side xid in RpcContext[{}]", rpcXid);
        }
        boolean bind = false;

        if (rpcXid != null) {
            RootContext.bind(rpcXid);
            if (StringUtils.equals(BranchType.TCC.name(), rpcBranchType)) {
                RootContext.bindBranchType(BranchType.TCC);
            }
            bind = true;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("bind xid [{}] branchType [{}] to RootContext", rpcXid, rpcBranchType);
            }
        }
        try {
            return invoker.invoke(invocation);
        } finally {
            if (bind) {
                BranchType previousBranchType = RootContext.getBranchType();
                String unbindXid = RootContext.unbind();
                if (BranchType.TCC == previousBranchType) {
                    RootContext.unbindBranchType();
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("unbind xid [{}] branchType [{}] from RootContext", unbindXid, previousBranchType);
                }
                if (!rpcXid.equalsIgnoreCase(unbindXid)) {
                    LOGGER.warn("xid in change during RPC from {} to {},branchType from {} to {}", rpcXid, unbindXid,
                            rpcBranchType != null ? rpcBranchType : "AT", previousBranchType);
                    if (unbindXid != null) {
                        RootContext.bind(unbindXid);
                        LOGGER.warn("bind xid [{}] back to RootContext", unbindXid);
                        if (BranchType.TCC == previousBranchType) {
                            RootContext.bindBranchType(BranchType.TCC);
                            LOGGER.warn("bind branchType [{}] back to RootContext", previousBranchType);
                        }
                    }
                }
            }
        }
    }
}
