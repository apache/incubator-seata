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
package io.seata.spring.annotation.scannercheckers;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;

import io.seata.common.loader.LoadLevel;
import io.seata.spring.annotation.GlobalLock;
import io.seata.spring.annotation.GlobalTransactionScanner;
import io.seata.spring.annotation.GlobalTransactional;
import io.seata.spring.annotation.ScannerChecker;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.MultiValueMap;

/**
 * Scope scanner checker.
 *
 * @author wang.liang
 */
@LoadLevel(name = "ScopeBeans", order = 200)
public class ScopeBeansScannerChecker implements ScannerChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScopeBeansScannerChecker.class);
    private static final Set<String> EXCLUDE_SCOPE_SET = new HashSet<>();

    private static final String SCOPE_NAME = "scopeName";

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
            synchronized (EXCLUDE_SCOPE_SET) {
                for (String scopeName : scopeNames) {
                    if (StringUtils.isNotBlank(scopeName)) {
                        EXCLUDE_SCOPE_SET.add(scopeName.trim().toLowerCase());
                    }
                }
            }
        }
    }

    /**
     * Match the '@Scope' beans, and check whether exclusion is required.
     */
    @Override
    public boolean check(Object bean, String beanName, @Nullable ConfigurableListableBeanFactory beanFactory) throws Exception {
        if (beanFactory == null) {
            // the beanFactory is null, pass this checker
            return true;
        }

        // get bean definition
        BeanDefinition beanDefinition;
        try {
            beanDefinition = beanFactory.getBeanDefinition(beanName);
        } catch (NoSuchBeanDefinitionException e) {
            // if no bean definition, need scan
            return true;
        }

        // find the AnnotatedBeanDefinition
        while (beanDefinition != null && !(beanDefinition instanceof AnnotatedBeanDefinition)) {
            beanDefinition = beanDefinition.getOriginatingBeanDefinition();
        }

        // if found the AnnotatedBeanDefinition, do check
        if (beanDefinition != null) {
            AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition)beanDefinition;
            if (annotatedBeanDefinition.getFactoryMethodMetadata() != null) {
                if (this.hasExcludeScope(beanName, annotatedBeanDefinition.getFactoryMethodMetadata())) {
                    // found the target @Scope, do not scan
                    return false;
                }
            }
            if (this.hasExcludeScope(beanName, annotatedBeanDefinition.getMetadata())) {
                // found the target @Scope, do not scan
                return false;
            }
        }

        // need scan
        return true;
    }

    private boolean hasExcludeScope(String beanName, AnnotatedTypeMetadata annotatedTypeMetadata) {
        MultiValueMap<String, Object> scopeAttributes = annotatedTypeMetadata.getAllAnnotationAttributes(Scope.class.getName());
        if (scopeAttributes == null || scopeAttributes.isEmpty()) {
            // not found @Scope
            return false;
        }

        if (scopeAttributes.containsKey(SCOPE_NAME)) {
            Object scopeName = scopeAttributes.getFirst(SCOPE_NAME);
            if (scopeName != null) {
                if (EXCLUDE_SCOPE_SET.contains(scopeName.toString().toLowerCase())) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Exclude bean `{}` from the `{}`, cause of `@Scope(scopeName = \"{}\")`. " +
                                        "`@{}` and `@{}` will be invalid in this bean. Please refactor the code if you want to continue using it.",
                                beanName, GlobalTransactionScanner.class.getSimpleName(), scopeName.toString(),
                                GlobalTransactional.class.getSimpleName(), GlobalLock.class.getSimpleName());
                    }

                    // found @Scope and the scopeName is in the `EXCLUDE_SCOPE_SET`, do not scan
                    return true;
                }
            }
        }

        // no @Scope to exclude was found
        return false;
    }
}
