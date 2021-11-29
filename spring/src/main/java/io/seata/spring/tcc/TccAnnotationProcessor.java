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
package io.seata.spring.tcc;

import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import io.seata.rm.tcc.remoting.RemotingDesc;
import io.seata.spring.util.TCCBeanParserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An annotation adapter for TCC
 *
 * @author ppf
 */
public class TccAnnotationProcessor implements BeanPostProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TccAnnotationProcessor.class);

    private static final List<Class<? extends Annotation>> ANNOTATIONS = new ArrayList<>(4);
    private static final Set<String> PROXIED_SET = new HashSet<>();

    static {
        ANNOTATIONS.add(loadAnnotation("org.apache.dubbo.config.annotation.Reference"));
        ANNOTATIONS.add(loadAnnotation("com.alipay.sofa.runtime.api.annotation.SofaReference"));
    }

    private static Class<? extends Annotation> loadAnnotation(String annotation) {
        try {
            return (Class<? extends Annotation>) Class.forName(annotation);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Process annotation
     *
     * @param bean       the bean
     * @param beanName   the bean name
     * @param annotation the annotation
     */
    protected void process(Object bean, String beanName, Class<? extends Annotation> annotation) {
        if (Objects.isNull(annotation) || PROXIED_SET.contains(beanName)) {
            return;
        }

        ReflectionUtils.doWithFields(bean.getClass(), field -> {
            Annotation reference = field.getAnnotation(annotation);
            if (reference == null) {
                return;
            }

            addTccAdvise(bean, beanName, field, field.getType());

        }, field -> !Modifier.isStatic(field.getModifiers())
                && (field.isAnnotationPresent(annotation)));

        PROXIED_SET.add(beanName);
    }

    /**
     * Add TCC interceptor for tcc proxy bean
     *
     * @param bean           the bean
     * @param beanName       the bean name
     * @param field          the field
     * @param interfaceClass the interface class
     * @throws IllegalAccessException the illegal access exception
     */
    public void addTccAdvise(Object bean, String beanName, Field field, Class interfaceClass) throws IllegalAccessException {
        Object fieldValue = field.get(bean);
        if (fieldValue == null) {
            return;
        }
        for (Method method : field.getType().getMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) && (method.isAnnotationPresent(TwoPhaseBusinessAction.class))) {
                RemotingDesc remotingDesc = new RemotingDesc();
                remotingDesc.setInterfaceClass(interfaceClass);

                TccActionInterceptor actionInterceptor = new TccActionInterceptor(remotingDesc);
                Object proxyBean = TCCBeanParserUtils.createProxy(interfaceClass, fieldValue, actionInterceptor);
                field.setAccessible(true);
                field.set(bean, proxyBean);
                LOGGER.info("Bean[" + bean.getClass().getName() + "] with name [" + field.getName() + "] would use proxy [" + actionInterceptor.getClass().getName() + "]");
            }
        }
    }


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        for (Class<? extends Annotation> annotation : ANNOTATIONS) {
            process(bean, beanName, annotation);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

}

