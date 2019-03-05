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
 * 解析远程通信协议
 * @author zhangsen
 */
public class DefaultRemotingParser {

    /**
     * 所有的远程通信协议解析
     */
    protected static List<RemotingParser> allRemotingParsers = new ArrayList<RemotingParser>();

    /**
     * 本地所有 TCC 资源, 注册至服务端
     */
    protected static Set<TCCResource> allLocalTccResources = new HashSet<TCCResource>();

    /**
     * 服务订阅bean 信息
     * beanName -> RemotingDesc
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
     * 是否是远程通信bean
     * @param bean
     * @param beanName
     * @return
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
     * 是否是服务 订阅 bean
     * @param bean
     * @param beanName
     * @return
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
     * 是否是服务发布bean
     * @param bean
     * @param beanName
     * @return
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
     * 获取 remoting Service desc
     * @param bean
     * @param beanName
     * @return
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
     * 解析远程通信bean 信息
     * @param bean
     * @param beanName
     * @return  是否生成动态代理
     * @throws NoSuchMethodException
     */
    public RemotingDesc parserRemotingServiceInfo(Object bean, String beanName)  {
        //remoting bean 信息
        RemotingDesc remotingBeanDesc = getServiceDesc(bean, beanName);
        remotingServiceMap.put(beanName, remotingBeanDesc);

        Class<?> interfaceClass = remotingBeanDesc.getInterfaceClass();
        Method[] methods = interfaceClass.getMethods();
        if(isService(bean, beanName)){
            try{
                //服务发布bean， 注册资源
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
                        //注册TCC 资源
                        DefaultResourceManager.get().registerResource(tccResource);
                        allLocalTccResources.add(tccResource);
                    }
                }
            }catch (Throwable t){
                throw new FrameworkException(t, "parser remting service error");
            }
        }else if(isReference(bean, beanName)){
            //服务订阅bean， 生成动态代理
            remotingBeanDesc.setReference(true);
        }
        return remotingBeanDesc;
    }

    /**
     * 获取远程服务bean信息
     * @param beanName
     * @return
     */
    public RemotingDesc getRemotingBeanDesc(String beanName){
        return remotingServiceMap.get(beanName);
    }

}
