/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.springcloud.anno;

import com.alibaba.fescar.spring.annotation.GlobalTransactionScanner;
import com.alibaba.fescar.springcloud.TransactionContextFilter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ObjectUtils;

public class GlobalTransactionScannerRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalTransactionScannerRegistrar.class);

    private static final String ATTR_APPLICATION_ID = "applicationId";
    private static final String ATTR_TX_SERVICE_GROUP = "txServiceGroup";
    private static final String ATTR_MODE = "mode";
    private static final String ATTR_FAILURE_HANDLER = "failureHandler";

    private Environment environment;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {
        Class<EnableGlobalTransaction> annoType = EnableGlobalTransaction.class;
        Map<String, Object> attributesMap = annotationMetadata.getAnnotationAttributes(annoType.getName(), false);
        AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(attributesMap);
        if (annotationAttributes == null) {
            throw new IllegalArgumentException(String.format(
                "@%s is not present on importing class '%s' as expected",
                annoType.getSimpleName(), annotationMetadata.getClassName()));
        }
        String applicationId = annotationAttributes.getString(ATTR_APPLICATION_ID);
        String txServiceGroup = annotationAttributes.getString(ATTR_TX_SERVICE_GROUP);
        int mode = annotationAttributes.getNumber(ATTR_MODE);
        Class<?> failureHandlerClass = annotationAttributes.getClass(ATTR_FAILURE_HANDLER);

        if (ObjectUtils.isEmpty(applicationId)) {
            applicationId = environment.getProperty("spring.application.name");
        }

        Object failureHandler = null;
        if (failureHandlerClass != void.class) {
            try {
                failureHandler = failureHandlerClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                LOGGER.warn("can't initialize the failureHandler [{}] : {}", failureHandlerClass.getName(), e.getLocalizedMessage());
            }
        }

        registerGlobalTransactionScanner(registry, applicationId, txServiceGroup, mode, failureHandler);

    }

    private void registerGlobalTransactionScanner(BeanDefinitionRegistry registry, Object applicationId,
        Object txServiceGroup, int mode, Object failureHandler) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(GlobalTransactionScanner.class);
        if (!ObjectUtils.isEmpty(applicationId)) {
            builder.addConstructorArgValue(applicationId);
        }
        builder.addConstructorArgValue(txServiceGroup);
        builder.addConstructorArgValue(mode);
        if (failureHandler != null) {
            builder.addConstructorArgValue(failureHandler);
        }
        registry.registerBeanDefinition(GlobalTransactionScanner.class.getName(), builder.getBeanDefinition());
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

}
