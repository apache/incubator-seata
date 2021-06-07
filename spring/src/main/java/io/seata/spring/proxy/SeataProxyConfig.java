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

import java.util.Set;

import org.springframework.core.Ordered;

/**
 * Seata Proxy Config
 *
 * @author wang.liang
 */
public class SeataProxyConfig {

    /**
     * proxy enabled
     */
    private boolean enabled = false;

    /**
     * target bean classes
     */
    private Set<String> targetBeanClasses;

    /**
     * target bean names
     */
    private Set<String> targetBeanNames;

    /**
     * proxy handler type
     */
    private String proxyHandlerType = "tcc";

    /**
     * proxy interceptor order
     */
    private int proxyInterceptorOrder = Ordered.HIGHEST_PRECEDENCE + 1000;


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<String> getTargetBeanClasses() {
        return targetBeanClasses;
    }

    public void setTargetBeanClasses(Set<String> targetBeanClasses) {
        this.targetBeanClasses = targetBeanClasses;
    }

    public Set<String> getTargetBeanNames() {
        return targetBeanNames;
    }

    public void setTargetBeanNames(Set<String> targetBeanNames) {
        this.targetBeanNames = targetBeanNames;
    }

    public String getProxyHandlerType() {
        return proxyHandlerType;
    }

    public void setProxyHandlerType(String proxyHandlerType) {
        this.proxyHandlerType = proxyHandlerType;
    }

    public int getProxyInterceptorOrder() {
        return proxyInterceptorOrder;
    }

    public void setProxyInterceptorOrder(int proxyInterceptorOrder) {
        this.proxyInterceptorOrder = proxyInterceptorOrder;
    }
}

