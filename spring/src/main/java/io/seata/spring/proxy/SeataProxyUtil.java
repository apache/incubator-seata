package io.seata.spring.proxy;

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

    public static boolean isNeedProxy() {
        return !Boolean.FALSE.equals(NEED_PROXY.get());
    }
}
