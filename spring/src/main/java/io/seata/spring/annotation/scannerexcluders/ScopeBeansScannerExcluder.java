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
package io.seata.spring.annotation.scannerexcluders;

import io.seata.common.loader.LoadLevel;
import io.seata.spring.annotation.GlobalLock;
import io.seata.spring.annotation.GlobalTransactionScanner;
import io.seata.spring.annotation.GlobalTransactional;
import io.seata.spring.annotation.ScannerExcluder;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.scope.ScopedProxyFactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Scope scanner excluder.
 *
 * @author wang.liang
 */
@LoadLevel(name = "ScopeBeans", order = 200)
public class ScopeBeansScannerExcluder implements ScannerExcluder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScopeBeansScannerExcluder.class);
    private static final Set<String> EXCLUDE_SCOPE_SET = Collections.synchronizedSet(new HashSet<>());

    public static final String REQUEST_SCOPE_NAME = "request";
    public static final String SESSION_SCOPE_NAME = "session";
    public static final String JOB_SCOPE_NAME = "job";
    public static final String STEP_SCOPE_NAME = "step";

    static {
        EXCLUDE_SCOPE_SET.add(REQUEST_SCOPE_NAME);
        EXCLUDE_SCOPE_SET.add(SESSION_SCOPE_NAME);
        EXCLUDE_SCOPE_SET.add(JOB_SCOPE_NAME);
        EXCLUDE_SCOPE_SET.add(STEP_SCOPE_NAME);
    }

    /**
     * Add more exclude scopes.
     *
     * @param scopeNames the scope names
     */
    public static void addExcludeScopes(String... scopeNames) {
        if (ArrayUtils.isNotEmpty(scopeNames)) {
            for (String scopeName : scopeNames) {
                if (StringUtils.isNotBlank(scopeName)) {
                    EXCLUDE_SCOPE_SET.add(scopeName.trim().toLowerCase());
                }
            }
        }
    }

    @Override
    public boolean isMatch(Object bean, String beanName, BeanDefinition beanDefinition) throws Throwable {
        if (bean instanceof ScopedProxyFactoryBean) {
            // exclude
            return true;
        }

        while (beanDefinition != null && !(beanDefinition instanceof AnnotatedBeanDefinition)) {
            beanDefinition = beanDefinition.getOriginatingBeanDefinition();
        }

        if (beanDefinition != null) {
            AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) beanDefinition;
            if (annotatedBeanDefinition.getFactoryMethodMetadata() != null) {
                if (this.hasExcludeScope(beanName, annotatedBeanDefinition.getFactoryMethodMetadata())) {
                    // exclude
                    return true;
                }
            }
            if (this.hasExcludeScope(beanName, annotatedBeanDefinition.getMetadata())) {
                // exclude
                return true;
            }
        }

        // not exclude
        return false;
    }

    private boolean hasExcludeScope(String beanName, AnnotatedTypeMetadata annotatedTypeMetadata) {
        MultiValueMap<String, Object> scopeAttributes = annotatedTypeMetadata.getAllAnnotationAttributes(Scope.class.getName());

        if (scopeAttributes == null || scopeAttributes.isEmpty()) {
            return false;
        }

        if (scopeAttributes.containsKey("scopeName")) {
            Object scopeName = scopeAttributes.getFirst("scopeName");
            if (scopeName != null) {
                if (EXCLUDE_SCOPE_SET.contains(scopeName.toString().toLowerCase())) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Exclude bean '{}' from the {}, cause of @Scope(scopeName = \"{}\")." +
                                        " @{} and @{} will be invalid in this bean.",
                                beanName, GlobalTransactionScanner.class.getSimpleName(), scopeName.toString(),
                                GlobalTransactional.class.getSimpleName(), GlobalLock.class.getSimpleName());
                    }

                    // exclude
                    return true;
                }
            }
        }

        return false;
    }
}
