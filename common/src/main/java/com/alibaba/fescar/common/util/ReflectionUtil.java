package com.alibaba.fescar.common.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Reflection tools
 *
 * @author zhangsen
 */
public class ReflectionUtil {

    public static int MAX_NEST_DEPTH = 20;

    public static Class<?> getClassByName(String className) throws ClassNotFoundException{
        return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
    }

    /**
     * get Field Value
     * @param target
     * @param fieldName
     * @return
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public static Object getFieldValue (Object target, String fieldName) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Class<?> cl = target.getClass();
        int i = 0;
        while((i++) < MAX_NEST_DEPTH && cl != null){
            try{
                Field field = cl.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            }catch(Exception e){
                cl = cl.getSuperclass();
            }
        }
        throw new NoSuchFieldException("class:"+target.getClass() + ", field:" + fieldName);
    }

    /**
     * invoke Method
     * @param target
     * @param methodName
     * @return
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public static Object invokeMethod(Object target, String methodName) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class<?> cl = target.getClass();
        int i = 0;
        while((i++) < MAX_NEST_DEPTH && cl != null){
            try{
                Method m = cl.getDeclaredMethod(methodName);
                m.setAccessible(true);
                return m.invoke(target);
            }catch(Exception e){
                cl = cl.getSuperclass();
            }
        }
        throw new NoSuchMethodException("class:"+target.getClass() + ", methodName:" + methodName);
    }

    /**
     * invoke Method
     * @param target
     * @param methodName
     * @param parameterTypes
     * @param args
     * @return
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public static Object invokeMethod(Object target, String methodName, Class<?>[] parameterTypes, Object[] args) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        Class<?> cl = target.getClass();
        int i = 0;
        while((i++) < MAX_NEST_DEPTH && cl != null){
            try{
                Method m = cl.getDeclaredMethod(methodName, parameterTypes);
                m.setAccessible(true);
                return m.invoke(target, args);
            }catch(Exception e){
                cl = cl.getSuperclass();
            }
        }
        throw new NoSuchMethodException("class:"+target.getClass() + ", methodName:" + methodName);
    }

    /**
     * invoke static Method
     *
     * @param targetClass
     * @param methodName
     * @param parameterTypes
     * @param parameterValues
     * @return
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public static Object invokeStaticMethod(Class<?> targetClass, String methodName, Class<?>[] parameterTypes , Object[] parameterValues) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        int i = 0;
        while((i++) < MAX_NEST_DEPTH && targetClass != null){
            try{
                Method m = targetClass.getMethod(methodName, parameterTypes);
                return m.invoke(null, parameterValues);
            }catch(Exception e){
                targetClass = targetClass.getSuperclass();
            }
        }
        throw new NoSuchMethodException("class:"+targetClass + ", methodName:" + methodName);
    }

    /**
     * get Method by name
     * @param classType
     * @param methodName
     * @param parameterTypes
     * @return
     * @throws NoSuchMethodException
     * @throws SecurityException
     */
    public static Method getMethod(Class<?> classType, String methodName, Class<?>[] parameterTypes) throws NoSuchMethodException, SecurityException{
        return classType.getMethod(methodName, parameterTypes);
    }

    /**
     * get all interface of the clazz
     * @param clazz
     * @return
     */
    public static Set<Class<?>> getInterfaces(Class<?> clazz){
        if (clazz.isInterface() ) {
            return Collections.<Class<?>>singleton(clazz);
        }
        Set<Class<?>> interfaces = new LinkedHashSet<Class<?>>();
        while (clazz != null) {
            Class<?>[] ifcs = clazz.getInterfaces();
            for (Class<?> ifc : ifcs) {
                interfaces.addAll(getInterfaces(ifc));
            }
            clazz = clazz.getSuperclass();
        }
        return interfaces;
    }

}
