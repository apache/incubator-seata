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
package org.apache.seata.integration.tx.api.fence;

import java.lang.reflect.Method;
import java.util.Date;

import org.apache.seata.common.executor.Callback;


public class DefaultCommonFenceHandler implements FenceHandler {

    private FenceHandler fenceHandler;

    private static class SingletonHolder {
        private static final DefaultCommonFenceHandler INSTANCE = new DefaultCommonFenceHandler();
    }

    public static DefaultCommonFenceHandler get() {
        return DefaultCommonFenceHandler.SingletonHolder.INSTANCE;
    }

    public void setFenceHandler(FenceHandler fenceHandler) {
        this.fenceHandler = fenceHandler;
    }

    private void check() {
        if (fenceHandler == null) {
            throw new RuntimeException("fenceHandler is null, need to set a fenceHandler implement");
        }
    }

    @Override
    public Object prepareFence(String xid, Long branchId, String actionName, Callback<Object> targetCallback) {
        check();
        return fenceHandler.prepareFence(xid, branchId, actionName, targetCallback);
    }

    @Override
    public boolean commitFence(Method commitMethod, Object targetTCCBean, String xid, Long branchId, Object[] args) {
        check();
        return fenceHandler.commitFence(commitMethod, targetTCCBean, xid, branchId, args);
    }

    @Override
    public boolean rollbackFence(Method rollbackMethod, Object targetTCCBean, String xid, Long branchId, Object[] args, String actionName) {
        check();
        return fenceHandler.rollbackFence(rollbackMethod, targetTCCBean, xid, branchId, args, actionName);
    }

    @Override
    public int deleteFenceByDate(Date datetime) {
        check();
        return fenceHandler.deleteFenceByDate(datetime);
    }
}
