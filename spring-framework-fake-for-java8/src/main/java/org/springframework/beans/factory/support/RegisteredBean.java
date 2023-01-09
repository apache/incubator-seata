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
package org.springframework.beans.factory.support;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

import java.lang.reflect.Executable;
import java.util.function.Supplier;

/**
 * RegisteredBean's fake
 *
 * @author wang.liang
 */
public final class RegisteredBean {

    private RegisteredBean(ConfigurableListableBeanFactory beanFactory, Supplier<String> beanName,
                           boolean generatedBeanName, Supplier<RootBeanDefinition> mergedBeanDefinition,
                           @Nullable RegisteredBean parent) {
    }


    public static RegisteredBean of(ConfigurableListableBeanFactory beanFactory, String beanName) {
        return null;
    }

    public static RegisteredBean ofInnerBean(RegisteredBean parent, BeanDefinitionHolder innerBean) {
        return null;
    }

    public static RegisteredBean ofInnerBean(RegisteredBean parent, BeanDefinition innerBeanDefinition) {
        return null;
    }

    public static RegisteredBean ofInnerBean(RegisteredBean parent,
                                             @Nullable String innerBeanName, BeanDefinition innerBeanDefinition) {
        return null;
    }


    public String getBeanName() {
        return null;
    }

    public boolean isGeneratedBeanName() {
        return true;
    }

    public ConfigurableListableBeanFactory getBeanFactory() {
        return null;
    }

    public Class<?> getBeanClass() {
        return null;
    }

    public ResolvableType getBeanType() {
        return null;
    }

    public RootBeanDefinition getMergedBeanDefinition() {
        return null;
    }

    public boolean isInnerBean() {
        return true;
    }

    @Nullable
    public RegisteredBean getParent() {
        return null;
    }

    public Executable resolveConstructorOrFactoryMethod() {
        return null;
    }

}
