/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.rm;

import com.alibaba.fescar.common.exception.FrameworkException;
import com.alibaba.fescar.common.exception.ShouldNeverHappenException;
import com.alibaba.fescar.common.loader.EnhancedServiceLoader;
import com.alibaba.fescar.common.util.CollectionUtils;
import com.alibaba.fescar.core.exception.AbstractExceptionHandler;
import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.core.model.ResourceManager;
import com.alibaba.fescar.core.protocol.transaction.BranchCommitRequest;
import com.alibaba.fescar.core.protocol.transaction.BranchCommitResponse;
import com.alibaba.fescar.core.protocol.transaction.BranchRollbackRequest;
import com.alibaba.fescar.core.protocol.transaction.BranchRollbackResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * the default RM event handler implement, deal with the phase two events
 *
 * @author zhangsen
 */
public class DefaultRMHandler extends AbstractRMHandler {

    protected static Map<BranchType, AbstractRMHandler> allRMHandlersMap = new ConcurrentHashMap<BranchType, AbstractRMHandler>();

    protected DefaultRMHandler(){
        initRMHandlers();
    }

    protected void initRMHandlers(){
        List<AbstractRMHandler> allRMHandlers = EnhancedServiceLoader.loadAll(AbstractRMHandler.class);
        if(CollectionUtils.isNotEmpty(allRMHandlers)){
            for(AbstractRMHandler rmHandler : allRMHandlers){
                allRMHandlersMap.put(rmHandler.getBranchType(), rmHandler);
            }
        }
    }

    @Override
    public BranchCommitResponse handle(BranchCommitRequest request) {
        return getRMHandler(request.getBranchType()).handle(request);
    }

    @Override
    public BranchRollbackResponse handle(BranchRollbackRequest request) {
        return getRMHandler(request.getBranchType()).handle(request);
    }

    protected AbstractRMHandler getRMHandler(BranchType branchType){
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
