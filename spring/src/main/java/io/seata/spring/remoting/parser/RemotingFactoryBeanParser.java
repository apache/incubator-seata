/*
 *  Copyright 1999-2019 Seata.io Group.
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
package io.seata.spring.remoting.parser;

import io.seata.common.exception.FrameworkException;
import io.seata.integration.tx.api.remoting.RemotingDesc;
import io.seata.integration.tx.api.remoting.parser.AbstractedRemotingParser;
import io.seata.integration.tx.api.remoting.parser.DefaultRemotingParser;
import io.seata.spring.util.SpringProxyUtils;
import org.springframework.context.ApplicationContext;

/**
 * @author leezongjie
 */
public class RemotingFactoryBeanParser extends AbstractedRemotingParser {

    public static ApplicationContext applicationContext;

    /**
     * if it is proxy bean, check if the FactoryBean is Remoting bean
     *
     * @param bean               the bean
     * @param beanName           the bean name
     * @return boolean boolean
     */
    protected static Object getRemotingFactoryBean(Object bean, String beanName) {
        if (!SpringProxyUtils.isProxy(bean)) {
            return null;
        }
        //the FactoryBean of proxy bean
        String factoryBeanName = "&" + beanName;
        Object factoryBean = null;
        if (applicationContext != null && applicationContext.containsBean(factoryBeanName)) {
            factoryBean = applicationContext.getBean(factoryBeanName);
        }
        return factoryBean;
    }

    @Override
    public boolean isReference(Object bean, String beanName) {
        Object factoryBean = getRemotingFactoryBean(bean, beanName);
        if (factoryBean == null) {
            return false;
        }
        return DefaultRemotingParser.get().isReference(bean, beanName);
    }

    @Override
    public boolean isService(Object bean, String beanName) {
        Object factoryBean = getRemotingFactoryBean(bean, beanName);
        if (factoryBean == null) {
            return false;
        }
        return DefaultRemotingParser.get().isReference(bean, beanName);
    }

    @Override
    public RemotingDesc getServiceDesc(Object bean, String beanName) throws FrameworkException {
        Object factoryBean = getRemotingFactoryBean(bean, beanName);
        if (factoryBean == null) {
            return null;
        }
        return DefaultRemotingParser.get().getServiceDesc(bean, beanName);
    }


    @Override
    public short getProtocol() {
        return 0;
    }
}