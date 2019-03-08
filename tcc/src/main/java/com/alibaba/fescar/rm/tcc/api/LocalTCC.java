package com.alibaba.fescar.rm.tcc.api;

import java.lang.annotation.*;

/**
 * Local TCC bean annotation, add on the TCC interface
 *
 * @author zhangsen
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface LocalTCC {
}
