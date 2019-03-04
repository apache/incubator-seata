package com.alibaba.fescar.rm.tcc.api;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记TCC服务需要透传的参数；
 * 一阶段try方法上的参数如果要透传到二阶段，需要添加此注解
 * 
 * @author zhangsen
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.FIELD })
public @interface BusinessActionContextParameter {
	
    /**
     * 要传递的参数名称，作为context的key，value是传入的参数值
     */
    String paramName() default "";

    /**
     * 该参数是否用来做分库用
     *
     * @return
     */
    boolean isShardingParam() default false;

    /**
     * 从某一个list参数中指定第几个参数从获取最终的参数
     * 用在需要获取list参数中某一个对象的某一个属性的特殊场景
     * @return
     */
    int index() default -1;

    /**
     * 如果业务需要透传的参数隐藏在业务对象的某一个属性，则需要指定这个属性，xts会自动遍历该对象所以属性，并找到打了BusinessActionContextParameter的那些属性
     *
     * @return
     */
    boolean isParamInProperty() default false;
}