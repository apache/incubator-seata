package com.alibaba.fescar.common.loader;

/**
 * @author melon.zhao
 * @since 2019/2/26
 */
@LoadLevel(name = "EnglishHello",order = 1)
public class EnglishHello implements Hello {

    @Override
    public String say() {
        return "hello!";
    }
}
