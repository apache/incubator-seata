package io.seata.saga.proctrl;

import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.tm.api.GlobalTransaction;

import java.util.Map;

public class ProcessUtil {

	private ProcessUtil() {
	}

	/**
	 * Gets xid from saga process context.
	 *
	 * @return the xid
	 */
	public static String getXIDFromProcessContext(ProcessContext context) {
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
		String xid = RootContext.unbind();
		String xidType = RootContext.getXIDInterceptorType();
		boolean isSagaBranch = xidType != null && xidType.endsWith(BranchType.SAGA.name());
		boolean inGlobalTransaction = (xid != null);
		if (xid == null) {
			xid = getXIDFromProcessContext(context);
		}
		if (!isSagaBranch) {
			RootContext.bindInterceptorType(xid, BranchType.SAGA);
		}

		try {
			runnable.run();
		} finally {
			if (inGlobalTransaction) {
				RootContext.bind(xid);
			}
			if (!isSagaBranch) {
				if (StringUtils.isNotBlank(xidType)) {
					RootContext.bindInterceptorType(xidType);
				} else {
					RootContext.unbindInterceptorType();
				}
			}
		}
	}

}