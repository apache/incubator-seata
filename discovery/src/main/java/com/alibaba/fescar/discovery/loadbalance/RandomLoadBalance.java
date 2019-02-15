package com.alibaba.fescar.discovery.loadbalance;

import java.util.List;
import java.util.Random;

/**
 * @author: yuoyao
 * @date 2019/02/14
 */
public class RandomLoadBalance extends AbstractLoadBalance {

    private final Random random=new Random();

    @Override
    protected <T> T doSelect(List<T> invokers) {
        int length=invokers.size();
        return invokers.get(random.nextInt(length));
    }
}
