package com.alibaba.fescar.rm.tcc.api;

import java.lang.annotation.*;

/**
 * TCC bean是本地bean，非远程服务bean
 * @author zhangsen
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface LocalTCC {
}
