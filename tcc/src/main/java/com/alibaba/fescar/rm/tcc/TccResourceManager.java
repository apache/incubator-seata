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
package com.alibaba.fescar.rm.tcc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fescar.common.Constants;
import com.alibaba.fescar.common.XID;
import com.alibaba.fescar.common.exception.FrameworkException;
import com.alibaba.fescar.common.exception.NotSupportYetException;
import com.alibaba.fescar.common.exception.ShouldNeverHappenException;
import com.alibaba.fescar.common.util.StringUtils;
import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.exception.TransactionExceptionCode;
import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.core.model.Resource;
import com.alibaba.fescar.core.protocol.ResultCode;
import com.alibaba.fescar.core.protocol.transaction.BranchRegisterRequest;
import com.alibaba.fescar.core.protocol.transaction.BranchRegisterResponse;
import com.alibaba.fescar.core.protocol.transaction.BranchReportRequest;
import com.alibaba.fescar.core.protocol.transaction.BranchReportResponse;
import com.alibaba.fescar.core.rpc.netty.RmRpcClient;
import com.alibaba.fescar.rm.AbstractResourceManager;
import com.alibaba.fescar.rm.tcc.api.BusinessActionContext;
import com.alibaba.fescar.rm.tcc.api.BusinessActivityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * TCC resource manager
 *
 * @author zhangsen
 */
public class TccResourceManager extends AbstractResourceManager {

	/**
	 * TCC resource cache
	 */
	private Map<String, Resource> tccResourceCache = new ConcurrentHashMap<String, Resource>();

    /**
     * Instantiates a new Tcc resource manager.
     */
    public TccResourceManager(){
	}

	/**
	 * registry TCC resource
	 * @param resource The resource to be managed.
	 */
	@Override
	public void registerResource(Resource resource) {
		TCCResource tccResource = (TCCResource) resource;
		tccResourceCache.put(tccResource.getResourceId(), tccResource);
		super.registerResource(tccResource);
	}

	@Override
	public Map<String, Resource> getManagedResources() {
		return tccResourceCache;
	}

	/**
	 * TCC branch commit
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
		TCCResource tccResource = (TCCResource) tccResourceCache.get(resourceId);
		if(tccResource == null){
			throw new ShouldNeverHappenException("TCC resource is not exist, resourceId:" + resourceId);
		}
		Object targetTCCBean = tccResource.getTargetBean();
		Method commitMethod = tccResource.getCommitMethod();
		if(targetTCCBean == null || commitMethod == null){
			throw new ShouldNeverHappenException("TCC resource is not available, resourceId:" + resourceId);
		}
		try {
			boolean result = false;
			//BusinessActionContext
			BusinessActionContext businessActionContext = getBusinessActionContext(xid, branchId, resourceId, applicationData);
			Object ret = commitMethod.invoke(targetTCCBean, businessActionContext);
			if(ret != null && ret instanceof TwoPhaseResult){
				result = ((TwoPhaseResult)ret).isSuccess();
			}else {
				result = (boolean) ret;
			}
			return result ? BranchStatus.PhaseTwo_Committed:BranchStatus.PhaseTwo_CommitFailed_Retryable;
		}catch (Throwable t){
			String msg = String.format("commit TCC resource error, resourceId: %s, xid: %s.", resourceId, xid);
			LOGGER.error(msg , t);
			throw new FrameworkException(t, msg);
		}
	}

	/**
	 * TCC branch rollback
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
		TCCResource tccResource = (TCCResource) tccResourceCache.get(resourceId);
		if (tccResource == null) {
			throw new ShouldNeverHappenException("TCC resource is not exist, resourceId:" + resourceId);
		}
		Object targetTCCBean = tccResource.getTargetBean();
		Method rollbackMethod = tccResource.getRollbackMethod();
		if (targetTCCBean == null || rollbackMethod == null) {
			throw new ShouldNeverHappenException("TCC resource is not available, resourceId:" + resourceId);
		}
		try {
			boolean result = false;
			//BusinessActionContext
			BusinessActionContext businessActionContext = getBusinessActionContext(xid, branchId, resourceId, applicationData);
			Object ret = rollbackMethod.invoke(targetTCCBean, businessActionContext);
			if (ret != null && ret instanceof TwoPhaseResult) {
				result = ((TwoPhaseResult) ret).isSuccess();
			} else {
				result = (boolean) ret;
			}
			return result ? BranchStatus.PhaseTwo_Rollbacked : BranchStatus.PhaseTwo_RollbackFailed_Retryable;
		} catch (Throwable t) {
			String msg = String.format("rollback TCC resource error, resourceId: %s, xid: %s.", resourceId, xid);
			LOGGER.error(msg, t);
			throw new FrameworkException(t, msg);
		}
	}

    /**
     * transfer tcc applicationData to BusinessActionContext
     *
     * @param xid the xid
     * @param branchId the branch id
     * @param resourceId the resource id
     * @param applicationData the application data
     * @return business action context
     */
    protected BusinessActionContext getBusinessActionContext(String xid, long branchId, String resourceId, String applicationData){
		//transfer tcc applicationData to Context
		Map tccContext = StringUtils.isBlank(applicationData)?new HashMap():(Map) JSON.parse(applicationData);
		Map actionContextMap = (Map) tccContext.get(Constants.TCC_ACTION_CONTEXT);
		BusinessActionContext businessActionContext = new BusinessActionContext(
				xid, String.valueOf(branchId),  actionContextMap);
		businessActionContext.setActionName(resourceId);
		return businessActionContext;
	}

	@Override
	public BranchType getBranchType(){
		return BranchType.TCC;
	}
}
