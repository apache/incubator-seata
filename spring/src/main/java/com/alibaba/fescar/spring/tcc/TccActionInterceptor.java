package com.alibaba.fescar.spring.tcc;

import com.alibaba.fescar.common.Constants;
import com.alibaba.fescar.common.executor.Callback;
import com.alibaba.fescar.common.util.StringUtils;
import com.alibaba.fescar.core.context.RootContext;
import com.alibaba.fescar.rm.tcc.api.TwoPhaseBusinessAction;
import com.alibaba.fescar.rm.tcc.interceptor.ActionInterceptorHandler;
import com.alibaba.fescar.rm.tcc.remoting.RemotingDesc;
import com.alibaba.fescar.rm.tcc.remoting.parser.DubboUtil;
import com.alibaba.fescar.spring.util.SpringProxyUtils;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * TCC 参与者 切面
 * 
 * @author zhangsen
 *
 */
public class TccActionInterceptor implements MethodInterceptor {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(TccActionInterceptor.class);

	private ActionInterceptorHandler actionInterceptorHandler = new ActionInterceptorHandler();

	/**
	 * remoting bean info
	 */
	protected RemotingDesc remotingDesc;

	public TccActionInterceptor(){
	}

	public TccActionInterceptor(RemotingDesc remotingDesc){
		this.remotingDesc = remotingDesc;
	}

	@Override
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		Method method = getActionInterfaceMethod(invocation);
		TwoPhaseBusinessAction businessAction = method.getAnnotation(TwoPhaseBusinessAction.class);	
		//只拦截一阶段方法
	    if(businessAction != null) {
			if(StringUtils.isBlank(RootContext.getXID())){
				//非分布式事务上下文
				return invocation.proceed();
			}
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
			Class<?> interfaceType  = null;
			if(remotingDesc == null){
				interfaceType = getProxyInterface(invocation.getThis());
			}else {
				interfaceType = remotingDesc.getInterfaceClass();
			}
			if(interfaceType == null && remotingDesc.getInterfaceClassName() != null){
				interfaceType = Class.forName(remotingDesc.getInterfaceClassName(), true, Thread.currentThread().getContextClassLoader());
			}
			if(interfaceType == null){
				return invocation.getMethod();
			}
			Method method = interfaceType.getMethod(invocation.getMethod().getName(), invocation.getMethod().getParameterTypes());
			return method;
		} catch (Exception e) {
			LOGGER.warn("get Method from interface failed", e);
			return invocation.getMethod();
		}
    }

	/**
	 * 获取代理接口
	 * @param proxyBean
	 * @return
	 */
    protected Class<?> getProxyInterface(Object proxyBean) throws Exception {
		if(proxyBean.getClass().getName().startsWith("com.alibaba.dubbo.common.bytecode.proxy")){
			//dubbo javaassist proxy
			return DubboUtil.getAssistInterface(proxyBean);
		}else {
			//jdk/cglib proxy
			return SpringProxyUtils.getTargetInterface(proxyBean);
		}
	}
}
