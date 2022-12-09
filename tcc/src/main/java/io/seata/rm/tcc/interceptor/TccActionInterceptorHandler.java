package io.seata.rm.tcc.interceptor;

import io.seata.common.Constants;
import io.seata.commonapi.interceptor.ActionInterceptorHandler;
import io.seata.commonapi.interceptor.InvocationWrapper;
import io.seata.commonapi.interceptor.TwoPhaseBusinessActionParam;
import io.seata.commonapi.interceptor.handler.AbstractProxyInvocationHandler;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.slf4j.MDC;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static io.seata.common.DefaultValues.DEFAULT_DISABLE_GLOBAL_TRANSACTION;

/**
 * @author leezongjie
 * @date 2022/11/26
 */
public class TccActionInterceptorHandler extends AbstractProxyInvocationHandler {

    private volatile boolean disable = ConfigurationFactory.getInstance().getBoolean(
            ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION, DEFAULT_DISABLE_GLOBAL_TRANSACTION);

    private ActionInterceptorHandler actionInterceptorHandler = new ActionInterceptorHandler();

    private Class[] interfaceToProxy;
    private Set<String> methodsToProxy;

    public TccActionInterceptorHandler(Class[] interfaceToProxy, Set<String> methodsToProxy) {
        this.interfaceToProxy = interfaceToProxy;
        this.methodsToProxy = methodsToProxy;
    }

    @Override
    public Class[] getInterfaceToProxy() {
        return interfaceToProxy;
    }

    @Override
    protected Set<String> methodsToProxy() {
        return methodsToProxy;
    }

    @Override
    protected Object doInvoke(InvocationWrapper invocation) throws Throwable {

        if (!RootContext.inGlobalTransaction() || disable || RootContext.inSagaBranch()) {
            //not in transaction, or this interceptor is disabled
            return invocation.proceed();
        }
        Method method = invocation.getMethod();
        TwoPhaseBusinessAction businessAction = method.getAnnotation(TwoPhaseBusinessAction.class);
        //try method
        if (businessAction != null) {
            //save the xid
            String xid = RootContext.getXID();
            //save the previous branchType
            BranchType previousBranchType = RootContext.getBranchType();
            //if not TCC, bind TCC branchType
            if (BranchType.TCC != previousBranchType) {
                RootContext.bindBranchType(BranchType.TCC);
            }
            try {
                TwoPhaseBusinessActionParam businessActionParam = new TwoPhaseBusinessActionParam();
                businessActionParam.setActionName(businessAction.name());
                businessActionParam.setDelayReport(businessAction.isDelayReport());
                businessActionParam.setUseCommonFence(businessAction.useTCCFence());
                businessActionParam.setBranchType(BranchType.TCC);
                Map<String, Object> businessActionContextMap = new HashMap<>(4);
                //the phase two method name
                businessActionContextMap.put(Constants.COMMIT_METHOD, businessAction.commitMethod());
                businessActionContextMap.put(Constants.ROLLBACK_METHOD, businessAction.rollbackMethod());
                businessActionContextMap.put(Constants.ACTION_NAME, businessAction.name());
                businessActionContextMap.put(Constants.USE_COMMON_FENCE, businessAction.useTCCFence());
                businessActionParam.setBusinessActionContext(businessActionContextMap);
                //Handler the TCC Aspect, and return the business result
                return actionInterceptorHandler.proceed(method, invocation.getArguments(), xid, businessActionParam,
                        invocation::proceed);
            } finally {
                //if not TCC, unbind branchType
                if (BranchType.TCC != previousBranchType) {
                    RootContext.unbindBranchType();
                }
                //MDC remove branchId
                MDC.remove(RootContext.MDC_KEY_BRANCH_ID);
            }
        }

        //not TCC try method
        return invocation.proceed();
    }


}
