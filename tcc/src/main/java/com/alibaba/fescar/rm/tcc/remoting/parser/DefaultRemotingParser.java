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
package com.alibaba.fescar.rm.tcc.remoting.parser;

import com.alibaba.fescar.common.exception.FrameworkException;
import com.alibaba.fescar.common.loader.EnhancedServiceLoader;
import com.alibaba.fescar.common.util.CollectionUtils;
import com.alibaba.fescar.common.util.ReflectionUtil;
import com.alibaba.fescar.rm.DefaultResourceManager;
import com.alibaba.fescar.rm.tcc.TCCResource;
import com.alibaba.fescar.rm.tcc.api.BusinessActionContext;
import com.alibaba.fescar.rm.tcc.api.TwoPhaseBusinessAction;
import com.alibaba.fescar.rm.tcc.remoting.RemotingDesc;
import com.alibaba.fescar.rm.tcc.remoting.RemotingParser;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * parsing remoting bean
 *
 * @author zhangsen
 */
public class DefaultRemotingParser {

    /**
     * all remoting bean parser
     */
    protected static List<RemotingParser> allRemotingParsers = new ArrayList<RemotingParser>();

    /**
     * all remoting beans beanName -> RemotingDesc
     */
    protected static Map<String, RemotingDesc> remotingServiceMap = new ConcurrentHashMap<String,RemotingDesc>();


    /**
     * 单实例
     */
    private static class SingletonHolder {
        private static DefaultRemotingParser INSTANCE = new DefaultRemotingParser();
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
    protected DefaultRemotingParser(){
        initRemotingParser();
    }

    /**
     * init parsers
     */
    protected void initRemotingParser() {
        //init all resource managers
        List<RemotingParser> remotingParsers = EnhancedServiceLoader.loadAll(RemotingParser.class);
        if(CollectionUtils.isNotEmpty(remotingParsers)){
            for(RemotingParser rp : remotingParsers){
                allRemotingParsers.add(rp);
            }
        }
    }

    /**
     * is remoting bean ?
     *
     * @param bean the bean
     * @param beanName the bean name
     * @return boolean
     */
    public boolean isRemoting(Object bean, String beanName) {
        for(RemotingParser remotingParser : allRemotingParsers){
            if(remotingParser.isRemoting(bean, beanName)){
                return true;
            }
        }
        return false;
    }

    /**
     * is reference bean?
     *
     * @param bean the bean
     * @param beanName the bean name
     * @return boolean
     */
    public boolean isReference(Object bean, String beanName) {
        for(RemotingParser remotingParser : allRemotingParsers){
            if(remotingParser.isReference(bean, beanName)){
                return true;
            }
        }
        return false;
    }

    /**
     * is service bean ?
     *
     * @param bean the bean
     * @param beanName the bean name
     * @return boolean
     */
    public boolean isService(Object bean, String beanName) {
        for(RemotingParser remotingParser : allRemotingParsers){
            if(remotingParser.isService(bean, beanName)){
                return true;
            }
        }
        return false;
    }

    /**
     * get the remoting Service desc
     *
     * @param bean the bean
     * @param beanName the bean name
     * @return service desc
     */
    public RemotingDesc getServiceDesc(Object bean, String beanName)  {
        List<RemotingDesc>  ret = new ArrayList<RemotingDesc>();
        for(RemotingParser remotingParser : allRemotingParsers){
            RemotingDesc s = remotingParser.getServiceDesc(bean, beanName);
            if(s != null){
                ret.add(s);
            }
        }
        if(ret.size() == 1){
            return ret.get(0);
        }else if(ret.size() > 1){
            throw new FrameworkException("More than one RemotingParser for bean:" + beanName);
        }else{
            return null;
        }
    }

    /**
     * parse the remoting bean info
     *
     * @param bean the bean
     * @param beanName the bean name
     * @return remoting desc
     */
    public RemotingDesc parserRemotingServiceInfo(Object bean, String beanName)  {
        //remoting bean 信息
        RemotingDesc remotingBeanDesc = getServiceDesc(bean, beanName);
        remotingServiceMap.put(beanName, remotingBeanDesc);

        Class<?> interfaceClass = remotingBeanDesc.getInterfaceClass();
        Method[] methods = interfaceClass.getMethods();
        if(isService(bean, beanName)){
            try{
                //service bean， registry resource
                Object targetBean = remotingBeanDesc.getTargetBean();
                for(Method m : methods){
                    TwoPhaseBusinessAction twoPhaseBusinessAction = m.getAnnotation(TwoPhaseBusinessAction.class);
                    if(twoPhaseBusinessAction != null){
                        //一阶段方法, 提取TCC 服务 信息，注册TCC资源
                        TCCResource tccResource = new TCCResource();
                        tccResource.setActionName(twoPhaseBusinessAction.name());
                        tccResource.setTargetBean(targetBean);
                        tccResource.setPrepareMethod(m);
                        tccResource.setCommitMethodName(twoPhaseBusinessAction.commitMethod());
                        tccResource.setCommitMethod(ReflectionUtil.getMethod(interfaceClass, twoPhaseBusinessAction.commitMethod(), new Class[]{BusinessActionContext.class}));
                        tccResource.setRollbackMethodName(twoPhaseBusinessAction.rollbackMethod());
                        tccResource.setRollbackMethod(ReflectionUtil.getMethod(interfaceClass, twoPhaseBusinessAction.rollbackMethod(), new Class[]{BusinessActionContext.class}));
                        //registry tcc resource
                        DefaultResourceManager.get().registerResource(tccResource);
                    }
                }
            }catch (Throwable t){
                throw new FrameworkException(t, "parser remting service error");
            }
        }
        if(isReference(bean, beanName)){
            //reference bean， TCC proxy
            remotingBeanDesc.setReference(true);
        }
        return remotingBeanDesc;
    }

    /**
     * Get remoting bean desc remoting desc.
     *
     * @param beanName the bean name
     * @return the remoting desc
     */
    public RemotingDesc getRemotingBeanDesc(String beanName){
        return remotingServiceMap.get(beanName);
    }

}
