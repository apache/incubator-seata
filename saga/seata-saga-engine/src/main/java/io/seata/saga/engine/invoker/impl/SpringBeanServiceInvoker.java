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
package io.seata.saga.engine.invoker.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import io.seata.common.exception.FrameworkErrorCode;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.invoker.ServiceInvoker;
import io.seata.saga.statelang.domain.ServiceTaskState;
import io.seata.saga.statelang.domain.impl.ServiceTaskStateImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * SpringBean Service Invoker
 *
 * @author lorne.cl
 */
public class SpringBeanServiceInvoker implements ServiceInvoker, ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringBeanServiceInvoker.class);

    private ApplicationContext applicationContext;

    @Override
    public Object invoke(ServiceTaskState serviceTaskState, Object... input) {

        ServiceTaskStateImpl state = (ServiceTaskStateImpl) serviceTaskState;

        Object bean = applicationContext.getBean(state.getServiceName());

        Method method = state.getMethod();
        if (method == null) {
            synchronized (state) {
                method = state.getMethod();
                if (method == null) {
                    method = findMethod(bean.getClass(), state.getServiceMethod(), state.getParameterTypes());
                    if (method != null) {
                        state.setMethod(method);
                    }
                }
            }
        }

        if (method == null) {
            throw new EngineExecutionException("No such method[" + state.getServiceMethod() + "] on BeanClass[" + bean.getClass() + "]", FrameworkErrorCode.NoSuchMethod);
        }

        Object[] args = new Object[method.getParameterCount()];
        try {
            Class[] paramTypes = method.getParameterTypes();
            if (input != null && input.length > 0) {
                int len = input.length < paramTypes.length ? input.length : paramTypes.length;
                for (int i = 0; i < len; i++) {
                    args[i] = toJavaObject(input[i], paramTypes[i]);
                }
            }
        } catch (Exception e) {
            throw new EngineExecutionException(e, "Input to java object error, Method[" + state.getServiceMethod() + "] on BeanClass[" + bean.getClass() + "]", FrameworkErrorCode.InvalidParameter);
        }

        return invokeMethod(bean, method, args);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    protected Method findMethod(Class<?> clazz, String methodName, List<String> parameterTypes) {

        if (parameterTypes == null || parameterTypes.size() == 0) {
            return BeanUtils.findDeclaredMethodWithMinimalParameters(clazz, methodName);
        } else {
            Class[] paramClassTypes = new Class[parameterTypes.size()];
            for (int i = 0; i < parameterTypes.size(); i++) {
                paramClassTypes[i] = classForName(parameterTypes.get(i));
            }
            return BeanUtils.findDeclaredMethod(clazz, methodName, paramClassTypes);
        }
    }

    protected Class classForName(String className) {
        Class clazz = getPrimitiveClass(className);
        if (clazz == null) {
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        if (clazz == null) {
            try {
                clazz = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        if (clazz == null) {
            throw new EngineExecutionException("Parameter class not found [" + className + "]", FrameworkErrorCode.ObjectNotExists);
        }
        return clazz;
    }

    protected Object invokeMethod(Object serviceBean, Method method, Object... input) {

        if (!Modifier.isPublic(method.getModifiers())) {
            throw new EngineExecutionException("Method[" + method.getName() + "] must be public", FrameworkErrorCode.MethodNotPublic);
        }
        try {
            return method.invoke(serviceBean, input);
        } catch (InvocationTargetException e) {
            Throwable targetExp = e.getTargetException();
            if (targetExp == null) {
                throw new EngineExecutionException(e, e.getMessage(), FrameworkErrorCode.MethodInvokeError);
            }

            if (targetExp instanceof RuntimeException) {
                throw (RuntimeException) targetExp;
            } else {
                throw new EngineExecutionException(targetExp, targetExp.getMessage(), FrameworkErrorCode.MethodInvokeError);
            }
        } catch (Exception e) {
            throw new EngineExecutionException(e, e.getMessage(), FrameworkErrorCode.MethodInvokeError);
        }
    }

    protected Object toJavaObject(Object value, Class paramType) {
        if (value == null) {
            return value;
        }

        if (paramType.isAssignableFrom(value.getClass())) {
            return value;
        } else if (isPrimitive(paramType)) {
            return value;
        } else {
            String jsonValue = JSON.toJSONString(value);
            return JSON.parseObject(jsonValue, paramType, Feature.SupportAutoType);
        }
    }

    protected boolean isPrimitive(Class<?> clazz) {
        return clazz.isPrimitive() //
            || clazz == Boolean.class //
            || clazz == Character.class //
            || clazz == Byte.class //
            || clazz == Short.class //
            || clazz == Integer.class //
            || clazz == Long.class //
            || clazz == Float.class //
            || clazz == Double.class //
            || clazz == BigInteger.class //
            || clazz == BigDecimal.class //
            || clazz == String.class //
            || clazz == java.util.Date.class //
            || clazz == java.sql.Date.class //
            || clazz == java.sql.Time.class //
            || clazz == java.sql.Timestamp.class //
            || clazz.isEnum() //
            ;
    }

    protected Class getPrimitiveClass(String className) {

        if (boolean.class.getName().equals(className)) {
            return boolean.class;
        } else if (char.class.getName().equals(className)) {
            return char.class;
        } else if (byte.class.getName().equals(className)) {
            return byte.class;
        } else if (short.class.getName().equals(className)) {
            return short.class;
        } else if (int.class.getName().equals(className)) {
            return int.class;
        } else if (long.class.getName().equals(className)) {
            return long.class;
        } else if (float.class.getName().equals(className)) {
            return float.class;
        } else if (double.class.getName().equals(className)) {
            return double.class;
        } else {
            return null;
        }
    }
}