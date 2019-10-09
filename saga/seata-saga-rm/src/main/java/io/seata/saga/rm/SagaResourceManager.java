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
package io.seata.saga.rm;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.Resource;
import io.seata.rm.AbstractResourceManager;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.exception.ForwardInvalidException;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateMachineInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Saga resource manager
 *
 * @author lorne.cl
 */
public class SagaResourceManager extends AbstractResourceManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(SagaResourceManager.class);

	/**
	 * Saga resource cache
	 */
	private Map<String, Resource> sagaResourceCache = new ConcurrentHashMap<String, Resource>();

    /**
     * Instantiates a new saga resource manager.
     */
    public SagaResourceManager(){
	}

	/**
	 * registry saga resource
	 * @param resource The resource to be managed.
	 */
	@Override
	public void registerResource(Resource resource) {
		SagaResource sagaResource = (SagaResource) resource;
		sagaResourceCache.put(sagaResource.getResourceId(), sagaResource);
		super.registerResource(sagaResource);
	}

	@Override
	public Map<String, Resource> getManagedResources() {
		return sagaResourceCache;
	}

	/**
	 * SAGA branch commit
	 * @param branchType
	 * @param xid             Transaction id.
	 * @param branchId        Branch id.
	 * @param resourceId      Resource id.
	 * @param applicationData Application data bind with this branch.
	 * @return
	 * @throws TransactionException
	 */
	@Override
	public BranchStatus branchCommit(BranchType branchType, String xid, long branchId, String resourceId, String applicationData) throws TransactionException {
		try {
			StateMachineInstance machineInstance = StateMachineEngineHolder.getStateMachineEngine().forward(xid, null);

			if(ExecutionStatus.SU.equals(machineInstance.getStatus()) && machineInstance.getCompensationStatus() == null){
				return BranchStatus.PhaseTwo_Committed;
			}
			else if(ExecutionStatus.SU.equals(machineInstance.getCompensationStatus())){
				return BranchStatus.PhaseTwo_Rollbacked;
			}
			else if(ExecutionStatus.FA.equals(machineInstance.getCompensationStatus())
					|| ExecutionStatus.UN.equals(machineInstance.getCompensationStatus())){
				return BranchStatus.PhaseTwo_RollbackFailed_Retryable;
			}
			else if(ExecutionStatus.FA.equals(machineInstance.getStatus()) && machineInstance.getCompensationStatus() == null){
				return BranchStatus.PhaseOne_Failed;
			}

		} catch (ForwardInvalidException e) {
			LOGGER.error("StateMachine forward failed, xid: " + xid, e);

			//if StateMachineInstanceNotExists stop retry
			if(FrameworkErrorCode.StateMachineInstanceNotExists.equals(e.getErrcode())){
				return BranchStatus.PhaseTwo_Committed;
			}
		}
		return BranchStatus.PhaseTwo_CommitFailed_Retryable;
	}

	/**
	 * SAGA branch rollback
	 * @param branchType the branch type
	 * @param xid             Transaction id.
	 * @param branchId        Branch id.
	 * @param resourceId      Resource id.
	 * @param applicationData Application data bind with this branch.
	 * @return
	 * @throws TransactionException
	 */
	@Override
	public BranchStatus branchRollback(BranchType branchType, String xid, long branchId, String resourceId, String applicationData) throws TransactionException {
		try {
			StateMachineInstance stateMachineInstance = StateMachineEngineHolder.getStateMachineEngine().compensate(xid, null);
			if(ExecutionStatus.SU.equals(stateMachineInstance.getCompensationStatus())){
                return BranchStatus.PhaseTwo_Rollbacked;
            }
		} catch (EngineExecutionException e) {
			LOGGER.error("StateMachine compensate failed, xid: " + xid, e);

			//if StateMachineInstanceNotExists stop retry
			if(FrameworkErrorCode.StateMachineInstanceNotExists.equals(e.getErrcode())){
				return BranchStatus.PhaseTwo_Rollbacked;
			}
		}
		return BranchStatus.PhaseTwo_RollbackFailed_Retryable;
	}

	@Override
	public BranchType getBranchType(){
		return BranchType.SAGA;
	}
}
