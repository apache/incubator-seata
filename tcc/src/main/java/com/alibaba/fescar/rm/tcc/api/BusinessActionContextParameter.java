package com.alibaba.fescar.rm.tcc.api;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * the TCC parameters that need to be passed to  the BusinessActivityContext；
 *
 * add this annotation on the parameters of the try method, and the parameters will be passed to  the BusinessActivityContext；
 * 
 * @author zhangsen
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.FIELD })
public @interface BusinessActionContextParameter {
	
    /**
     * parameter's name
     */
    String paramName() default "";

    /**
     * if it is a sharding param ?
     *
     * @return
     */
    boolean isShardingParam() default false;

    /**
     * Specify the index of the parameter in the List
     * @return
     */
    int index() default -1;

    /**
     * if get the parameter from the property of the object ?
     *
     * @return
     */
    boolean isParamInProperty() default false;
}