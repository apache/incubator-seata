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
package io.seata.spring.annotation.datasource;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.spring.util.SpringProxyUtils;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * @author xingfudeshi@gmail.com
 * The type seata data source bean post processor
 */
public class SeataDataSourceBeanPostProcessor implements BeanPostProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeataDataSourceBeanPostProcessor.class);
    private final boolean useJdkProxy;

    public SeataDataSourceBeanPostProcessor(boolean useJdkProxy) {
        this.useJdkProxy = useJdkProxy;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource && !(bean instanceof DataSourceProxy)) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Auto proxy of [{}]", beanName);
            }
            return proxyDataSource(bean);
        }
        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof DataSourceProxy) {
            throw new ShouldNeverHappenException("Auto proxy of DataSource can't be enabled as you've created a DataSourceProxy bean." +
                "Please consider removing DataSourceProxy bean or disabling auto proxy of DataSource.");
        }
        return bean;
    }

    /**
     * proxy data source
     *
     * @param originBean
     * @return proxied datasource
     */
    private Object proxyDataSource(Object originBean) {
        DataSourceProxy dataSourceProxy = DataSourceProxyHolder.get().putDataSource((DataSource) originBean);
        if (this.useJdkProxy) {
            return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), SpringProxyUtils.getAllInterfaces(originBean), (proxy, method, args) -> handleMethodProxy(dataSourceProxy, method, args, originBean));
        } else {
            return Enhancer.create(originBean.getClass(), (MethodInterceptor) (proxy, method, args, methodProxy) -> handleMethodProxy(dataSourceProxy, method, args, originBean));
        }

    }

    /**
     * handle method proxy
     *
     * @param dataSourceProxy
     * @param method
     * @param args
     * @param originBean
     * @return proxied datasource
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private Object handleMethodProxy(DataSourceProxy dataSourceProxy, Method method, Object[] args, Object originBean) throws InvocationTargetException, IllegalAccessException {
        Method m = BeanUtils.findDeclaredMethod(DataSourceProxy.class, method.getName(), method.getParameterTypes());
        if (null != m) {
            return m.invoke(dataSourceProxy, args);
        } else {
            boolean oldAccessible = method.isAccessible();
            try {
                method.setAccessible(true);
                return method.invoke(originBean, args);
            } finally {
                //recover the original accessible for security reason
                method.setAccessible(oldAccessible);
            }
        }
    }
}
