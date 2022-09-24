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
package io.seata.commonapi.remoting.parser;

import io.seata.common.exception.FrameworkException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;
import io.seata.commonapi.interceptor.ActionContextUtil;
import io.seata.commonapi.remoting.RemotingDesc;
import io.seata.commonapi.remoting.RemotingParser;
import io.seata.commonapi.rm.tcc.api.BusinessActionContext;
import io.seata.commonapi.rm.tcc.api.BusinessActionContextParameter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * parsing remoting bean
 *
 * @author zhangsen
 * @author Yujianfei
 */
public class DefaultRemotingParser {

    /**
     * all remoting bean parser
     */
    protected static List<RemotingParser> allRemotingParsers = new ArrayList<>();

    /**
     * all remoting beans beanName -> RemotingDesc
     */
    protected static Map<String, RemotingDesc> remotingServiceMap = new ConcurrentHashMap<>();

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
        RemotingDesc remotingBeanDesc = remotingParser.getServiceDesc(bean, beanName);
        if (remotingBeanDesc == null) {
            return null;
        }
        remotingServiceMap.put(beanName, remotingBeanDesc);

        if (remotingParser.isReference(bean, beanName)) {
            //reference bean, TCC proxy
            remotingBeanDesc.setReference(true);
        }
        return remotingBeanDesc;
    }

    public String[] getTwoPhaseArgs(Method method, Class<?>[] argsClasses) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        String[] keys = new String[parameterAnnotations.length];
        /*
         * get parameter's key
         * if method's parameter list is like
         * (BusinessActionContext, @BusinessActionContextParameter("a") A a, @BusinessActionContextParameter("b") B b)
         * the keys will be [null, a, b]
         */
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (int j = 0; j < parameterAnnotations[i].length; j++) {
                if (parameterAnnotations[i][j] instanceof BusinessActionContextParameter) {
                    BusinessActionContextParameter param = (BusinessActionContextParameter)parameterAnnotations[i][j];
                    String key = ActionContextUtil.getParamNameFromAnnotation(param);
                    keys[i] = key;
                    break;
                }
            }
            if (keys[i] == null && !(argsClasses[i].equals(BusinessActionContext.class))) {
                throw new IllegalArgumentException("non-BusinessActionContext parameter should use annotation " +
                        "BusinessActionContextParameter");
            }
        }
        return keys;
    }

    /**
     * Get remoting bean desc remoting desc.
     *
     * @param beanName the bean name
     * @return the remoting desc
     */
    public RemotingDesc getRemotingBeanDesc(String beanName) {
        return remotingServiceMap.get(beanName);
    }

}