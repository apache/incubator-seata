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
package org.apache.seata.integration.rpc.core;

import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.BranchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public final class TransactionPropagationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionPropagationHandler.class);

    private TransactionPropagationHandler() {}

    public static Map<String, String> getTransactionPropagationContext() {
        Map<String, String> context = new HashMap<>();
        String xid = RootContext.getXID();
        if (xid != null) {
            context.put(RootContext.KEY_XID, xid);
            BranchType branchType = RootContext.getBranchType();
            if (branchType != null) {
                context.put(RootContext.KEY_BRANCH_TYPE, branchType.name());
            }
        }
        return context;
    }

    public static boolean bindProviderContext(String rpcXid, String rpcBranchType) {
        if (rpcXid == null) {
            return false;
        }
        RootContext.bind(rpcXid);
        if (BranchType.TCC.name().equalsIgnoreCase(rpcBranchType)) {
            RootContext.bindBranchType(BranchType.TCC);
        }
        return true;
    }

    public static void unbindProviderContext(String rpcXid) {
        BranchType previousBranchType = RootContext.getBranchType();
        String unbindXid = RootContext.unbind();
        if (BranchType.TCC == previousBranchType) {
            RootContext.unbindBranchType();
        }
        if (rpcXid != null && !rpcXid.equalsIgnoreCase(unbindXid)) {
            LOGGER.warn("xid changed during RPC from {} to {}", rpcXid, unbindXid);
            if (unbindXid != null) {
                RootContext.bind(unbindXid);
                if (BranchType.TCC == previousBranchType) {
                    RootContext.bindBranchType(BranchType.TCC);
                }
            }
        }
    }

    public static String resolveXid(String xid, String xidLowercase) {
        return xid != null ? xid : xidLowercase;
    }
}
