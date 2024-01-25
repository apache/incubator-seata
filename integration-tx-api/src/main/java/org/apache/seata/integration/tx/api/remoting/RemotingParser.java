/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.integration.tx.api.remoting;

import org.apache.seata.common.exception.FrameworkException;

/**
 * extract remoting bean info
 *
 */
public interface RemotingParser {

    /**
     * if it is remoting bean ?
     *
     * @param bean     the bean
     * @param beanName the bean name
     * @return boolean boolean
     * @throws FrameworkException the framework exception
     */
    boolean isRemoting(Object bean, String beanName) throws FrameworkException;

    /**
     * if it is reference bean ?
     *
     * @param bean     the bean
     * @param beanName the bean name
     * @return boolean boolean
     * @throws FrameworkException the framework exception
     */
    boolean isReference(Object bean, String beanName) throws FrameworkException;

    /**
     * if it is service bean ?
     *
     * @param bean     the bean
     * @param beanName the bean name
     * @return boolean boolean
     * @throws FrameworkException the framework exception
     */
    boolean isService(Object bean, String beanName) throws FrameworkException;

    /**
     * if it is service bean ?
     *
     * @param beanClass the bean class
     * @return boolean boolean
     * @throws FrameworkException the framework exception
     */
    boolean isService(Class<?> beanClass) throws FrameworkException;

    /**
     * get the remoting bean info
     *
     * @param bean     the bean
     * @param beanName the bean name
     * @return service desc
     * @throws FrameworkException the framework exception
     */
    RemotingDesc getServiceDesc(Object bean, String beanName) throws FrameworkException;

    /**
     * the remoting protocol
     *
     * @return protocol
     */
    short getProtocol();


}
