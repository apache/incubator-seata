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
package io.seata.spring.annotation.datasource;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;

/**
 * @author xingfudeshi@gmail.com
 * The type auto data source proxy registrar
 */
public class AutoDataSourceProxyRegistrar implements ImportBeanDefinitionRegistrar {
    private static final String ATTRIBUTE_KEY_USE_JDK_PROXY = "useJdkProxy";
    private static final String ATTRIBUTE_KEY_EXCLUDES = "excludes";
    private static final String ATTRIBUTE_KEY_DATA_SOURCE_PROXY_MODE = "dataSourceProxyMode";
    public static final String BEAN_NAME_SEATA_AUTO_DATA_SOURCE_PROXY_CREATOR = "seataAutoDataSourceProxyCreator";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        if (!registry.containsBeanDefinition(BEAN_NAME_SEATA_AUTO_DATA_SOURCE_PROXY_CREATOR)) {
            Map<String, Object> annotationAttributes = importingClassMetadata.getAnnotationAttributes(EnableAutoDataSourceProxy.class.getName());

            boolean useJdkProxy = Boolean.parseBoolean(annotationAttributes.get(ATTRIBUTE_KEY_USE_JDK_PROXY).toString());
            String[] excludes = (String[]) annotationAttributes.get(ATTRIBUTE_KEY_EXCLUDES);
            String dataSourceProxyMode = (String) annotationAttributes.get(ATTRIBUTE_KEY_DATA_SOURCE_PROXY_MODE);

            AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(SeataAutoDataSourceProxyCreator.class)
                .addConstructorArgValue(useJdkProxy)
                .addConstructorArgValue(excludes)
                .addConstructorArgValue(dataSourceProxyMode)
                .getBeanDefinition();
            registry.registerBeanDefinition(BEAN_NAME_SEATA_AUTO_DATA_SOURCE_PROXY_CREATOR, beanDefinition);
        }
    }

}
