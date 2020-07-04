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
package io.seata.spring.annotation;

import org.springframework.beans.factory.config.BeanDefinition;

/**
 * The ScannerExcluder for GlobalTransactionScanner
 *
 * @author wang.liang
 */
public interface ScannerExcluder {

    /**
     * Do matches need to be excluded
     *
     * @param bean           the bean
     * @param beanName       the bean name
     * @param beanDefinition the bean definition
     * @return the boolean: true=exclude|false=not exclude
     */
    boolean isMatch(Object bean, String beanName, BeanDefinition beanDefinition) throws Throwable;
}
