package com.alibaba.fescar.common.loader;

/**
 * @author melon.zhao
 * @since 2019/2/26
 */
@LoadLevel(name = "ChineseHello",order = Integer.MIN_VALUE)
public class ChineseHello implements Hello{

    @Override
    public String say() {
        return "你好!";
    }
}
