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
 * TCC Interceptor
 *
 * @author zhangsen
 */
public class TccActionInterceptor implements MethodInterceptor {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(TccActionInterceptor.class);

	private ActionInterceptorHandler actionInterceptorHandler = new ActionInterceptorHandler();

	/**
	 * remoting bean info
	 */
	protected RemotingDesc remotingDesc;

	/**
	 * Instantiates a new Tcc action interceptor.
	 */
	public TccActionInterceptor(){
	}

	/**
	 * Instantiates a new Tcc action interceptor.
	 *
	 * @param remotingDesc the remoting desc
	 */
	public TccActionInterceptor(RemotingDesc remotingDesc){
		this.remotingDesc = remotingDesc;
	}

	@Override
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		Method method = getActionInterfaceMethod(invocation);
		TwoPhaseBusinessAction businessAction = method.getAnnotation(TwoPhaseBusinessAction.class);	
		//try method
	    if(businessAction != null) {
			if(StringUtils.isBlank(RootContext.getXID())){
				//not in distribute transaction
				return invocation.proceed();
			}
	    	Object[] methodArgs = invocation.getArguments();
	    	//Handler the TCC Aspect
			Map<String, Object> ret = actionInterceptorHandler.proceed(method, methodArgs, businessAction, new Callback<Object>(){
				@Override
				public Object execute() throws Throwable {
					return invocation.proceed();
				}
	    	});
	    	//reset the parameters of try method
	    	Object[] targetArguments = (Object[]) ret.get(Constants.TCC_METHOD_ARGUMENTS);
	    	if(targetArguments != null && methodArgs != null){
	    		for(int i = 0; i < targetArguments.length; i ++){
	    			methodArgs[i] = targetArguments[i];
	    		}
	    	}
	    	//return the final result
	    	return ret.get(Constants.TCC_METHOD_RESULT);
	    }
		return invocation.proceed();
	}

	/**
	 * get the method from interface
	 *
	 * @param invocation the invocation
	 * @return the action interface method
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
	 * get the interface of proxy
	 *
	 * @param proxyBean the proxy bean
	 * @return proxy interface
	 * @throws Exception the exception
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
