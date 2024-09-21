/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
