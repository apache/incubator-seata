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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.Constants;
import io.seata.common.executor.Initialize;
import io.seata.common.util.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Enhanced service loader.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /10/10
 */
public class EnhancedServiceLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedServiceLoader.class);
    private static final String SERVICES_DIRECTORY = "META-INF/services/";
    private static final String SEATA_DIRECTORY = "META-INF/seata/";
    @SuppressWarnings("rawtypes")
    private static Map<Class, List<Class>> providers = new ConcurrentHashMap<Class, List<Class>>();

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
        return loadFile(service, null, loader);
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
        return loadFile(service, null, findClassLoader());
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
        return loadFile(service, activateName, findClassLoader());
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
        return loadFile(service, activateName, loader);
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
        Class[] argsType = null;
        if (args != null && args.length > 0) {
            argsType = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                argsType[i] = args[i].getClass();
            }
        }
        return loadFile(service, activateName, findClassLoader(), argsType, args);
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
        return loadFile(service, activateName, findClassLoader(), argsType, args);
    }

    /**
     * get all implements
     *
     * @param <S>     the type parameter
     * @param service the service
     * @return list list
     */
    public static <S> List<S> loadAll(Class<S> service) {
        List<S> allInstances = new ArrayList<>();
        List<Class> allClazzs = getAllExtensionClass(service);
        if (CollectionUtils.isEmpty(allClazzs)) {
            return allInstances;
        }
        try {
            for (Class clazz : allClazzs) {
                allInstances.add(initInstance(service, clazz, null, null));
            }
        } catch (Throwable t) {
            throw new EnhancedServiceNotFoundException(t);
        }
        return allInstances;
    }

    /**
     * Get all the extension classes, follow {@linkplain LoadLevel} defined and sort order
     *
     * @param <S>     the type parameter
     * @param service the service
     * @return all extension class
     */
    @SuppressWarnings("rawtypes")
    public static <S> List<Class> getAllExtensionClass(Class<S> service) {
        return findAllExtensionClass(service, null, findClassLoader());
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
    public static <S> List<Class> getAllExtensionClass(Class<S> service, ClassLoader loader) {
        return findAllExtensionClass(service, null, loader);
    }

    private static <S> S loadFile(Class<S> service, String activateName, ClassLoader loader) {
        return loadFile(service, activateName, loader, null, null);
    }

    @SuppressWarnings("rawtypes")
    private static <S> S loadFile(Class<S> service, String activateName, ClassLoader loader, Class[] argTypes,
                                  Object[] args) {
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
            if (StringUtils.isNotEmpty(activateName)) {
                loadFile(service, SEATA_DIRECTORY + activateName.toLowerCase() + "/", loader, extensions);

                List<Class> activateExtensions = new ArrayList<Class>();
                for (Class clz : extensions) {
                    @SuppressWarnings("unchecked")
                    LoadLevel activate = (LoadLevel) clz.getAnnotation(LoadLevel.class);
                    if (activate != null && activateName.equalsIgnoreCase(activate.name())) {
                        activateExtensions.add(clz);
                    }
                }

                extensions = activateExtensions;
            }

            if (extensions.isEmpty()) {
                throw new EnhancedServiceNotFoundException(
                    "not found service provider for : " + service.getName() + "[" + activateName
                        + "] and classloader : " + ObjectUtils.toString(loader));
            }
            Class<?> extension = extensions.get(extensions.size() - 1);
            S result = initInstance(service, extension, argTypes, args);
            if (!foundFromCache && LOGGER.isInfoEnabled()) {
                LOGGER.info("load " + service.getSimpleName() + "[" + activateName + "] extension by class[" + extension
                    .getName() + "]");
            }
            return result;
        } catch (Throwable e) {
            if (e instanceof EnhancedServiceNotFoundException) {
                throw (EnhancedServiceNotFoundException)e;
            } else {
                throw new EnhancedServiceNotFoundException(
                    "not found service provider for : " + service.getName() + " caused by " + ExceptionUtils
                        .getFullStackTrace(e));
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private static <S> List<Class> findAllExtensionClass(Class<S> service, String activateName, ClassLoader loader) {
        List<Class> extensions = new ArrayList<Class>();
        try {
            loadFile(service, SERVICES_DIRECTORY, loader, extensions);
            loadFile(service, SEATA_DIRECTORY, loader, extensions);
        } catch (IOException e) {
            throw new EnhancedServiceNotFoundException(e);
        }

        if (extensions.isEmpty()) {
            return extensions;
        }
        Collections.sort(extensions, new Comparator<Class>() {
            @Override
            public int compare(Class c1, Class c2) {
                int o1 = 0;
                int o2 = 0;
                @SuppressWarnings("unchecked")
                LoadLevel a1 = (LoadLevel)c1.getAnnotation(LoadLevel.class);
                @SuppressWarnings("unchecked")
                LoadLevel a2 = (LoadLevel)c2.getAnnotation(LoadLevel.class);

                if (a1 != null) {
                    o1 = a1.order();
                }

                if (a2 != null) {
                    o2 = a2.order();
                }

                return Integer.compare(o1, o2);

            }
        });

        return extensions;
    }

    @SuppressWarnings("rawtypes")
    private static void loadFile(Class<?> service, String dir, ClassLoader classLoader, List<Class> extensions)
        throws IOException {
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
                                extensions.add(Class.forName(line, true, classLoader));
                            } catch (LinkageError | ClassNotFoundException e) {
                                LOGGER.warn("load [{}] class fail. {}", line, e.getMessage());
                            }
                        }
                    }
                } catch (Throwable e) {
                    LOGGER.warn(e.getMessage());
                } finally {
                    try {
                        if (reader != null) {
                            reader.close();
                        }
                    } catch (IOException ioe) {
                    }
                }
            }
        }
    }

    /**
     * init instance
     *
     * @param <S>       the type parameter
     * @param service   the service
     * @param implClazz the impl clazz
     * @param argTypes  the arg types
     * @param args      the args
     * @return s s
     * @throws IllegalAccessException the illegal access exception
     * @throws InstantiationException the instantiation exception
     * @throws NoSuchMethodException the no such method exception
     * @throws InvocationTargetException the invocation target exception
     */
    protected static <S> S initInstance(Class<S> service, Class implClazz, Class[] argTypes, Object[] args)
        throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        S s = null;
        if (argTypes != null && args != null) {
            // Constructor with arguments
            Constructor<S> constructor = implClazz.getDeclaredConstructor(argTypes);
            constructor.setAccessible(true);
            s = service.cast(constructor.newInstance(args));
        } else {
            // default Constructor
            s = service.cast(implClazz.newInstance());
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
    private static ClassLoader findClassLoader() {
        return EnhancedServiceLoader.class.getClassLoader();
    }
}
