package com.alibaba.fescar.springcloud.anno;

import com.alibaba.fescar.spring.annotation.GlobalTransactionScanner;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(GlobalTransactionScannerRegistrar.class)
public @interface EnableGlobalTransaction {

    String applicationId() default "";

    String txServiceGroup();

    int mode() default GlobalTransactionScanner.DEFAULT_MODE;

    Class<?> failureHandler() default void.class;
}
