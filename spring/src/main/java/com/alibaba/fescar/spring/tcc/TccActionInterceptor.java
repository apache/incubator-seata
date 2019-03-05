package com.alibaba.fescar.spring.tcc;

import java.lang.reflect.Method;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fescar.common.Constants;
import com.alibaba.fescar.common.executor.Callback;
import com.alibaba.fescar.rm.tcc.api.TwoPhaseBusinessAction;
import com.alibaba.fescar.rm.tcc.interceptor.ActionInterceptorHandler;

/**
 * TCC 参与者 切面
 * 
 * @author zhangsen
 *
 */
public class TccActionInterceptor implements MethodInterceptor {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(TccActionInterceptor.class);

	private ActionInterceptorHandler actionInterceptorHandler = new ActionInterceptorHandler();
	
	@Override
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		Method method = getActionInterfaceMethod(invocation);
		TwoPhaseBusinessAction businessAction = method.getAnnotation(TwoPhaseBusinessAction.class);	
		//只拦截一阶段方法
	    if(businessAction != null) {
	    	Object[] methodArgs = invocation.getArguments();
	    	//处理TCC 参与者切面
			Map<String, Object> ret = actionInterceptorHandler.proceed(method, methodArgs, businessAction, new Callback<Object>(){
				@Override
				public Object execute() throws Throwable {
					return invocation.proceed();
				}
	    	});
	    	//重新设置参数
	    	Object[] targetArguments = (Object[]) ret.get(Constants.TCC_METHOD_ARGUMENTS);
	    	if(targetArguments != null && methodArgs != null){
	    		for(int i = 0; i < targetArguments.length; i ++){
	    			methodArgs[i] = targetArguments[i];
	    		}
	    	}
	    	//返回target method 结果
	    	return ret.get(Constants.TCC_METHOD_RESULT);
	    }
		return invocation.proceed();
	}

	
	/**
     * 被拦截的方法
     */
    protected Method getActionInterfaceMethod(MethodInvocation invocation) {
        try {
            Method method = invocation.getMethod();
            return method;
        } catch (Throwable t) {
        	LOGGER.warn("get Method from interface failed", t);
        	throw t;
        }
    }
}
