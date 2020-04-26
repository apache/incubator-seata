/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.spring.boot.autoconfigure.provider;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.seata.config.CacheConfigurationProvider;
import io.seata.config.Configuration;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

/**
 * @author funkye
 */
public class SpringCacheConfigurationProvider implements CacheConfigurationProvider {

    private static final String INTERCEPT_METHOD_PREFIX = "getConfig";

    private static final long CACHE_SIZE = 100;

    private static final long EXPIRE_TIME = 30 * 1000;

    private static final Cache<String, Object> CONFIG_CACHE = Caffeine.newBuilder().maximumSize(CACHE_SIZE)
        .expireAfterWrite(EXPIRE_TIME, TimeUnit.MILLISECONDS).softValues().build();

    @Override
    public Configuration provide(Configuration originalConfiguration) {
        return (Configuration)Enhancer.create(Configuration.class, new MethodInterceptor() {
            @Override
            public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy)
                throws Throwable {
                if (method.getName().equalsIgnoreCase(INTERCEPT_METHOD_PREFIX)) {
                    String rawDataId = (String)args[0];
                    Object result = CONFIG_CACHE.get(rawDataId, mappingFuncation -> {
                        return null;
                    });
                    if (null == result) {
                        result = method.invoke(originalConfiguration, args);
                        if (result != null) {
                            CONFIG_CACHE.put(rawDataId, result);
                        }
                    }
                    if (method.getReturnType().equals(String.class)) {
                        return String.valueOf(result);
                    }
                    return result;
                }
                return method.invoke(originalConfiguration, args);
            }
        });
    }
}
