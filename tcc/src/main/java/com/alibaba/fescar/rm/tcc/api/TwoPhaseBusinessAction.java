package com.alibaba.fescar.rm.tcc.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TCC annotation, Define a TCC interface，which added on the try method
 * 
 * @author zhangsen
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
@Inherited
public @interface TwoPhaseBusinessAction {

    /**
     * TCC bean name, must be unique
     */
    String name() ;

    /**
     *  commit methed name
     */
    String commitMethod() default "commit";

    /**
     * rollback method name
     */
    String rollbackMethod() default "rollback";

}