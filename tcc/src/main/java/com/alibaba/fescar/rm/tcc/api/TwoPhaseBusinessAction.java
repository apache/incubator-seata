package com.alibaba.fescar.rm.tcc.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 两阶段原子业务活动标记注解；此注解添加至TCC服务的一阶段try方法上
 * 
 * @author zhangsen
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
@Inherited
public @interface TwoPhaseBusinessAction {

    /**
     * 原子业务活动名称；全局唯一；
     */
    String name() ;

    /**
     *  提交方法名称
     */
    String commitMethod() default "commit";

    /**
     * 回滚方法名称
     */
    String rollbackMethod() default "rollback";

}