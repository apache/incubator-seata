package com.alibaba.fescar.common.executor;

/**
 * 回调
 * 
 * @author zhangsen
 *
 * @param <T>
 */
public interface Callback<T> {

    public T execute() throws Throwable;
}

