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
package io.seata.saga.proctrl;

import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.tm.api.GlobalTransaction;
import java.util.Map;

/**
 * Process Util
 *
 * @author wang.liang
 */
public class ProcessUtil {

    private ProcessUtil() {
    }

    /**
     * Gets xid from saga process context.
     *
     * @return the xid
     */
    public static String getXidFromProcessContext(ProcessContext context) {
        String xid = null;
        Map<String, Object> contextVariable = (Map<String, Object>) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT);
        if (contextVariable != null && contextVariable.containsKey(DomainConstants.VAR_NAME_GLOBAL_TX)) {
            GlobalTransaction globalTransaction = (GlobalTransaction) contextVariable.get(DomainConstants.VAR_NAME_GLOBAL_TX);
            xid = globalTransaction.getXid();
        } else {
            StateMachineInstance smi = (StateMachineInstance) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_INST);
            if (smi != null) {
                xid = smi.getId();
            }
        }
        return xid;
    }

    /**
     * Run in the saga branch.
     *
     * @param context
     * @param runnable
     */
    public static void runInSagaBranch(ProcessContext context, Runnable runnable) {
        // get xidType
        String xidType = RootContext.getXIDInterceptorType();
        boolean isSagaBranch = xidType != null && xidType.endsWith(BranchType.SAGA.name());

        // before run
        String xid = null;
        boolean inGlobalTransaction = false;
        if (!isSagaBranch) {
            // unbind xid
            xid = RootContext.unbind();
            inGlobalTransaction = xid != null;
            if (xid == null) {
                xid = getXidFromProcessContext(context);
            }

            // bind xidType to saga
            RootContext.bindInterceptorType(xid, BranchType.SAGA);
        }

        try {
            runnable.run();
        } finally {
            // after run
            if (!isSagaBranch) {
                if (StringUtils.isNotBlank(xidType)) {
                    // bind xid
                    if (inGlobalTransaction) {
                        RootContext.bind(xid);
                    }
                    // bind xidType
                    RootContext.bindInterceptorType(xidType);
                } else {
                    // unbind xid
                    RootContext.unbind();
                    // unbind xidType
                    RootContext.unbindInterceptorType();
                }
            }
        }
    }

}