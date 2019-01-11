/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.common.loader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: jimin.jm@alibaba-inc.com
 * @Project: fescar-all
 * @DateTime: 2018/10/10 14:28
 * @FileName: EnhancedServiceLoader
 * @Description:
 */
public class EnhancedServiceLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedServiceLoader.class);
    private static final String SERVICES_DIRECTORY = "META-INF/services/";
    private static final String FESCAR_DIRECTORY = "META-INF/fescar/";
    @SuppressWarnings("rawtypes")
    private static Map<Class, List<Class>> providers = new ConcurrentHashMap<Class, List<Class>>();

    /**
     * 指定classloader加载server provider
     *
     * @param service
     * @param loader
     * @return
     * @throws EnhancedServiceNotFoundException
     */
    public static <S> S load(Class<S> service, ClassLoader loader) throws EnhancedServiceNotFoundException {
        return loadFile(service, null, loader);
    }

    /**
     * 加载server provider
     *
     * @param service
     * @return
     * @throws EnhancedServiceNotFoundException
     */
    public static <S> S load(Class<S> service) throws EnhancedServiceNotFoundException {
        return loadFile(service, null, findClassLoader());
    }

    /**
     * 加载server provider
     *
     * @param service
     * @return
     * @throws EnhancedServiceNotFoundException
     */
    public static <S> S load(Class<S> service, String activateName) throws EnhancedServiceNotFoundException {
        return loadFile(service, activateName, findClassLoader());
    }

    /**
     * 指定classloader加载server provider
     *
     * @param service
     * @param loader
     * @return
     * @throws EnhancedServiceNotFoundException
     */
    public static <S> S load(Class<S> service, String activateName, ClassLoader loader) throws EnhancedServiceNotFoundException {
        return loadFile(service, activateName, loader);
    }

    /**
     * 获取所有的扩展类，按照{@linkplain LoadLevel}定义的order顺序进行排序
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <S> List<Class> getAllExtensionClass(Class<S> service) {
        return findAllExtensionClass(service, null, findClassLoader());
    }

    /**
     * 获取所有的扩展类，按照{@linkplain LoadLevel}定义的order顺序进行排序
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <S> List<Class> getAllExtensionClass(Class<S> service, ClassLoader loader) {
        return findAllExtensionClass(service, null, loader);
    }

    @SuppressWarnings("rawtypes")
    private static <S> S loadFile(Class<S> service, String activateName, ClassLoader loader) {
        try {
            boolean foundFromCache = true;
            List<Class> extensions = providers.get(service);
            if (extensions == null) {
                synchronized (service) {
                    extensions = providers.get(service);
                    if (extensions == null) {
                        extensions = findAllExtensionClass(service, activateName, loader);
                        foundFromCache = false;
                        providers.put(service, extensions);
                    }
                }
            }

            // 为避免被覆盖，每个activateName的查找，允许再加一层子目录
            if (StringUtils.isNotEmpty(activateName)) {
                loadFile(service, FESCAR_DIRECTORY + activateName.toLowerCase() + "/", loader, extensions);

                List<Class> activateExtensions = new ArrayList<Class>();
                for (int i = 0; i < extensions.size(); i++) {
                    Class clz = extensions.get(i);
                    @SuppressWarnings("unchecked")
                    LoadLevel activate = (LoadLevel) clz.getAnnotation(LoadLevel.class);
                    if (activate != null && activateName.equals(activate.name())) {
                        activateExtensions.add(clz);
                    }
                }

                extensions = activateExtensions;
            }

            if (extensions.isEmpty()) {
                throw new EnhancedServiceNotFoundException("not found service provider for : " + service.getName() + "[" + activateName
                    + "] and classloader : " + ObjectUtils.toString(loader));
            }
            Class<?> extension = extensions.get(extensions.size() - 1);// 最大的一个
            S result = service.cast(extension.newInstance());
            if (!foundFromCache && LOGGER.isInfoEnabled()) {
                LOGGER.info("load " + service.getSimpleName() + "[" + activateName + "] extension by class[" + extension.getName() + "]");
            }
            return result;
        } catch (Throwable e) {
            if (e instanceof EnhancedServiceNotFoundException) {
                throw (EnhancedServiceNotFoundException) e;
            } else {
                throw new EnhancedServiceNotFoundException(
                    "not found service provider for : " + service.getName() + " caused by " + ExceptionUtils.getFullStackTrace(e));
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private static <S> List<Class> findAllExtensionClass(Class<S> service, String activateName, ClassLoader loader) {
        List<Class> extensions = new ArrayList<Class>();
        try {
            loadFile(service, SERVICES_DIRECTORY, loader, extensions);
            loadFile(service, FESCAR_DIRECTORY, loader, extensions);
        } catch (IOException e) {
            throw new EnhancedServiceNotFoundException(e);
        }

        if (extensions.isEmpty()) {
            return extensions;
        }

        // 做一下排序
        Collections.sort(extensions, new Comparator<Class>() {
            @Override
            public int compare(Class c1, Class c2) {
                Integer o1 = 0;
                Integer o2 = 0;
                @SuppressWarnings("unchecked")
                LoadLevel a1 = (LoadLevel) c1.getAnnotation(LoadLevel.class);
                @SuppressWarnings("unchecked")
                LoadLevel a2 = (LoadLevel) c2.getAnnotation(LoadLevel.class);

                if (a1 != null) {
                    o1 = a1.order();
                }

                if (a2 != null) {
                    o2 = a2.order();
                }

                return o1.compareTo(o2);

            }
        });

        return extensions;
    }

    @SuppressWarnings("rawtypes")
    private static void loadFile(Class<?> service, String dir, ClassLoader classLoader, List<Class> extensions) throws IOException {
        String fileName = dir + service.getName();
        Enumeration<URL> urls;
        if (classLoader != null) {
            urls = classLoader.getResources(fileName);
        } else {
            urls = ClassLoader.getSystemResources(fileName);
        }

        if (urls != null) {
            while (urls.hasMoreElements()) {
                java.net.URL url = urls.nextElement();
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        final int ci = line.indexOf('#');
                        if (ci >= 0) {
                            line = line.substring(0, ci);
                        }
                        line = line.trim();
                        if (line.length() > 0) {
                            extensions.add(Class.forName(line, true, classLoader));
                        }
                    }
                } catch (ClassNotFoundException e) {
                    // ignore
                } catch (Throwable e) {
                    LOGGER.warn(e.getMessage()); // 记录一下失败日志
                } finally {
                    try {
                        if (reader != null) {
                            reader.close();
                        }
                    } catch (IOException ioe) {
                        // ignore
                    }
                }
            }
        }
    }

    private static ClassLoader findClassLoader() {
        // 不能使用TCCL,在pandora容器中会导致无法加载plugin中的类
        return EnhancedServiceLoader.class.getClassLoader();
    }
}
