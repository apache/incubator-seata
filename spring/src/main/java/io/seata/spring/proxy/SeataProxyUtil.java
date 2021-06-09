package io.seata.spring.proxy;

import org.aopalliance.intercept.MethodInvocation;

/**
 * The Seata Proxy Util
 *
 * @author wang.liang
 */
public final class SeataProxyUtil {

    private SeataProxyUtil() {
    }

    private static final ThreadLocal<Boolean> NEED_PROXY = new ThreadLocal<>();

    public static void disableProxy() {
        NEED_PROXY.set(false);
    }

    public static void enableProxy() {
        NEED_PROXY.remove();
    }

    /**
     * @return the boolean
     * @see SeataProxyInterceptor#invoke(MethodInvocation)
     */
    public static boolean isNeedProxy() {
        return !Boolean.FALSE.equals(NEED_PROXY.get());
    }
}
