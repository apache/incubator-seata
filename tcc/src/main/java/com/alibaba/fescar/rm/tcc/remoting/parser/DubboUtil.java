package com.alibaba.fescar.rm.tcc.remoting.parser;

import com.alibaba.fescar.common.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * dubbo 属性解析
 * 
 * @author zhangsen
 */
public class DubboUtil {
	
	/**
	 * 获取dubbo javaassist 代理的interface class 
	 * @throws SecurityException 
	 * @throws NoSuchFieldException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 * @throws InvocationTargetException 
	 * @throws NoSuchMethodException 
	 * @throws Throwable 
	 */
	@SuppressWarnings("rawtypes")
	public static Class<?> getAssistInterface(Object proxyBean) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		if(proxyBean == null){
			return null;
		}
		if(!proxyBean.getClass().getName().startsWith("com.alibaba.dubbo.common.bytecode.proxy")
				&&  !proxyBean.getClass().getName().startsWith("com.apache.dubbo.common.bytecode.proxy")){
			return null;
		}
		Field handlerField=proxyBean.getClass().getDeclaredField("handler");  
        handlerField.setAccessible(true);  
        Object invokerInvocationHandler = handlerField.get(proxyBean);  
        Field invokerField = invokerInvocationHandler.getClass().getDeclaredField("invoker");  
        invokerField.setAccessible(true);  
        Object invoker =  invokerField.get(invokerInvocationHandler);  
        Field failoverClusterInvokerField=invoker.getClass().getDeclaredField("invoker");  
        failoverClusterInvokerField.setAccessible(true);
        Object failoverClusterInvoker = failoverClusterInvokerField.get(invoker);  
        Class failoverClusterInvokerInterfaceClass = (Class) ReflectionUtil.invokeMethod(failoverClusterInvoker, "getInterface");
        return failoverClusterInvokerInterfaceClass;
	}

}
