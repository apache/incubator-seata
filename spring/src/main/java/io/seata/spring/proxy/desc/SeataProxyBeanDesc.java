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
package io.seata.spring.proxy.desc;

import java.lang.reflect.Method;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.seata.common.util.CollectionUtils;
import io.seata.spring.proxy.SeataProxyHandler;
import io.seata.spring.proxy.SeataProxyResultHandler;
import io.seata.spring.proxy.SeataProxyValidator;
import io.seata.spring.proxy.util.SeataProxyInterceptorUtil;

/**
 * The Seata Proxy Bean Desc
 *
 * @author wang.liang
 * @see SeataProxyParser
 * @see SeataProxyMethodDesc
 * @see SeataProxyImplementationDesc
 */
public class SeataProxyBeanDesc {

    private final Object targetBean;
    private final String targetBeanName;

    private final SeataProxyImplementationDesc implDesc;

    private final Map<Method, SeataProxyMethodDesc> methodDescMap;


    public SeataProxyBeanDesc(Object targetBean, String targetBeanName, SeataProxyImplementationDesc implDesc, Map<Method, SeataProxyMethodDesc> methodDescMap) {
        this.targetBean = targetBean;
        this.targetBeanName = targetBeanName;
        this.implDesc = implDesc != null ? implDesc : SeataProxyParser.parserImplDesc(targetBean.getClass());

        this.methodDescMap = SeataProxyParser.parserMethodDescMap(targetBean.getClass());
        if (CollectionUtils.isNotEmpty(methodDescMap)) {
            this.methodDescMap.putAll(methodDescMap);
        }
    }

    public SeataProxyBeanDesc(@Nonnull Object targetBean, String targetBeanName) {
        this(targetBean, targetBeanName, null, null);
    }


    public String getTargetBeanName() {
        return targetBeanName;
    }

    public Object getTargetBean() {
        return this.targetBean;
    }

    @Nullable
    public SeataProxyImplementationDesc getImplDesc() {
        return implDesc;
    }

    public Map<Method, SeataProxyMethodDesc> getMethodDescMap() {
        return methodDescMap;
    }

    public SeataProxyMethodDesc getMethodDesc(Method method) {
        return SeataProxyInterceptorUtil.getMethodDesc(this.getMethodDescMap(), method);
    }

    public void addMethodDesc(Method method, SeataProxyMethodDesc methodDesc) {
        this.methodDescMap.put(method, methodDesc);
    }

    @Nullable
    public SeataProxyValidator getValidator() {
        return implDesc == null ? null : implDesc.getValidator();
    }

    @Nullable
    public SeataProxyValidator getValidator(Method method) {
        SeataProxyMethodDesc methodDesc = methodDescMap.get(method);
        if (methodDesc != null) {
            SeataProxyValidator validator = methodDesc.getValidator();
            if (validator != null) {
                return validator;
            }
        }

        return implDesc == null ? null : implDesc.getValidator();
    }

    @Nullable
    public SeataProxyHandler getHandler() {
        return implDesc == null ? null : implDesc.getHandler();
    }

    @Nullable
    public SeataProxyHandler getHandler(Method method) {
        SeataProxyMethodDesc methodDesc = methodDescMap.get(method);
        if (methodDesc != null) {
            SeataProxyHandler handler = methodDesc.getHandler();
            if (handler != null) {
                return handler;
            }
        }

        return this.getHandler();
    }

    @Nullable
    public SeataProxyResultHandler getResultHandler() {
        return implDesc == null ? null : implDesc.getResultHandler();
    }

    @Nullable
    public SeataProxyResultHandler getResultHandler(Method method) {
        SeataProxyMethodDesc methodDesc = methodDescMap.get(method);
        if (methodDesc != null) {
            SeataProxyResultHandler resultHandler = methodDesc.getResultHandler();
            if (resultHandler != null) {
                return resultHandler;
            }
        }

        return this.getResultHandler();
    }
}
