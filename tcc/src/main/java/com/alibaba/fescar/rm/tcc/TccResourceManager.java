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
import com.alibaba.fescar.core.model.ResourceManager;
import com.alibaba.fescar.core.protocol.ResultCode;
import com.alibaba.fescar.core.protocol.transaction.BranchRegisterRequest;
import com.alibaba.fescar.core.protocol.transaction.BranchRegisterResponse;
import com.alibaba.fescar.core.protocol.transaction.BranchReportRequest;
import com.alibaba.fescar.core.protocol.transaction.BranchReportResponse;
import com.alibaba.fescar.core.rpc.netty.RmRpcClient;
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
public class TccResourceManager implements ResourceManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(TccResourceManager.class);

	/**
	 * TCC资源信息
	 */
	private Map<String, Resource> tccResourceCache = new ConcurrentHashMap<String, Resource>();

	public TccResourceManager(){
	}

	/**
	 * 注册 TCC 资源
	 * @param resource The resource to be managed.
	 */
	@Override
	public void registerResource(Resource resource) {
		TCCResource tccResource = (TCCResource) resource;
		tccResourceCache.put(tccResource.getResourceId(), tccResource);
		RmRpcClient.getInstance().registerResource(tccResource.getResourceGroupId(), tccResource.getResourceId());
	}

	@Override
	public void unregisterResource(Resource resource) {
		throw new NotSupportYetException("unregister a resource");
	}

	@Override
	public Map<String, Resource> getManagedResources() {
		return tccResourceCache;
	}

	/**
	 * 二阶段提交
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
			//方法参数
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
	 * 二阶段回滚
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
			//方法参数
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
	 * 生成二阶段方法参数
	 * @param xid
	 * @param branchId
	 * @param resourceId
	 * @param applicationData
	 * @return
	 */
	protected BusinessActionContext getBusinessActionContext(String xid, long branchId, String resourceId, String applicationData){
		//方法参数
		Map tccContext = StringUtils.isBlank(applicationData)?new HashMap():(Map) JSON.parse(applicationData);
		Map activityContextMap = (Map) tccContext.get(Constants.TCC_ACTIVITY_CONTEXT);
		Map actionContextMap = (Map) tccContext.get(Constants.TCC_ACTION_CONTEXT);
		BusinessActionContext businessActionContext = new BusinessActionContext(
				xid, String.valueOf(branchId), new BusinessActivityContext(activityContextMap==null?new HashMap<String, Object>():activityContextMap),
				actionContextMap);
		businessActionContext.setActionName(resourceId);
		return businessActionContext;
	}

	/**
	 * 创建分支事务记录
	 * @param branchType the branch type
	 * @param resourceId the resource id
	 * @param clientId   the client id
	 * @param xid        the xid
	 * @param lockKeys   the lock keys
	 * @return
	 * @throws TransactionException
	 */
	@Override
	public Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid, String applicationData, String lockKeys) throws TransactionException {
		try {
			BranchRegisterRequest request = new BranchRegisterRequest();
			request.setTransactionId(XID.getTransactionId(xid));
			request.setLockKey(lockKeys);
			request.setResourceId(resourceId);
			request.setBranchType(branchType);
			request.setApplicationData(applicationData);

			BranchRegisterResponse response = (BranchRegisterResponse) RmRpcClient.getInstance().sendMsgWithResponse(request);
			if (response.getResultCode() == ResultCode.Failed) {
				throw new TransactionException(response.getTransactionExceptionCode(), "Response[" + response.getMsg() + "]");
			}
			return response.getBranchId();
		} catch (TimeoutException toe) {
			throw new TransactionException(TransactionExceptionCode.IO, "RPC Timeout", toe);
		} catch (RuntimeException rex) {
			throw new TransactionException(TransactionExceptionCode.BranchRegisterFailed, "Runtime", rex);
		}
	}

	/**
	 * 更新分支事务记录状态
	 * @param branchType      the branch type
	 * @param xid             the xid
	 * @param branchId        the branch id
	 * @param status          the status
	 * @param applicationData the application data
	 * @throws TransactionException
	 */
	@Override
	public void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status, String applicationData) throws TransactionException {
		try {
			BranchReportRequest request = new BranchReportRequest();
			request.setTransactionId(XID.getTransactionId(xid));
			request.setBranchId(branchId);
			request.setStatus(status);
			request.setApplicationData(applicationData);

			BranchReportResponse response = (BranchReportResponse) RmRpcClient.getInstance().sendMsgWithResponse(request);
			if (response.getResultCode() == ResultCode.Failed) {
				throw new TransactionException(response.getTransactionExceptionCode(), "Response[" + response.getMsg() + "]");
			}
		} catch (TimeoutException toe) {
			throw new TransactionException(TransactionExceptionCode.IO, "RPC Timeout", toe);
		} catch (RuntimeException rex) {
			throw new TransactionException(TransactionExceptionCode.BranchReportFailed, "Runtime", rex);
		}
	}

	@Override
	public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys) throws TransactionException {
		return false;
	}


	@Override
	public BranchType getBranchType(){
		return BranchType.TCC;
	}
}
