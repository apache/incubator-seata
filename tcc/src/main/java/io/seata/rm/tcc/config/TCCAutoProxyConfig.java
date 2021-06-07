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
package io.seata.rm.tcc.config;

import java.util.List;

import org.springframework.core.Ordered;

/**
 * TCC Auto Proxy Config
 *
 * @author wang.liang
 */
public class TCCAutoProxyConfig {

    /**
     * Tcc auto proxy enabled
     */
    private boolean enabled = false;

    /**
     * TCC auto proxy bean classes
     */
    private List<String> proxyBeanClasses;

    /**
     * TCC auto proxy bean names
     */
    private List<String> proxyBeanNames;

    /**
     * TCC auto proxy interceptor order
     */
    private int proxyInterceptorOrder = Ordered.HIGHEST_PRECEDENCE + 1000;


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getProxyBeanClasses() {
        return proxyBeanClasses;
    }

    public void setProxyBeanClasses(List<String> proxyBeanClasses) {
        this.proxyBeanClasses = proxyBeanClasses;
    }

    public List<String> getProxyBeanNames() {
        return proxyBeanNames;
    }

    public void setProxyBeanNames(List<String> proxyBeanNames) {
        this.proxyBeanNames = proxyBeanNames;
    }

    public int getProxyInterceptorOrder() {
        return proxyInterceptorOrder;
    }

    public void setProxyInterceptorOrder(int proxyInterceptorOrder) {
        this.proxyInterceptorOrder = proxyInterceptorOrder;
    }
}

