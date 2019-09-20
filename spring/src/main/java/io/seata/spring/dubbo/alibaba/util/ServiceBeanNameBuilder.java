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
package io.seata.spring.dubbo.alibaba.util;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;


public class ServiceBeanNameBuilder {
    
    private static final String SEPARATOR = ":";
    
    private final String        interfaceClassName;
    
    private final Environment   environment;
    
    // Optional
    private String              version;
    
    private String              group;
    
    private ServiceBeanNameBuilder(String interfaceClassName, Environment environment) {
        this.interfaceClassName = interfaceClassName;
        this.environment = environment;
    }
    
    private ServiceBeanNameBuilder(Class<?> interfaceClass, Environment environment) {
        this(interfaceClass.getName(), environment);
    }
    
    private ServiceBeanNameBuilder(Service service, Class<?> interfaceClass, Environment environment) {
        this(AnnotationUtils.resolveInterfaceName(service, interfaceClass), environment);
        this.group(service.group());
        this.version(service.version());
    }
    
    private ServiceBeanNameBuilder(Reference reference, Class<?> interfaceClass, Environment environment) {
        this(AnnotationUtils.resolveInterfaceName(reference, interfaceClass), environment);
        this.group(reference.group());
        this.version(reference.version());
    }
    
    public static ServiceBeanNameBuilder create(Class<?> interfaceClass, Environment environment) {
        return new ServiceBeanNameBuilder(interfaceClass, environment);
    }
    
    public static ServiceBeanNameBuilder create(Service service, Class<?> interfaceClass, Environment environment) {
        return new ServiceBeanNameBuilder(service, interfaceClass, environment);
    }
    
    public static ServiceBeanNameBuilder create(Reference reference, Class<?> interfaceClass, Environment environment) {
        return new ServiceBeanNameBuilder(reference, interfaceClass, environment);
    }
    
    private static void append(StringBuilder builder, String value) {
        if (StringUtils.hasText(value)) {
            builder.append(SEPARATOR).append(value);
        }
    }
    
    public ServiceBeanNameBuilder group(String group) {
        this.group = group;
        return this;
    }
    
    public ServiceBeanNameBuilder version(String version) {
        this.version = version;
        return this;
    }
    
    public String build() {
        StringBuilder beanNameBuilder = new StringBuilder("ServiceBean");
        // Required
        append(beanNameBuilder, interfaceClassName);
        // Optional
        append(beanNameBuilder, version);
        append(beanNameBuilder, group);
        // Build
        String rawBeanName = beanNameBuilder.toString();
        // Resolve placeholders
        return environment.resolvePlaceholders(rawBeanName);
    }
}
