package org.apache.seata.common.http;

import org.apache.seata.common.loader.EnhancedServiceLoader;

public class HttpExecutorFactory {

    private static final String HTTP1_IMPL = "Http1";
    private static final String HTTP2_IMPL = "Http2";
    private static final HttpExecutor INSTANCE = createInstance();

    private HttpExecutorFactory() {
    }

    private static HttpExecutor createInstance() {
        String implName = isOkHttpAvailable() ? HTTP1_IMPL : HTTP2_IMPL;
        return EnhancedServiceLoader.load(HttpExecutor.class, implName);
    }

    /**
     * 检测当前 classpath 中是否存在 OkHttp
     */
    private static boolean isOkHttpAvailable() {
        try {
            Class.forName("okhttp3.OkHttpClient", false, HttpExecutorFactory.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    /**
     * 获取单例 HttpClient 实例
     */
    public static HttpExecutor getInstance() {
        return INSTANCE;
    }
}
