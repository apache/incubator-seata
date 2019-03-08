package com.alibaba.fescar.rm.tcc.remoting.parser;

import com.alibaba.fescar.common.exception.FrameworkException;
import com.alibaba.fescar.common.util.ReflectionUtil;
import com.alibaba.fescar.rm.tcc.remoting.Protocols;
import com.alibaba.fescar.rm.tcc.remoting.RemotingDesc;

/**
 * sofa-rpc remoting bean parsing
 *
 * @author zhangsen
 */
public class SofaRpcRemotingParser extends AbstractedRemotingParser {

    /**
     * is reference bean ?
     * @param bean
     * @param beanName
     * @return
     * @throws FrameworkException
     */
    @Override
    public boolean isReference(Object bean, String beanName)
            throws FrameworkException {
		String beanClassName = bean.getClass().getName();
        if("com.alipay.sofa.runtime.spring.factory.ReferenceFactoryBean".equals(beanClassName) ){
            return true;
        }
        return false;
    }

    /**
     * is service bean ?
     * @param bean
     * @param beanName
     * @return
     * @throws FrameworkException
     */
    @Override
    public boolean isService(Object bean, String beanName) 	throws FrameworkException {
		String beanClassName = bean.getClass().getName();
        if("com.alipay.sofa.runtime.spring.factory.ServiceFactoryBean".equals(beanClassName) ){
            return true;
        }
        return false;
    }

    @Override
    public RemotingDesc getServiceDesc(Object bean, String beanName) throws FrameworkException {
        if(!this.isRemoting(bean, beanName)){
            return null;
        }
        try{
            RemotingDesc serviceBeanDesc = new RemotingDesc();
            Class<?> interfaceClass = (Class<?>) ReflectionUtil.invokeMethod(bean, "getInterfaceClass");
            String interfaceClassName = (String) ReflectionUtil.getFieldValue(bean, "interfaceType");
            String uniqueId = (String) ReflectionUtil.getFieldValue(bean, "uniqueId");
            serviceBeanDesc.setInterfaceClass(interfaceClass);
            serviceBeanDesc.setInterfaceClassName(interfaceClassName);
            serviceBeanDesc.setUniqueId(uniqueId);
            serviceBeanDesc.setProtocol(Protocols.SOFA_RPC.getCode());
            if(isService(bean, beanName)){
                Object targetBean = ReflectionUtil.getFieldValue(bean, "ref");
                serviceBeanDesc.setTargetBean(targetBean);
            }
            return serviceBeanDesc;
        }catch (Throwable t){
            throw new FrameworkException(t);
        }
    }

    @Override
    public Protocols getProtocol() {
        return Protocols.SOFA_RPC;
    }
}
