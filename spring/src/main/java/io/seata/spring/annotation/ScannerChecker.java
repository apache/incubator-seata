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

import javax.annotation.Nullable;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * The Scanner checker for {@link GlobalTransactionScanner}
 *
 * @author wang.liang
 * @see GlobalTransactionScanner#wrapIfNecessary(Object, String, Object)
 */
public interface ScannerChecker {

    /**
     * Do check
     *
     * @param bean        the bean
     * @param beanName    the bean name
     * @param beanFactory the bean factory
     * @return the boolean: true=need scan | false=do not scan
     * @throws Exception the exception
     */
    boolean check(Object bean, String beanName, @Nullable ConfigurableListableBeanFactory beanFactory) throws Exception;
}
