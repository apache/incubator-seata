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

    /**
     * The constant MAX_NEST_DEPTH.
     */
    public static int MAX_NEST_DEPTH = 20;

    /**
     * Gets class by name.
     *
     * @param className the class name
     * @return the class by name
     * @throws ClassNotFoundException the class not found exception
     */
    public static Class<?> getClassByName(String className) throws ClassNotFoundException{
        return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
    }

    /**
     * get Field Value
     *
     * @param target the target
     * @param fieldName the field name
     * @return field value
     * @throws NoSuchFieldException the no such field exception
     * @throws SecurityException the security exception
     * @throws IllegalArgumentException the illegal argument exception
     * @throws IllegalAccessException the illegal access exception
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
     *
     * @param target the target
     * @param methodName the method name
     * @return object
     * @throws NoSuchMethodException the no such method exception
     * @throws SecurityException the security exception
     * @throws IllegalAccessException the illegal access exception
     * @throws IllegalArgumentException the illegal argument exception
     * @throws InvocationTargetException the invocation target exception
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
     *
     * @param target the target
     * @param methodName the method name
     * @param parameterTypes the parameter types
     * @param args the args
     * @return object
     * @throws NoSuchMethodException the no such method exception
     * @throws SecurityException the security exception
     * @throws IllegalAccessException the illegal access exception
     * @throws IllegalArgumentException the illegal argument exception
     * @throws InvocationTargetException the invocation target exception
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
     * @param targetClass the target class
     * @param methodName the method name
     * @param parameterTypes the parameter types
     * @param parameterValues the parameter values
     * @return object
     * @throws NoSuchMethodException the no such method exception
     * @throws SecurityException the security exception
     * @throws IllegalAccessException the illegal access exception
     * @throws IllegalArgumentException the illegal argument exception
     * @throws InvocationTargetException the invocation target exception
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
     *
     * @param classType the class type
     * @param methodName the method name
     * @param parameterTypes the parameter types
     * @return method
     * @throws NoSuchMethodException the no such method exception
     * @throws SecurityException the security exception
     */
    public static Method getMethod(Class<?> classType, String methodName, Class<?>[] parameterTypes) throws NoSuchMethodException, SecurityException{
        return classType.getMethod(methodName, parameterTypes);
    }

    /**
     * get all interface of the clazz
     *
     * @param clazz the clazz
     * @return set
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
