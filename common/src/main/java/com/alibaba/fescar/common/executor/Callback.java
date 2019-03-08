package com.alibaba.fescar.common.executor;

/**
 * Callback
 * 
 * @author zhangsen
 *
 * @param <T>
 */
public interface Callback<T> {

    public T execute() throws Throwable;
}

