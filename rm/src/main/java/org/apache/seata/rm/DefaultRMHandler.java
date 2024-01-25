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
package org.apache.seata.rm;

import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.ResourceManager;
import org.apache.seata.core.protocol.transaction.BranchCommitRequest;
import org.apache.seata.core.protocol.transaction.BranchCommitResponse;
import org.apache.seata.core.protocol.transaction.BranchRollbackRequest;
import org.apache.seata.core.protocol.transaction.BranchRollbackResponse;
import org.apache.seata.core.protocol.transaction.UndoLogDeleteRequest;
import org.slf4j.MDC;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * the default RM event handler implement, deal with the phase two events
 *
 */
public class DefaultRMHandler extends AbstractRMHandler {

    protected static Map<BranchType, AbstractRMHandler> allRMHandlersMap = new ConcurrentHashMap<>();

    protected DefaultRMHandler() {
        initRMHandlers();
    }

    protected void initRMHandlers() {
        List<AbstractRMHandler> allRMHandlers = EnhancedServiceLoader.loadAll(AbstractRMHandler.class);
        if (CollectionUtils.isNotEmpty(allRMHandlers)) {
            for (AbstractRMHandler rmHandler : allRMHandlers) {
                allRMHandlersMap.put(rmHandler.getBranchType(), rmHandler);
            }
        }
    }

    @Override
    public BranchCommitResponse handle(BranchCommitRequest request) {
        MDC.put(RootContext.MDC_KEY_XID, request.getXid());
        MDC.put(RootContext.MDC_KEY_BRANCH_ID, String.valueOf(request.getBranchId()));
        return getRMHandler(request.getBranchType()).handle(request);
    }

    @Override
    public BranchRollbackResponse handle(BranchRollbackRequest request) {
        MDC.put(RootContext.MDC_KEY_XID, request.getXid());
        MDC.put(RootContext.MDC_KEY_BRANCH_ID, String.valueOf(request.getBranchId()));
        return getRMHandler(request.getBranchType()).handle(request);
    }

    @Override
    public void handle(UndoLogDeleteRequest request) {
        getRMHandler(request.getBranchType()).handle(request);
    }

    protected AbstractRMHandler getRMHandler(BranchType branchType) {
        return allRMHandlersMap.get(branchType);
    }

    @Override
    protected ResourceManager getResourceManager() {
        throw new FrameworkException("DefaultRMHandler isn't a real AbstractRMHandler");
    }

    private static class SingletonHolder {
        private static AbstractRMHandler INSTANCE = new DefaultRMHandler();
    }

    /**
     * Get resource manager.
     *
     * @return the resource manager
     */
    public static AbstractRMHandler get() {
        return DefaultRMHandler.SingletonHolder.INSTANCE;
    }

    @Override
    public BranchType getBranchType() {
        throw new FrameworkException("DefaultRMHandler isn't a real AbstractRMHandler");
    }
}
