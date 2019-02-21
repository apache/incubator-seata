package com.alibaba.fescar.discovery.registry;

/**
 * Created by kl on 2019/2/19.
 * Content :
 */
public  interface RedisListener {
    public static final String REGISTER = "register";
    public static final String UN_REGISTER = "unregister";
    void onEvent(String event);
}
