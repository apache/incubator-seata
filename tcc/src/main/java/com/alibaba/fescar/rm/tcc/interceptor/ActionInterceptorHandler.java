package com.alibaba.fescar.rm.tcc.interceptor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fescar.common.Constants;
import com.alibaba.fescar.common.exception.FrameworkException;
import com.alibaba.fescar.common.executor.Callback;
import com.alibaba.fescar.common.util.NetUtil;
import com.alibaba.fescar.core.context.RootContext;
import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.rm.DefaultResourceManager;
import com.alibaba.fescar.rm.tcc.api.BusinessActionContext;
import com.alibaba.fescar.rm.tcc.api.BusinessActionContextParameter;
import com.alibaba.fescar.rm.tcc.api.TwoPhaseBusinessAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 处理TCC参与者切面逻辑：设置上下文、创建分支事务记录
 * 
 * @author zhangsen
 */
public class ActionInterceptorHandler {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(ActionInterceptorHandler.class);
	
	/**
	 * 处理 TCC 切面 ：创建主事务记录，
	 * @param method
	 * @param arguments
	 * @param businessAction
	 * @param targetCallback
	 * @return
	 * @throws Throwable
	 */
	public Map<String, Object> proceed(Method method, Object[] arguments, TwoPhaseBusinessAction businessAction, Callback<Object> targetCallback) throws Throwable {
		Map<String, Object> ret = new HashMap<String, Object>();
		
		//TCC 一阶段方法
        String actionName = businessAction.name();
        String txId = RootContext.getXID();
        BusinessActionContext actionContext = new BusinessActionContext();
        //填充事物号
        actionContext.setTxId(txId);
        //填充参与者名字,此处填写资源的唯一id
        actionContext.setActionName(actionName);
        //TODO fescar当前版本暂无主事务记录上下文
        
        //创建分支事务记录
        String branchId = doTccActionLogStore(method, arguments, businessAction, actionContext);
        actionContext.setActionId(branchId);
        
        //参数填冲，找到并设置 BusinessActionContext参数
        Class<?>[] types = method.getParameterTypes();
        int argIndex = 0;
        for (Class<?> cls : types) {
            if (cls.getName().equals(BusinessActionContext.class.getName())) {
            	arguments[argIndex] = actionContext;
                break;
            }
            argIndex++;
        }
        //最终参数
        ret.put(Constants.TCC_METHOD_ARGUMENTS, arguments);
        //目标函数结果
        ret.put(Constants.TCC_METHOD_RESULT, targetCallback.execute());
        return ret;
	}

	/**
     * 创建分支事务记录
	 */
	protected String doTccActionLogStore(Method method, Object[] arguments, TwoPhaseBusinessAction businessAction, BusinessActionContext actionContext) {
		String actionName = actionContext.getActionName();
        String txId = actionContext.getTxId();
        //提取参数至上下文
        Map<String, Object> context = fetchActionRequestContext(method, arguments);
        context.put(Constants.ACTION_START_TIME, System.currentTimeMillis());
        
        //初始化业务上下文
        initBusinessContext(context, method, businessAction);
        //运行环境上下文
        initFrameworkContext(context);
        //参与者名称
        actionContext.setActionContext(context);

        //TCC 分支上下文
        Map<String, Object> applicationContext = new HashMap<String, Object>();
        applicationContext.put(Constants.TCC_ACTION_CONTEXT, context);
        if(actionContext.getActivityContext() != null && actionContext.getActivityContext().getContext() != null){
            applicationContext.put( Constants.TCC_ACTIVITY_CONTEXT, actionContext.getActivityContext().getContext());
        }
        String applicationContextStr = JSON.toJSONString(applicationContext);
        try {
        	//注册分支事务
        	Long branchId = DefaultResourceManager.get().branchRegister(BranchType.TCC, actionName, null, txId, applicationContextStr, null);
        	return String.valueOf(branchId);
        }catch(Throwable t){
            String msg = "TCC branch Register error, xid:" + txId;
        	LOGGER.error(msg, t);
        	throw new FrameworkException(t, msg);
        }
	}
	
	/**
     * 初始化运行环境上下文
     * @param context
     */
    protected void initFrameworkContext(Map<String, Object> context){
        try {
            context.put(Constants.HOST_NAME, NetUtil.getLocalIp());
        } catch (Throwable t) {
        	LOGGER.warn("getLocalIP error", t);
        }
    }
	
	 /**
     * 初始化业务上下文
     * @param context
     * @param method
     * @param businessAction
     */
    protected void initBusinessContext(Map<String,Object> context, Method method, TwoPhaseBusinessAction businessAction) {
    	if(method != null){
        	//一阶段方法名称
            context.put(Constants.PREPARE_METHOD, method.getName());
        }
        if(businessAction != null){
        	//二阶段方法名称
            context.put(Constants.COMMIT_METHOD, businessAction.commitMethod());
            context.put(Constants.ROLLBACK_METHOD, businessAction.rollbackMethod());
            context.put(Constants.ACTION_NAME, businessAction.name());
        }
    }

    /**
     * 从一阶段方法中提取参数，存入分布式事务上下文
     * @param method
     * @param arguments
     * @return
     */
    protected Map<String, Object> fetchActionRequestContext(Method method, Object[] arguments) {
        Map<String, Object> context = new HashMap<String, Object>();

        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (int j = 0; j < parameterAnnotations[i].length; j++) {
                if (parameterAnnotations[i][j] instanceof BusinessActionContextParameter) {
                    BusinessActionContextParameter param = (BusinessActionContextParameter) parameterAnnotations[i][j];
                    if (null == arguments[i]) {
                        throw new IllegalArgumentException("@BusinessActionContextParameter 's params can not null");
                    }
                    Object paramObject = arguments[i];
                    int index = param.index();
                    //如果是，则找到特定参数
                    if (index >= 0) {
                        @SuppressWarnings("unchecked")
						Object targetParam = ((List<Object>) paramObject).get(index);
                        if (param.isParamInProperty()) {
                            context.putAll(ActionContextUtil.fetchContextFromObject(targetParam));
                        } else {
                            context.put(param.paramName(), targetParam);
                        }
                    } else {
                        if (param.isParamInProperty()) {
                            context.putAll(ActionContextUtil.fetchContextFromObject(paramObject));
                        } else {
                            context.put(param.paramName(), paramObject);
                        }
                    }
                }
            }
        }
        return context;
    }
	

}
