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
package org.apache.seata.spring.remoting.parser;

import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.integration.tx.api.remoting.RemotingDesc;
import org.apache.seata.integration.tx.api.remoting.parser.AbstractedRemotingParser;
import org.apache.seata.integration.tx.api.remoting.parser.DefaultRemotingParser;
import org.apache.seata.spring.util.SpringProxyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;


public class RemotingFactoryBeanParser extends AbstractedRemotingParser {

    public ApplicationContext applicationContext;

    public RemotingFactoryBeanParser(ApplicationContext applicationContext) {
        Assert.notNull(applicationContext, "applicationContext must not be null");
        this.applicationContext = applicationContext;
    }

    /**
     * if it is proxy bean, check if the FactoryBean is Remoting bean
     *
     * @param bean               the bean
     * @param beanName           the bean name
     * @return boolean boolean
     */
    protected Object getRemotingFactoryBean(Object bean, String beanName) {
        if (!SpringProxyUtils.isProxy(bean)) {
            return null;
        }
        //the FactoryBean of proxy bean
        String factoryBeanName = getFactoryBeanName(beanName);
        Object factoryBean = null;
        if (applicationContext.containsBean(factoryBeanName)) {
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
        return DefaultRemotingParser.get().isReference(factoryBean, getFactoryBeanName(beanName));
    }

    @Override
    public boolean isService(Object bean, String beanName) {
        Object factoryBean = getRemotingFactoryBean(bean, beanName);
        if (factoryBean == null) {
            return false;
        }
        return DefaultRemotingParser.get().isService(factoryBean, getFactoryBeanName(beanName));
    }

    @Override
    public boolean isService(Class<?> beanClass) throws FrameworkException {
        return false;
    }

    @Override
    public RemotingDesc getServiceDesc(Object bean, String beanName) throws FrameworkException {
        Object factoryBean = getRemotingFactoryBean(bean, beanName);
        if (factoryBean == null) {
            return null;
        }
        return DefaultRemotingParser.get().getServiceDesc(factoryBean, getFactoryBeanName(beanName));
    }

    private String getFactoryBeanName(String beanName) {
        return "&" + beanName;
    }

    @Override
    public short getProtocol() {
        return 0;
    }

}
