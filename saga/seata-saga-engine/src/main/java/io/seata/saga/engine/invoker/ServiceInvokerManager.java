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
package io.seata.saga.engine.invoker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.saga.statelang.domain.DomainConstants;
import org.springframework.util.StringUtils;

/**
 * Service Invoker Manager
 *
 * @author lorne.cl
 */
public class ServiceInvokerManager {

    private Map<String, ServiceInvoker> serviceInvokerMap = new ConcurrentHashMap<>();

    public ServiceInvoker getServiceInvoker(String serviceType) {
        if (StringUtils.isEmpty(serviceType)) {
            serviceType = DomainConstants.SERVICE_TYPE_SPRING_BEAN;
        }
        return serviceInvokerMap.get(serviceType);
    }

    public void putServiceInvoker(String serviceType, ServiceInvoker serviceInvoker) {
        serviceInvokerMap.put(serviceType, serviceInvoker);
    }

    public Map<String, ServiceInvoker> getServiceInvokerMap() {
        return serviceInvokerMap;
    }
}