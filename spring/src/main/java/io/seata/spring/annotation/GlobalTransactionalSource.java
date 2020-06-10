package io.seata.spring.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * @author chusen
 * @email chusen12@163.com
 */
public interface GlobalTransactionalSource {


    /**
     * decide exist the transaction annotation on the bean
     *
     * @param bean
     * @return
     * @throws Exception
     */
    boolean existGlobalTransactionalAnnotation(Object bean) throws Exception;


    /**
     * get global transaction by method and target class
     *
     * @param method
     * @param targetClass
     * @return
     */
    Annotation getGlobalTransactionalAnnotation(Method method, Class<?> targetClass);

}
