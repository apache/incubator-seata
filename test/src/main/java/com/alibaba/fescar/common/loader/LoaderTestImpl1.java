package com.alibaba.fescar.common.loader;

/**
 * @author zhangsen
 */
@LoadLevel(name = "one", order = 1)
public class LoaderTestImpl1 implements LoaderTestSPI {
    @Override
    public String echo() {
        return "impl_1";
    }
}
