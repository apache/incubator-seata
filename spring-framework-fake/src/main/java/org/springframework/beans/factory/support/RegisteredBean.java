package org.springframework.beans.factory.support;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

import java.lang.reflect.Executable;
import java.util.function.Supplier;

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
