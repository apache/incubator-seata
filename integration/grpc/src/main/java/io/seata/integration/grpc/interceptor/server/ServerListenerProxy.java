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
package io.seata.integration.grpc.interceptor.server;

import java.util.Map;
import java.util.Objects;

import io.grpc.ServerCall;
import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author eddyxu1213@126.com
 */
public class ServerListenerProxy<ReqT> extends ServerCall.Listener<ReqT> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerListenerProxy.class);

    private ServerCall.Listener<ReqT> target;
    private final String xid;

    private final Map<String, String> context;

    public ServerListenerProxy(String xid, Map<String, String> context, ServerCall.Listener<ReqT> target) {
        super();
        Objects.requireNonNull(target);
        this.target = target;
        this.xid = xid;
        this.context = context;
    }

    @Override
    public void onMessage(ReqT message) {
        target.onMessage(message);
    }

    @Override
    public void onHalfClose() {
        if (StringUtils.isNotBlank(xid)) {
            RootContext.bind(xid);
            String branchType = context.get(RootContext.KEY_BRANCH_TYPE);
            if (StringUtils.equals(BranchType.TCC.name(), branchType)) {
                RootContext.bindBranchType(BranchType.TCC);
            }
        }
        target.onHalfClose();
    }

    @Override
    public void onCancel() {
        cleanContext();
        target.onCancel();
    }

    @Override
    public void onComplete() {
        cleanContext();
        target.onComplete();
    }

    @Override
    public void onReady() {
        target.onReady();
    }

    private void cleanContext() {
        if (StringUtils.isNotBlank(xid) && RootContext.inGlobalTransaction()) {
            String unbindXid = RootContext.unbind();
            BranchType previousBranchType = RootContext.getBranchType();
            if (BranchType.TCC == previousBranchType) {
                RootContext.unbindBranchType();
            }
            if (!xid.equalsIgnoreCase(unbindXid)) {
                RootContext.bind(unbindXid);
                LOGGER.warn("bind xid [{}] back to RootContext", unbindXid);
                if (BranchType.TCC == previousBranchType) {
                    RootContext.bindBranchType(previousBranchType);
                    LOGGER.warn("bind branchType [{}] back to RootContext", previousBranchType);
                }
            }
        }
    }
}
