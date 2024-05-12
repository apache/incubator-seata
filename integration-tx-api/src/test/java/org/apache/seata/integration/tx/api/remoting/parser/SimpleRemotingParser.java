package org.apache.seata.integration.tx.api.remoting.parser;

import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.integration.tx.api.remoting.Protocols;
import org.apache.seata.integration.tx.api.remoting.RemotingDesc;

public class SimpleRemotingParser extends AbstractedRemotingParser {

    @Override
    public boolean isReference(Object bean, String beanName) throws FrameworkException {
        return isRemoteBean(bean);
    }

    @Override
    public boolean isService(Object bean, String beanName) throws FrameworkException {
        return isRemoteBean(bean);
    }

    @Override
    public boolean isService(Class<?> beanClass) throws FrameworkException {
        return isRemoteBean(beanClass);
    }

    @Override
    public RemotingDesc getServiceDesc(Object bean, String beanName) throws FrameworkException {
        if (!this.isRemoting(bean, beanName)) {
            return null;
        }

        RemotingDesc remotingDesc = new RemotingDesc();
        remotingDesc.setReference(this.isReference(bean, beanName));
        remotingDesc.setService(this.isService(bean, beanName));
        remotingDesc.setProtocol(Protocols.IN_JVM);
        remotingDesc.setServiceClass(bean.getClass());
        remotingDesc.setServiceClassName(remotingDesc.getServiceClass().getName());
        remotingDesc.setTargetBean(bean);

        return remotingDesc;
    }

    @Override
    public short getProtocol() {
        return Protocols.IN_JVM;
    }

    private boolean isRemoteBean(Object bean) {
        return isRemoteBean(bean.getClass());
    }

    private boolean isRemoteBean(Class<?> clazz) {
        return clazz.isAnnotationPresent(RemoteBean.class);
    }
}
