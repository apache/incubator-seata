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

import io.seata.common.util.ReflectionUtil;
import io.seata.spring.boot.autoconfigure.StarterConstants;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.support.RegisteredBean;

/**
 * The seata properties bean registration AOT processor
 *
 * @author wang.liang
 */
class SeataPropertiesBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

    @Override
    public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
        if (ReflectionUtil.existsClass("io.seata.spring.boot.autoconfigure.StarterConstants")) {
            Class<?> beanClass = registeredBean.getBeanClass();
            if (StarterConstants.PROPERTY_BEAN_MAP.containsValue(beanClass)) {
                return new SeataPropertiesBeanRegistrationAotContribution(beanClass);
            }
        }
        return null;
    }


    /**
     * The seata properties bean registration AOT contribution
     */
    private static class SeataPropertiesBeanRegistrationAotContribution implements BeanRegistrationAotContribution {

        private final Class<?> propertiesBeanClass;


        public SeataPropertiesBeanRegistrationAotContribution(Class<?> propertiesBeanClass) {
            this.propertiesBeanClass = propertiesBeanClass;
        }


        @Override
        public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
            ReflectionHints reflectionHints = generationContext.getRuntimeHints().reflection();

            // register the properties bean class
            // See SpringBootConfigurationProvider#getDefaultValueFromPropertyObject(...)
            AotUtils.registerType(reflectionHints, propertiesBeanClass, MemberCategory.DECLARED_FIELDS);
        }

    }

}
