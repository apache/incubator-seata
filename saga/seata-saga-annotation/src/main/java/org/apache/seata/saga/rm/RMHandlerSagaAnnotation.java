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
package org.apache.seata.saga.rm;


import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.ResourceManager;
import org.apache.seata.core.protocol.transaction.BranchCommitRequest;
import org.apache.seata.core.protocol.transaction.BranchCommitResponse;
import org.apache.seata.core.protocol.transaction.UndoLogDeleteRequest;
import org.apache.seata.rm.AbstractRMHandler;
import org.apache.seata.rm.DefaultResourceManager;

/**
 * The type Rm handler SAGA.
 *
 * @author leezongjie
 */
public class RMHandlerSagaAnnotation extends AbstractRMHandler {

    @Override
    public void handle(UndoLogDeleteRequest request) {
        //DO nothing
    }

    @Override
    protected void doBranchCommit(BranchCommitRequest request, BranchCommitResponse response) throws TransactionException {
        //do nothing
    }

    /**
     * get SAGA resource manager
     *
     * @return the resource manager
     */
    @Override
    protected ResourceManager getResourceManager() {
        return DefaultResourceManager.get().getResourceManager(BranchType.SAGA_ANNOTATION);
    }

    @Override
    public BranchType getBranchType() {
        return BranchType.SAGA_ANNOTATION;
    }

}
