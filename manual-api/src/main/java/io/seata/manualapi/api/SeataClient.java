package io.seata.manualapi.api;

import io.seata.commonapi.interceptor.parser.DefaultResourceRegisterParser;
import io.seata.commonapi.util.ProxyUtil;
import io.seata.rm.RMClient;
import io.seata.tm.TMClient;

public class SeataClient {

    public static void init(String applicationId, String txServiceGroup) {
        TMClient.init(applicationId, txServiceGroup);
        RMClient.init(applicationId, txServiceGroup);
    }

    /**
     * @param target
     * @param <T>
     * @return
     */
    public static <T> T createProxy(T target) {
        return ProxyUtil.createProxy(target);
    }

    /**
     * register a branch source
     */
    public static void registerBranchSource(Object target) {
        DefaultResourceRegisterParser.get().registerResource(target);
    }
}
