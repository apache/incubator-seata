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
package org.apache.seata.benchmark.saga;

import org.apache.seata.saga.engine.invoker.ServiceInvoker;
import org.apache.seata.saga.statelang.domain.ServiceTaskState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service invoker for benchmark Saga services.
 * Invokes registered service methods based on service name and method name.
 */
public class BenchmarkServiceInvoker implements ServiceInvoker {

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkServiceInvoker.class);

    private final Map<String, Object> serviceRegistry = new HashMap<>();

    public void registerService(String serviceName, Object service) {
        serviceRegistry.put(serviceName, service);
        LOGGER.debug("Registered service: {}", serviceName);
    }

    @Override
    public Object invoke(ServiceTaskState serviceTaskState, Object... input) throws Exception {
        String serviceName = serviceTaskState.getServiceName();
        String methodName = serviceTaskState.getServiceMethod();

        Object service = serviceRegistry.get(serviceName);
        if (service == null) {
            throw new IllegalArgumentException("Service not found: " + serviceName);
        }

        // Find the method
        Method method = findMethod(service.getClass(), methodName);
        if (method == null) {
            throw new NoSuchMethodException("Method not found: " + methodName + " in service: " + serviceName);
        }

        // Prepare input parameters
        Object inputParam = prepareInput(input);

        LOGGER.debug("Invoking service: {}.{}()", serviceName, methodName);

        // Invoke the method
        return method.invoke(service, inputParam);
    }

    private Method findMethod(Class<?> clazz, String methodName) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Object prepareInput(Object[] input) {
        if (input == null || input.length == 0) {
            return new HashMap<String, Object>();
        }

        // If input is a list with one map element, extract it
        if (input.length == 1) {
            Object first = input[0];
            if (first instanceof List) {
                List<?> list = (List<?>) first;
                if (!list.isEmpty() && list.get(0) instanceof Map) {
                    // Merge all maps in the list
                    Map<String, Object> merged = new HashMap<>();
                    for (Object item : list) {
                        if (item instanceof Map) {
                            merged.putAll((Map<String, Object>) item);
                        }
                    }
                    return merged;
                }
            }
            if (first instanceof Map) {
                return first;
            }
        }

        // Try to merge all inputs into a single map
        Map<String, Object> merged = new HashMap<>();
        for (Object o : input) {
            if (o instanceof Map) {
                merged.putAll((Map<String, Object>) o);
            }
        }
        return merged.isEmpty() ? input[0] : merged;
    }
}
