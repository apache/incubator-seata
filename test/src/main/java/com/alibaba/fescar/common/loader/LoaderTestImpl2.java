package com.alibaba.fescar.common.loader;

/**
 * @author zhangsen
 */
@LoadLevel(name = "two", order = 2)
public class LoaderTestImpl2 implements LoaderTestSPI {
    @Override
    public String echo() {
        return "impl_2";
    }
}
