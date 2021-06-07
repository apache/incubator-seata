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
package io.seata.spring.tcc;

import io.seata.common.Constants;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.ReflectionUtil;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.interceptor.ActionContextUtil;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * The Default Tcc Auto Proxy Action Implement
 *
 * @author wang.liang
 */
public class DefaultTccAutoProxyActionImpl implements TccAutoProxyAction, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Prepare boolean.
     *
     * @param actionContext the action context
     * @return the boolean
     */
    @Override
    public boolean prepare(BusinessActionContext actionContext) {
        // do nothing, only report applicationData at the beginning of the TCC branch
        return true;
    }

    /**
     * Commit boolean.
     *
     * @param actionContext the action context
     * @return the boolean
     */
    @Override
    public boolean commit(BusinessActionContext actionContext) throws Throwable {
        String beanName = actionContext.getActionContext(Constants.TCC_PROXY_BEAN_NAME, String.class);

        // get the parameters
        @SuppressWarnings("unchecked")
        String[] methodParameterTypeStrs = actionContext.getActionContext(Constants.TCC_PROXY_METHOD_PARAMETER_TYPES, String[].class);
        Class<?>[] methodParameterTypes = null;
        Object[] methodArgs = null;
        if (CollectionUtils.isNotEmpty(methodParameterTypeStrs)) {
            methodParameterTypes = new Class<?>[methodParameterTypeStrs.length];

            // get method parameters
            methodArgs = actionContext.getActionContext(Constants.TCC_PROXY_METHOD_ARGS, Object[].class);
            // convert method parameters
            Class<?> parameterClass;
            for (int i = 0; i < methodParameterTypeStrs.length; ++i) {
                if (methodArgs[i] == null) {
                    continue;
                }

                // convert type string to class
                String type = methodParameterTypeStrs[i];
                parameterClass = ReflectionUtil.getClassByName(type);
                methodParameterTypes[i] = parameterClass;

                // convert parameter by class
                methodArgs[i] = ActionContextUtil.convertActionContext(methodArgs[i], parameterClass);
            }
        }

        // get the bean
        Object bean = applicationContext.getBean(beanName);

        // get the method
        String methodName = actionContext.getActionContext(Constants.TCC_PROXY_METHOD_NAME, String.class);

        try {
            ReflectionUtil.invokeMethod(bean, methodName, methodParameterTypes, methodArgs);
        }
        // TODO: 等 PR #3803 (解决ReflectionUtil的BUG的PR) 合并后，修正这里的代码。
        catch (NoSuchMethodException e) {
            throw e;
        }
        /* catch (InvocationTargetException e) {
            throw e.getCause();
        }*/

        return true;
    }

    /**
     * rollback.
     *
     * @param actionContext the action context
     * @return the boolean
     */
    @Override
    public boolean rollback(BusinessActionContext actionContext) {
        // do nothing, remove the TCC branch directly
        return true;
    }
}
