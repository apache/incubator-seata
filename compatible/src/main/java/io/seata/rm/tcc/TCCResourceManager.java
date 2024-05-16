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
package io.seata.rm.tcc;

import org.apache.seata.rm.tcc.api.BusinessActionContext;

/**
 * TCC resource manager
 *
 */
public class TCCResourceManager extends org.apache.seata.rm.tcc.TCCResourceManager {

    @Override
    protected Object[] getTwoPhaseMethodParams(String[] keys, Class<?>[] argsClasses,
        BusinessActionContext businessActionContext) {
        Object[] args = new Object[argsClasses.length];
        for (int i = 0; i < argsClasses.length; i++) {
            if (argsClasses[i].equals(BusinessActionContext.class)) {
                args[i] = businessActionContext;
            } else if (argsClasses[i].equals(io.seata.rm.tcc.api.BusinessActionContext.class)) {
                io.seata.rm.tcc.api.BusinessActionContext oldBusinessActionContext =
                    new io.seata.rm.tcc.api.BusinessActionContext();
                oldBusinessActionContext.setUpdated(businessActionContext.getUpdated());
                oldBusinessActionContext.setXid(businessActionContext.getXid());
                oldBusinessActionContext.setActionContext(businessActionContext.getActionContext());
                oldBusinessActionContext.setActionName(businessActionContext.getActionName());
                oldBusinessActionContext.setBranchId(businessActionContext.getBranchId());
                oldBusinessActionContext.setBranchType(businessActionContext.getBranchType());
                oldBusinessActionContext.setDelayReport(businessActionContext.getDelayReport());
                args[i] = oldBusinessActionContext;
            } else {
                args[i] = businessActionContext.getActionContext(keys[i], argsClasses[i]);
            }
        }
        return args;
    }

}
