package org.apache.seata.common.http;

import org.apache.seata.common.loader.EnhancedServiceLoader;

/**
 * HttpClientFactory：根据运行环境动态加载合适的 HttpClient 实现
 */
public final class HttpClientFactory {

    private static final String HTTP1_IMPL = "Http1";
    private static final String HTTP2_IMPL = "Http2";
    private static final HttpClient INSTANCE = createInstance();

    private HttpClientFactory() {
    }

    private static HttpClient createInstance() {
        String implName = isOkHttpAvailable() ? HTTP1_IMPL : HTTP2_IMPL;
        return EnhancedServiceLoader.load(HttpClient.class, implName);
    }

    /**
     * 检测当前 classpath 中是否存在 OkHttp
     */
    private static boolean isOkHttpAvailable() {
        try {
            Class.forName("okhttp3.OkHttpClient", false, HttpClientFactory.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    /**
     * 获取单例 HttpClient 实例
     */
    public static HttpClient getInstance() {
        return INSTANCE;
    }
}


