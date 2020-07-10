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
package io.seata.spring.boot.autoconfigure.provider;

import io.seata.common.holder.ObjectHolder;
import io.seata.common.util.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import static io.seata.common.Constants.OBJECT_KEY_SPRING_APPLICATION_CONTEXT;
import static io.seata.spring.boot.autoconfigure.StarterConstants.PROPERTY_BEAN_MAP;

/**
 * The type spring application context provider
 *
 * @author xingfudeshi@gmail.com
 */
public class SpringApplicationContextProvider implements ApplicationContextAware, BeanPostProcessor {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT, applicationContext);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean.getClass().getName().startsWith("io.seata.spring.boot.autoconfigure.properties")) {
            String propertiesPrefix = getPropertiesPrefix(bean);
            if (propertiesPrefix != null) {
                PROPERTY_BEAN_MAP.put(propertiesPrefix, bean);
            }
        }
        return bean;
    }

    private String getPropertiesPrefix(Object bean) {
        Class<?> clazz = bean.getClass();
        ConfigurationProperties annotation = clazz.getAnnotation(ConfigurationProperties.class);
        if (annotation != null) {
            if (StringUtils.isNotBlank(annotation.value())) {
                return annotation.value();
            } else if (StringUtils.isNotBlank(annotation.prefix())) {
                return annotation.prefix();
            }
        }
        return null;
    }
}
