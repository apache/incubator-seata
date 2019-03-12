/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.rm.tcc.remoting;

import com.alibaba.fescar.common.exception.FrameworkException;

import java.lang.reflect.InvocationTargetException;

/**
 * extract remoting bean info
 *
 * @author zhangsen
 */
public interface RemotingParser {

    /**
     * if it is remoting bean ?
     *
     * @param bean the bean
     * @param beanName the bean name
     * @return boolean boolean
     * @throws FrameworkException the framework exception
     */
    public boolean isRemoting(Object bean, String beanName) throws FrameworkException;

    /**
     * if it is reference bean ?
     *
     * @param bean the bean
     * @param beanName the bean name
     * @return boolean boolean
     * @throws FrameworkException the framework exception
     */
    public boolean isReference(Object bean, String beanName) throws FrameworkException;

    /**
     * if it is service bean ?
     *
     * @param bean the bean
     * @param beanName the bean name
     * @return boolean boolean
     * @throws FrameworkException the framework exception
     */
    public boolean isService(Object bean, String beanName) throws FrameworkException;

    /**
     * get the remoting bean info
     *
     * @param bean the bean
     * @param beanName the bean name
     * @return service desc
     * @throws FrameworkException the framework exception
     */
    public RemotingDesc getServiceDesc(Object bean, String beanName) throws FrameworkException;

    /**
     * the remoting protocol
     *
     * @return protocol protocol
     */
    public Protocols getProtocol();


}
