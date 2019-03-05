package com.alibaba.fescar.rm.tcc.remoting;

import com.alibaba.fescar.common.exception.FrameworkException;

import java.lang.reflect.InvocationTargetException;

/**
 * 远程通信协议解析
 *
 * @author zhangsen
 */
public interface RemotingParser {

    /**
     * 是否是 rpc 服务bean； 发布者或者订阅者
     * @param bean
     * @param beanName
     * @return
     * @throws ClassNotFoundException
     */
    public boolean isRemoting(Object bean, String beanName) throws FrameworkException;

    /**
     * 是否是服务 订阅 bean
     * @param bean
     * @param beanName
     * @return
     * @throws ClassNotFoundException
     */
    public boolean isReference(Object bean, String beanName) throws FrameworkException;


    /**
     * 是否是服务发布bean
     * @param bean
     * @param beanName
     * @return
     */
    public boolean isService(Object bean, String beanName) throws FrameworkException;

    /**
     * remoting bean 描述信息
     * @param bean
     * @param beanName
     * @return
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws NoSuchFieldException
     * @throws ClassNotFoundException
     */
    public RemotingDesc getServiceDesc(Object bean, String beanName) throws FrameworkException;


    /**
     * 获取 当前解析器对应的协议
     * @return
     */
    public Protocols getProtocol();


}
