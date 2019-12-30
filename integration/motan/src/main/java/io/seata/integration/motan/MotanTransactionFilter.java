/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.integration.motan;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.core.extension.Activation;
import com.weibo.api.motan.core.extension.Scope;
import com.weibo.api.motan.core.extension.Spi;
import com.weibo.api.motan.filter.Filter;
import com.weibo.api.motan.rpc.Caller;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import io.seata.core.context.RootContext;
import io.seata.tm.api.GlobalTransactionRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author slievrly
 */
@Spi(scope = Scope.SINGLETON)
@Activation(key = {MotanConstants.NODE_TYPE_SERVICE, MotanConstants.NODE_TYPE_REFERER}, sequence = 100)
public class MotanTransactionFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MotanTransactionFilter.class);

    public MotanTransactionFilter(){}
    @Override
    public Response filter(final Caller<?> caller, final Request request) {
        String currentXid = RootContext.getXID();
        String currentXidRole = RootContext.getXIDRole();
        String requestXid = getRpcXid(request);
        String requestXidRole = request.getAttachments().get(RootContext.KEY_XID);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("xid in RootContext [" + currentXid + "] xid in Request [" + requestXid + "]");
        }
        boolean bind = false;
        if (currentXid != null) {
            if (currentXid != null) {
                request.getAttachments().put(RootContext.KEY_XID, currentXid);
                request.getAttachments().put(RootContext.KEY_XID_ROLE, GlobalTransactionRole.Participant.getName());
            } else {
                request.getAttachments().put(RootContext.KEY_XID_ROLE, currentXidRole);
            }
        } else
            if (null != requestXidRole) {
                RootContext.bindXIDRole(requestXidRole);
                bind = true;
                if (null != requestXid) {
                    RootContext.bind(requestXid);
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("bind [" + requestXid + "] to RootContext");
                }
            }
        try {
            return caller.call(request);
        } finally {
            if (bind) {
                String unbindXidRole = RootContext.unbindXIDRole();
                if (null != requestXid) {
                    String unbindXid = RootContext.unbind();
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("unbind [" + unbindXid + "] from RootContext");
                    }
                    if (!requestXid.equalsIgnoreCase(unbindXid)) {
                        LOGGER.warn("xid has changed, during RPC from " + requestXid + " to " + unbindXid);
                        if (unbindXid != null) {
                            RootContext.bind(unbindXid);
                            RootContext.bindXIDRole(unbindXidRole);
                            LOGGER.warn("bind [" + unbindXid + "] back to RootContext");
                        }
                    }
                }
            }
        }
    }

    /**
     * get rpc xid
     * @param request
     * @return
     */
    private String getRpcXid(Request request) {
        String rpcXid = request.getAttachments().get(RootContext.KEY_XID);
        if (rpcXid == null) {
            rpcXid = request.getAttachments().get(RootContext.KEY_XID.toLowerCase());
        }
        return rpcXid;
    }

}
