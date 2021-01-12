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
package io.seata.config.springcloud;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import static io.seata.common.Constants.BEAN_NAME_SPRING_APPLICATION_CONTEXT_PROVIDER;

/**
 * @author xingfudeshi@gmail.com
 * The type spring application context provider registrar
 */
public class SpringApplicationContextProviderRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        if (!registry.containsBeanDefinition(BEAN_NAME_SPRING_APPLICATION_CONTEXT_PROVIDER)) {
            AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(SpringApplicationContextProvider.class).getBeanDefinition();
            registry.registerBeanDefinition(BEAN_NAME_SPRING_APPLICATION_CONTEXT_PROVIDER, beanDefinition);
        }
    }
}