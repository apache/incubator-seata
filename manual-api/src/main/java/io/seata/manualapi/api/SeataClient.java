package io.seata.manualapi.api;

import io.seata.common.executor.Callback;
import io.seata.commonapi.util.ProxyUtil;
import io.seata.rm.RMClient;
import io.seata.tm.TMClient;

import java.lang.reflect.InvocationTargetException;

public class SeataClient {

    public static void init(String applicationId, String txServiceGroup) {
        TMClient.init(applicationId, txServiceGroup);
        RMClient.init(applicationId, txServiceGroup);
    }

    /**
     * @param target
     * @param <T>
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public static <T> T createProxy(T target) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        return ProxyUtil.createProxy(target);
    }

    public static void execute(Callback<Object> targetCallback){



    }

    /**
     * register a branch source
     */
    public static <T> T registerBranchSource(T target) {
        //1、注册分支服务
        //2、创建增强代理
        return ProxyUtil.createProxy(target);
    }
}
