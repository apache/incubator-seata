/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.integration.grpc.interceptor.server;

import java.util.Map;
import java.util.Objects;

import io.grpc.ServerCall;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.BranchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
        cleanContext();
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
        target.onCancel();
    }

    @Override
    public void onComplete() {
        target.onComplete();
    }

    @Override
    public void onReady() {
        target.onReady();
    }

    private void cleanContext() {
        RootContext.unbind();
        BranchType previousBranchType = RootContext.getBranchType();
        if (BranchType.TCC == previousBranchType) {
            RootContext.unbindBranchType();
        }
    }
}
