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
package io.seata.spring.aot;

import java.util.Set;

import io.seata.common.util.ReflectionUtil;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.spring.annotation.GlobalTransactionScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.support.RegisteredBean;

import static io.seata.spring.aot.AotUtils.ALL_MEMBER_CATEGORIES;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;

/**
 * The seata-client bean registration AOT processor
 *
 * @author wang.liang
 */
class SeataLocalTCCBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeataLocalTCCBeanRegistrationAotProcessor.class);

    @Override
    public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
        Class<?> beanClass = registeredBean.getBeanClass();
        if (GlobalTransactionScanner.isTccAutoProxy(beanClass)) {
            return new SeataTccBeanRegistrationAotContribution(beanClass);
        }
        return null;
    }


    /**
     * The seata tcc bean registration AOT contribution
     */
    private static class SeataTccBeanRegistrationAotContribution implements BeanRegistrationAotContribution {

        private final Class<?> beanClass;


        public SeataTccBeanRegistrationAotContribution(Class<?> beanClass) {
            this.beanClass = beanClass;
        }


        @Override
        public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
            ReflectionHints reflectionHints = generationContext.getRuntimeHints().reflection();

            // register the bean class
            registerClassAndItsInterfacesIfLocalTcc(reflectionHints, beanClass);

            // register the interface classes
            Set<Class<?>> interfaceClasses = ReflectionUtil.getInterfaces(beanClass);
            for (Class<?> interClass : interfaceClasses) {
                registerClassAndItsInterfacesIfLocalTcc(reflectionHints, interClass);
            }
        }

        private void registerClassAndItsInterfacesIfLocalTcc(ReflectionHints reflectionHints, Class<?> clazz) {
            if (clazz.isAnnotationPresent(LocalTCC.class)) {
                reflectionHints.registerType(clazz, INVOKE_PUBLIC_METHODS);
                LOGGER.info("TCC: Register reflection type '{}' (annotated `@LocalTCC`) with member categories {}", clazz.getName(), INVOKE_PUBLIC_METHODS);
                AotUtils.registerAllOfClass(false, reflectionHints, clazz, ALL_MEMBER_CATEGORIES);
            }
        }
    }
}
