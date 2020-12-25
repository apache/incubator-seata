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
package io.seata.spring.boot.autoconfigure;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static io.seata.spring.boot.autoconfigure.StarterConstants.PROPERTY_BEAN_MAP;

/**
 * @author xingfudeshi@gmail.com
 */
public class PropertyBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean.getClass().isAnnotationPresent(ConfigurationProperties.class)) {
            ConfigurationProperties configurationProperties = bean.getClass().getAnnotation(ConfigurationProperties.class);
            String prefix = configurationProperties.prefix();
            PROPERTY_BEAN_MAP.computeIfPresent(prefix, (k, v) -> {
                v.complete(bean);
                return v;
            });
        }
        return bean;
    }
}
