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
package io.seata.common.loader;

import io.seata.common.Constants;
import io.seata.common.executor.Initialize;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.Holder;
import io.seata.common.util.IOUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Enhanced service loader.
 *
 * @author slievrly
 */
public class EnhancedServiceLoader {

    /**
     * Specify classLoader to load the service provider
     *
     * @param <S>     the type parameter
     * @param service the service
     * @param loader  the loader
     * @return s s
     * @throws EnhancedServiceNotFoundException the enhanced service not found exception
     */
    public static <S> S load(Class<S> service, ClassLoader loader) throws EnhancedServiceNotFoundException {
        return InnerEnhancedServiceLoader.getServiceLoader(service).load(loader);
    }

    /**
     * load service provider
     *
     * @param <S>     the type parameter
     * @param service the service
     * @return s s
     * @throws EnhancedServiceNotFoundException the enhanced service not found exception
     */
    public static <S> S load(Class<S> service) throws EnhancedServiceNotFoundException {
        return InnerEnhancedServiceLoader.getServiceLoader(service).load();
    }

    /**
     * load service provider
     *
     * @param <S>          the type parameter
     * @param service      the service
     * @param activateName the activate name
     * @return s s
     * @throws EnhancedServiceNotFoundException the enhanced service not found exception
     */
    public static <S> S load(Class<S> service, String activateName) throws EnhancedServiceNotFoundException {
        return InnerEnhancedServiceLoader.getServiceLoader(service).load(activateName,
                InnerEnhancedServiceLoader.findClassLoader());
    }

    /**
     * Specify classLoader to load the service provider
     *
     * @param <S>          the type parameter
     * @param service      the service
     * @param activateName the activate name
     * @param loader       the loader
     * @return s s
     * @throws EnhancedServiceNotFoundException the enhanced service not found exception
     */
    public static <S> S load(Class<S> service, String activateName, ClassLoader loader)
        throws EnhancedServiceNotFoundException {
        return InnerEnhancedServiceLoader.getServiceLoader(service).load(activateName,loader);
    }

    /**
     * Load s.
     *
     * @param <S>          the type parameter
     * @param service      the service
     * @param activateName the activate name
     * @param args         the args
     * @return the s
     * @throws EnhancedServiceNotFoundException the enhanced service not found exception
     */
    public static <S> S load(Class<S> service, String activateName, Object[] args)
        throws EnhancedServiceNotFoundException {
        return InnerEnhancedServiceLoader.getServiceLoader(service).load(activateName,args);
    }

    /**
     * Load s.
     *
     * @param <S>          the type parameter
     * @param service      the service
     * @param activateName the activate name
     * @param argsType     the args type
     * @param args         the args
     * @return the s
     * @throws EnhancedServiceNotFoundException the enhanced service not found exception
     */
    public static <S> S load(Class<S> service, String activateName, Class[] argsType, Object[] args)
        throws EnhancedServiceNotFoundException {
        return InnerEnhancedServiceLoader.getServiceLoader(service).load(activateName,argsType,args);
    }

    /**
     * get all implements
     *
     * @param <S>     the type parameter
     * @param service the service
     * @return list list
     */
    public static <S> List<S> loadAll(Class<S> service) {
        return InnerEnhancedServiceLoader.getServiceLoader(service).loadAll();
    }

    /**
     * get all implements
     *
     * @param <S>     the type parameter
     * @param service the service
     * @param argsType     the args type
     * @param args         the args
     * @return list list
     */
    public static <S> List<S> loadAll(Class<S> service, Class[] argsType, Object[] args) {
        return InnerEnhancedServiceLoader.getServiceLoader(service).loadAll(argsType,args);
    }

    /**
     * Get all the extension classes, follow {@linkplain LoadLevel} defined and sort order
     *
     * @param <S>     the type parameter
     * @param service the service
     * @return all extension class
     */
    @SuppressWarnings("rawtypes")
    static <S> List<Class> getAllExtensionClass(Class<S> service) {
        return InnerEnhancedServiceLoader.getServiceLoader(service).getAllExtensionClass();
    }

    /**
     * Get all the extension classes, follow {@linkplain LoadLevel} defined and sort order
     *
     * @param <S>     the type parameter
     * @param service the service
     * @param loader  the loader
     * @return all extension class
     */
    @SuppressWarnings("rawtypes")
    static <S> List<Class> getAllExtensionClass(Class<S> service, ClassLoader loader) {
        return InnerEnhancedServiceLoader.getServiceLoader(service).getAllExtensionClass(loader);
    }


    static class InnerEnhancedServiceLoader<S> {
        private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedServiceLoader.class);
        private static final String SERVICES_DIRECTORY = "META-INF/services/";
        private static final String SEATA_DIRECTORY = "META-INF/seata/";

        private static final ConcurrentMap<Class<?>, InnerEnhancedServiceLoader<?>> SERVICE_LOADERS =
                new ConcurrentHashMap<>();
        private static final ConcurrentMap<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();

        private final Class<S> type;
        private final Holder<Map<ExtensionURL, Class<?>>> extensionClasses = new Holder<>();
        private final ConcurrentMap<ExtensionURL, Holder<Object>> extensionInstances = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, List<ExtensionURL>> extensionNameUrlsMap = new ConcurrentHashMap<>();
        private final ConcurrentMap<Class<?>, ExtensionURL> extensionClassUrlMap = new ConcurrentHashMap<>();
        private ExtensionURL defaultExtensionURL = null;
        private Integer highestLoadPriority = Integer.MIN_VALUE;

        private InnerEnhancedServiceLoader(Class<S> type) {
            this.type = type;
        }

        /**
         * Get the ServiceLoader for the specified Class
         *
         * @param type the type of the extension point
         * @param <S>  the type
         * @return the service loader
         */
        static <S> InnerEnhancedServiceLoader<S> getServiceLoader(Class<S> type) {
            if (type == null) {
                throw new IllegalArgumentException("Enhanced Service type == null");
            }
            InnerEnhancedServiceLoader<S> loader = (InnerEnhancedServiceLoader<S>)SERVICE_LOADERS.get(type);
            if (loader == null) {
                SERVICE_LOADERS.putIfAbsent(type, new InnerEnhancedServiceLoader<S>(type));
                loader = (InnerEnhancedServiceLoader<S>)SERVICE_LOADERS.get(type);
            }
            return loader;
        }

        /**
         * Specify classLoader to load the service provider
         *
         * @param loader the loader
         * @return s s
         * @throws EnhancedServiceNotFoundException the enhanced service not found exception
         */
        public S load(ClassLoader loader) throws EnhancedServiceNotFoundException {
            return loadExtension(loader, null, null);
        }

        /**
         * load service provider
         *
         * @return s s
         * @throws EnhancedServiceNotFoundException the enhanced service not found exception
         */
        public S load() throws EnhancedServiceNotFoundException {
            return loadExtension(findClassLoader(), null, null);
        }

        /**
         * load service provider
         *
         * @param activateName the activate name
         * @return s s
         * @throws EnhancedServiceNotFoundException the enhanced service not found exception
         */
        public S load(String activateName) throws EnhancedServiceNotFoundException {
            return loadExtension(activateName, findClassLoader(), null, null);
        }

        /**
         * Specify classLoader to load the service provider
         *
         * @param activateName the activate name
         * @param loader       the loader
         * @return s s
         * @throws EnhancedServiceNotFoundException the enhanced service not found exception
         */
        public S load(String activateName, ClassLoader loader)
                throws EnhancedServiceNotFoundException {
            return loadExtension(activateName, loader, null, null);
        }

        /**
         * Load s.
         *
         * @param activateName the activate name
         * @param args         the args
         * @return the s
         * @throws EnhancedServiceNotFoundException the enhanced service not found exception
         */
        public S load(String activateName, Object[] args)
                throws EnhancedServiceNotFoundException {
            Class[] argsType = null;
            if (args != null && args.length > 0) {
                argsType = new Class[args.length];
                for (int i = 0; i < args.length; i++) {
                    argsType[i] = args[i].getClass();
                }
            }
            return loadExtension(activateName, findClassLoader(), argsType, args);
        }

        /**
         * Load s.
         *
         * @param activateName the activate name
         * @param argsType     the args type
         * @param args         the args
         * @return the s
         * @throws EnhancedServiceNotFoundException the enhanced service not found exception
         */
        public S load(String activateName, Class[] argsType, Object[] args)
                throws EnhancedServiceNotFoundException {
            return loadExtension(activateName, findClassLoader(), argsType, args);
        }

        /**
         * get all implements
         *
         * @return list list
         */
        public List<S> loadAll() {
            return loadAll(null, null);
        }

        /**
         * get all implements
         *
         * @param argsType the args type
         * @param args     the args
         * @return list list
         */
        public List<S> loadAll(Class[] argsType, Object[] args) {
            List<S> allInstances = new ArrayList<>();
            List<Class> allClazzs = getAllExtensionClass();
            if (CollectionUtils.isEmpty(allClazzs)) {
                return allInstances;
            }
            try {
                for (Class clazz : allClazzs) {
                    ExtensionURL url = extensionClassUrlMap.get(clazz);
                    allInstances.add(getExtension(url, findClassLoader(), argsType, args));
                }
            } catch (Throwable t) {
                throw new EnhancedServiceNotFoundException(t);
            }
            return allInstances;
        }

        /**
         * Get all the extension classes, follow {@linkplain LoadLevel} defined and sort order
         *
         * @return all extension class
         */
        @SuppressWarnings("rawtypes")
        List<Class> getAllExtensionClass() {
            Map<ExtensionURL, Class<?>> map = loadAllExtensionClass(findClassLoader());
            List<Class> result = new ArrayList<>(map.values());
            return result;
        }

        /**
         * Get all the extension classes, follow {@linkplain LoadLevel} defined and sort order
         *
         * @param loader the loader
         * @return all extension class
         */
        @SuppressWarnings("rawtypes")
        List<Class> getAllExtensionClass(ClassLoader loader) {
            Map<ExtensionURL, Class<?>> map = loadAllExtensionClass(loader);
            List<Class> result = new ArrayList<>(map.values());
            return result;
        }

        @SuppressWarnings("rawtypes")
        private S loadExtension(ClassLoader loader, Class[] argTypes,
                                Object[] args) {
            try {
                loadAllExtensionClass(loader);
                return getExtension(defaultExtensionURL, loader, argTypes, args);
            } catch (Throwable e) {
                if (e instanceof EnhancedServiceNotFoundException) {
                    throw (EnhancedServiceNotFoundException)e;
                } else {
                    throw new EnhancedServiceNotFoundException(
                            "not found service provider for : " + type.getName() + " caused by " + ExceptionUtils
                                    .getFullStackTrace(e));
                }
            }
        }

        @SuppressWarnings("rawtypes")
        private S loadExtension(String activateName, ClassLoader loader, Class[] argTypes,
                                Object[] args) {
            if (io.seata.common.util.StringUtils.isEmpty(activateName)) {
                throw new IllegalArgumentException("the name of service provider for [" + type.getName() + "] name is null");
            }
            try {
                loadAllExtensionClass(loader);
                ExtensionURL cachedExtensionURL = getCachedServiceURL(activateName);
                return getExtension(cachedExtensionURL, loader, argTypes, args);
            } catch (Throwable e) {
                if (e instanceof EnhancedServiceNotFoundException) {
                    throw (EnhancedServiceNotFoundException)e;
                } else {
                    throw new EnhancedServiceNotFoundException(
                            "not found service provider for : " + type.getName() + " caused by " + ExceptionUtils
                                    .getFullStackTrace(e));
                }
            }
        }

        private S getExtension(ExtensionURL url, ClassLoader loader, Class[] argTypes,
                               Object[] args) {
            if (url == null) {
                throw new EnhancedServiceNotFoundException("service provider not found");
            }
            if (Scope.SINGLETON.equals(url.getScope())) {
                Holder<Object> holder = extensionInstances.get(url);
                if (holder == null) {
                    extensionInstances.putIfAbsent(url, new Holder<>());
                    holder = extensionInstances.get(url);
                }
                Object instance = holder.get();
                if (instance == null) {
                    synchronized (holder) {
                        instance = holder.get();
                        if (instance == null) {
                            instance = createExtension(url, loader, argTypes, args);
                            holder.set(instance);
                        }
                    }
                }
                return (S)instance;
            } else {
                return createNewExtension(url, loader, argTypes, args);
            }
        }

        private S createExtension(ExtensionURL url, ClassLoader loader, Class[] argTypes,
                                  Object[] args) {
            Class<?> clazz = loadAllExtensionClass(loader).get(url);
            if (clazz == null) {
                throw new EnhancedServiceNotFoundException("Not Found");
            }
            try {
                S instance = (S)EXTENSION_INSTANCES.get(clazz);
                if (instance == null) {
                    S newInstance = initInstance(clazz, argTypes, args);
                    EXTENSION_INSTANCES.putIfAbsent(clazz, newInstance);
                    instance = newInstance;
                }
                return instance;
            } catch (Throwable t) {
                throw new IllegalStateException("Extension instance(url: " + url + ", class: " +
                        type + ")  could not be instantiated: " + t.getMessage(), t);
            }
        }

        private S createNewExtension(ExtensionURL url, ClassLoader loader, Class[] argTypes, Object[] args) {
            Class<?> clazz = loadAllExtensionClass(loader).get(url);
            if (clazz == null) {
                throw new EnhancedServiceNotFoundException("Not Found");
            }
            try {
                S newInstance = initInstance(clazz, argTypes, args);
                return newInstance;
            } catch (Throwable t) {
                throw new IllegalStateException("Extension instance(url: " + url + ", class: " +
                        type + ")  could not be instantiated: " + t.getMessage(), t);
            }
        }

        private Map<ExtensionURL, Class<?>> loadAllExtensionClass(ClassLoader loader) {
            Map<ExtensionURL, Class<?>> classes = extensionClasses.get();
            if (classes == null) {
                synchronized (extensionClasses) {
                    classes = extensionClasses.get();
                    if (classes == null) {
                        classes = findAllExtensionClass(loader);
                        extensionClasses.set(classes);
                    }
                }
            }
            return classes;
        }

        @SuppressWarnings("rawtypes")
        private Map<ExtensionURL, Class<?>> findAllExtensionClass(ClassLoader loader) {
            Map<ExtensionURL, Class<?>> extensions = new HashMap<>();
            try {
                loadFile(SERVICES_DIRECTORY, loader, extensions);
                loadFile(SEATA_DIRECTORY, loader, extensions);
            } catch (IOException e) {
                throw new EnhancedServiceNotFoundException(e);
            }

            if (!extensions.isEmpty()) {
                extensions = sortAllExtensionClass(extensions);
            }

            if (!extensionNameUrlsMap.isEmpty()) {
                for (List<ExtensionURL> urlList : extensionNameUrlsMap.values()) {
                    Collections.sort(urlList, (url1, url2) -> {
                        int o1 = url1.getOrder();
                        int o2 = url2.getOrder();
                        return Integer.compare(o1, o2);
                    });
                }
            }
            return extensions;
        }

        private Map<ExtensionURL, Class<?>> sortAllExtensionClass(Map<ExtensionURL, Class<?>> extensions) {
            Set<ExtensionURL> entrySet = extensions.keySet();
            List<ExtensionURL> list = new ArrayList<>(entrySet);
            Collections.sort(list, (url1, url2) -> {
                int o1 = url1.getOrder();
                int o2 = url2.getOrder();
                return Integer.compare(o1, o2);
            });
            LinkedHashMap<ExtensionURL, Class<?>> linkedHashMap = new LinkedHashMap<ExtensionURL, Class<?>>();
            for (ExtensionURL url : list) {
                linkedHashMap.put(url, extensions.get(url));
            }
            return linkedHashMap;
        }

        @SuppressWarnings("rawtypes")
        private void loadFile(String dir, ClassLoader classLoader, Map<ExtensionURL, Class<?>> extensions)
                throws IOException {
            String fileName = dir + type.getName();
            Enumeration<java.net.URL> urls;
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
                        reader = new BufferedReader(new InputStreamReader(url.openStream(), Constants.DEFAULT_CHARSET));
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            final int ci = line.indexOf('#');
                            if (ci >= 0) {
                                line = line.substring(0, ci);
                            }
                            line = line.trim();
                            if (line.length() > 0) {
                                try {
                                    Class<?> clazz = Class.forName(line, true, classLoader);
                                    ExtensionURL extensionUrl = getExtensionURL(clazz);
                                    extensions.put(extensionUrl, clazz);
                                } catch (LinkageError | ClassNotFoundException e) {
                                    LOGGER.warn("load [{}] class fail. {}", line, e.getMessage());
                                }
                            }
                        }
                    } catch (Throwable e) {
                        LOGGER.warn(e.getMessage());
                    } finally {
                        IOUtil.close(reader);
                    }
                }
            }
        }

        private ExtensionURL getExtensionURL(Class<?> clazz) {
            String serviceName = null;
            String typeName = null;
            Integer priority = 0;
            Scope scope = Scope.SINGLETON;
            LoadLevel loadLevel = clazz.getAnnotation(LoadLevel.class);
            typeName = clazz.getTypeName();
            if (loadLevel != null) {
                serviceName = loadLevel.name();
                priority = loadLevel.order();
                scope = loadLevel.scope();
            }
            ExtensionURL result = new ExtensionURL(serviceName, typeName, priority, scope);
            //do cache
            extensionClassUrlMap.put(clazz, result);
            if (loadLevel != null) {
                if (extensionNameUrlsMap.containsKey(serviceName)) {
                    List<ExtensionURL> urls = extensionNameUrlsMap.get(serviceName);
                    urls.add(result);
                } else {
                    List<ExtensionURL> urls = new ArrayList<>();
                    urls.add(result);
                    extensionNameUrlsMap.put(serviceName, urls);
                }
            }
            if (priority >= highestLoadPriority) {
                //the highest priority based on the order param in LoadLevel,matches the current class's
                //defaultExtensionURL,so cached them together
                highestLoadPriority = priority;
                defaultExtensionURL = result;
                return defaultExtensionURL;
            }
            return result;
        }

        private ExtensionURL getCachedServiceURL(String activateName) {
            if (extensionNameUrlsMap.containsKey(activateName)) {
                List<ExtensionURL> urls = extensionNameUrlsMap.get(activateName);
                return urls.get(urls.size() - 1);
            }
            return null;
        }

        /**
         * init instance
         *
         * @param implClazz the impl clazz
         * @param argTypes  the arg types
         * @param args      the args
         * @return s s
         * @throws IllegalAccessException    the illegal access exception
         * @throws InstantiationException    the instantiation exception
         * @throws NoSuchMethodException     the no such method exception
         * @throws InvocationTargetException the invocation target exception
         */
        protected S initInstance(Class implClazz, Class[] argTypes, Object[] args)
                throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
            S s = null;
            if (argTypes != null && args != null) {
                // Constructor with arguments
                Constructor<S> constructor = implClazz.getDeclaredConstructor(argTypes);
                s = type.cast(constructor.newInstance(args));
            } else {
                // default Constructor
                s = type.cast(implClazz.newInstance());
            }
            if (s instanceof Initialize) {
                ((Initialize)s).init();
            }
            return s;
        }

        /**
         * Cannot use TCCL, in the pandora container will cause the class in the plugin not to be loaded
         *
         * @return
         */
        static ClassLoader findClassLoader() {
            return EnhancedServiceLoader.class.getClassLoader();
        }

    }
}