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
package io.seata.spring.proxy;

import java.util.HashMap;
import java.util.Map;

import io.seata.spring.proxy.desc.SeataProxyBeanDesc;
import org.springframework.context.annotation.Lazy;

/**
 * Seata Proxy Bean Register
 *
 * @author wang.liang
 * @see io.seata.spring.annotation.GlobalTransactionScanner
 */
@Lazy(false)
public class SeataProxyBeanRegister {

    private Map<Class<?>, SeataProxyBeanDesc> beanClassBeanDescMap = new HashMap<>();
    private Map<String, SeataProxyBeanDesc> beanNameBeanDescMap = new HashMap<>();


    //region the methods for register proxy bean

    public void registerProxyBean(Class<?> beanClass, SeataProxyBeanDesc proxyBeanDesc) {
        beanClassBeanDescMap.put(beanClass, proxyBeanDesc);
    }

    public void registerProxyBean(Class<?> beanClass) {
        registerProxyBean(beanClass, null);
    }

    public void registerProxyBean(String beanName, SeataProxyBeanDesc proxyBeanDesc) {
        beanNameBeanDescMap.put(beanName, proxyBeanDesc);
    }

    public void registerProxyBean(String beanName) {
        registerProxyBean(beanName, null);
    }

    public void merge(SeataProxyBeanRegister register) {
        if (register != null) {
            beanClassBeanDescMap.putAll(register.getBeanClassBeanDescMap());
            beanNameBeanDescMap.putAll(register.getBeanNameBeanDescMap());
        }
    }

    //endregion


    public Map<Class<?>, SeataProxyBeanDesc> getBeanClassBeanDescMap() {
        return beanClassBeanDescMap;
    }

    public Map<String, SeataProxyBeanDesc> getBeanNameBeanDescMap() {
        return beanNameBeanDescMap;
    }
}
