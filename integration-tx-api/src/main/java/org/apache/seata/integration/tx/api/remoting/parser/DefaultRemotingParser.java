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
package org.apache.seata.integration.tx.api.remoting.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.integration.tx.api.remoting.RemotingDesc;
import org.apache.seata.integration.tx.api.remoting.RemotingParser;

/**
 * parsing remoting bean
 *
 */
public class DefaultRemotingParser {

    /**
     * all remoting bean parser
     */
    protected static List<RemotingParser> allRemotingParsers = new ArrayList<>();

    /**
     * all remoting beans beanName -> RemotingDesc
     */
    protected static Map<Object, RemotingDesc> remotingServiceMap = new ConcurrentHashMap<>();

    private static class SingletonHolder {
        private static final DefaultRemotingParser INSTANCE = new DefaultRemotingParser();
    }

    /**
     * Get resource manager.
     *
     * @return the resource manager
     */
    public static DefaultRemotingParser get() {
        return DefaultRemotingParser.SingletonHolder.INSTANCE;
    }

    /**
     * Instantiates a new Default remoting parser.
     */
    protected DefaultRemotingParser() {
        initRemotingParser();
    }

    /**
     * init parsers
     */
    protected void initRemotingParser() {
        //init all resource managers
        List<RemotingParser> remotingParsers = EnhancedServiceLoader.loadAll(RemotingParser.class);
        if (CollectionUtils.isNotEmpty(remotingParsers)) {
            allRemotingParsers.addAll(remotingParsers);
        }
    }

    /**
     * register custom remoting parser
     * @param remotingParser
     */
    public boolean registerRemotingParser(RemotingParser remotingParser) {
        synchronized (this) {
            return allRemotingParsers.add(remotingParser);
        }
    }

    /**
     * is remoting bean ?
     *
     * @param bean     the bean
     * @param beanName the bean name
     * @return boolean boolean
     */
    public RemotingParser isRemoting(Object bean, String beanName) {
        for (RemotingParser remotingParser : allRemotingParsers) {
            if (remotingParser.isRemoting(bean, beanName)) {
                return remotingParser;
            }
        }
        return null;
    }

    /**
     * is reference bean?
     *
     * @param bean     the bean
     * @param beanName the bean name
     * @return boolean boolean
     */
    public boolean isReference(Object bean, String beanName) {
        for (RemotingParser remotingParser : allRemotingParsers) {
            if (remotingParser.isReference(bean, beanName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * is service bean ?
     *
     * @param bean     the bean
     * @param beanName the bean name
     * @return boolean boolean
     */
    public boolean isService(Object bean, String beanName) {
        for (RemotingParser remotingParser : allRemotingParsers) {
            if (remotingParser.isService(bean, beanName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * is service bean ?
     *
     * @param beanClass the bean class
     * @return boolean boolean
     */
    public boolean isService(Class<?> beanClass) {
        for (RemotingParser remotingParser : allRemotingParsers) {
            if (remotingParser.isService(beanClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * get the remoting Service desc
     *
     * @param bean     the bean
     * @param beanName the bean name
     * @return service desc
     */
    public RemotingDesc getServiceDesc(Object bean, String beanName) {
        List<RemotingDesc> ret = new ArrayList<>();
        for (RemotingParser remotingParser : allRemotingParsers) {
            RemotingDesc s = remotingParser.getServiceDesc(bean, beanName);
            if (s != null) {
                ret.add(s);
            }
        }
        if (ret.size() == 1) {
            return ret.get(0);
        } else if (ret.size() > 1) {
            throw new FrameworkException(String.format("More than one RemotingParser for bean: %s", beanName));
        } else {
            return null;
        }
    }

    /**
     * parse the remoting bean info
     *
     * @param bean           the bean
     * @param beanName       the bean name
     * @param remotingParser the remoting parser
     * @return remoting desc
     */
    public RemotingDesc parserRemotingServiceInfo(Object bean, String beanName, RemotingParser remotingParser) {
        if (remotingServiceMap.containsKey(bean)) {
            return remotingServiceMap.get(bean);
        }
        RemotingDesc remotingBeanDesc = remotingParser.getServiceDesc(bean, beanName);
        if (remotingBeanDesc == null) {
            return null;
        }
        remotingServiceMap.put(bean, remotingBeanDesc);
        if (remotingParser.isReference(bean, beanName)) {
            //reference bean, TCC proxy
            remotingBeanDesc.setReference(true);
        }
        return remotingBeanDesc;
    }

    /**
     * Get remoting bean desc remoting desc.
     *
     * @param bean the bean
     * @return the remoting desc
     */
    public RemotingDesc getRemotingBeanDesc(Object bean) {
        return remotingServiceMap.get(bean);
    }

}
