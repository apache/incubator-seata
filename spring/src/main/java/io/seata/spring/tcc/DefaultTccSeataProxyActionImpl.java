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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.seata.common.Constants;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.ReflectionUtil;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.interceptor.ActionContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * The Default Implement of the Tcc Seata Proxy Action
 *
 * @author wang.liang
 */
public class DefaultTccSeataProxyActionImpl implements TccSeataProxyAction, ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTccSeataProxyActionImpl.class);

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Prepare.
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
     * Commit.
     *
     * @param actionContext the action context
     * @return the boolean
     */
    @Override
    public boolean commit(BusinessActionContext actionContext) throws Throwable {
        // get the arguments
        @SuppressWarnings("unchecked")
        Class<?>[] parameterTypes = actionContext.getActionContext(Constants.TCC_PROXY_METHOD_PARAMETER_TYPES, Class[].class);
        Object[] args = null;
        if (CollectionUtils.isNotEmpty(parameterTypes)) {
            // get method arguments
            args = actionContext.getActionContext(Constants.TCC_PROXY_METHOD_ARGS, Object[].class);
            // convert method arguments
            Class<?> parameterClass;
            for (int i = 0; i < parameterTypes.length; ++i) {
                if (args[i] == null) {
                    continue;
                }

                // get the class of the parameter
                parameterClass = parameterTypes[i];

                // convert argument by parameter class
                args[i] = ActionContextUtil.convertActionContext(args[i], parameterClass);
            }
        }

        // get the target bean
        String targetBeanName = actionContext.getActionContext(Constants.TCC_PROXY_TARGET_BEAN_NAME, String.class);
        Object targetBean = applicationContext.getBean(targetBeanName);
        // get the method of the target bean
        String methodName = actionContext.getActionContext(Constants.TCC_PROXY_METHOD_NAME, String.class);
        Method method = ReflectionUtil.getMethod(targetBean.getClass(), methodName, parameterTypes);

        // invoke the method of the target bean
        try {
            ReflectionUtil.invokeMethod(targetBean, method, args);
            LOGGER.info("commit the proxy operation '{}' success", ReflectionUtil.methodToString(method));
            return true;
        }
        // TODO: 等 PR #3803 (解决ReflectionUtil的BUG的PR) 合并后，将注释的代码放开。
        /* catch (InvocationTargetException e) {
            LOGGER.error("commit the proxy operation '{}' failed", ReflectionUtil.methodToString(method), e.getCause());
        }*/
        catch (Exception e) {
            LOGGER.error("commit the proxy operation '{}' failed", ReflectionUtil.methodToString(method), e);
        }

        return false;
    }

    /**
     * Rollback.
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
