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
     * @param bean
     * @param beanName
     * @return
     * @throws ClassNotFoundException
     */
    public boolean isRemoting(Object bean, String beanName) throws FrameworkException;

    /**
     * if it is reference bean ?
     * @param bean
     * @param beanName
     * @return
     * @throws ClassNotFoundException
     */
    public boolean isReference(Object bean, String beanName) throws FrameworkException;


    /**
     * if it is service bean ?
     * @param bean
     * @param beanName
     * @return
     */
    public boolean isService(Object bean, String beanName) throws FrameworkException;

    /**
     * get the remoting bean info
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
     * the remoting protocol
     * @return
     */
    public Protocols getProtocol();


}
