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
import io.seata.common.util.IOUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
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
        return InnerEnhancedServiceLoader.getServiceLoader(service).load(activateName, loader);
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
        return InnerEnhancedServiceLoader.getServiceLoader(service).load(activateName, args);
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
        return InnerEnhancedServiceLoader.getServiceLoader(service).load(activateName, argsType, args);
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
     * @param <S>      the type parameter
     * @param service  the service
     * @param argsType the args type
     * @param args     the args
     * @return list list
     */
    public static <S> List<S> loadAll(Class<S> service, Class[] argsType, Object[] args) {
        return InnerEnhancedServiceLoader.getServiceLoader(service).loadAll(argsType, args);
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


    private static class InnerEnhancedServiceLoader<S> {
        private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedServiceLoader.class);
        private static final String SERVICES_DIRECTORY = "META-INF/services/";
        private static final String SEATA_DIRECTORY = "META-INF/seata/";

        private static final ConcurrentMap<Class<?>, InnerEnhancedServiceLoader<?>> SERVICE_LOADERS =
                new ConcurrentHashMap<>();

        private final Class<S> type;
        private final Holder<List<ExtensionDefinition>> definitionsHolder = new Holder<>();
        private final ConcurrentMap<ExtensionDefinition, Holder<Object>> definitionToInstanceMap =
                new ConcurrentHashMap<>();
        private final ConcurrentMap<String, List<ExtensionDefinition>> nameToDefinitionsMap = new ConcurrentHashMap<>();
        private final ConcurrentMap<Class<?>, ExtensionDefinition> classToDefinitionMap = new ConcurrentHashMap<>();
        private ExtensionDefinition defaultExtensionDefinition = null;
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
        private static <S> InnerEnhancedServiceLoader<S> getServiceLoader(Class<S> type) {
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
        private S load(ClassLoader loader) throws EnhancedServiceNotFoundException {
            return loadExtension(loader, null, null);
        }

        /**
         * load service provider
         *
         * @return s s
         * @throws EnhancedServiceNotFoundException the enhanced service not found exception
         */
        private S load() throws EnhancedServiceNotFoundException {
            return loadExtension(findClassLoader(), null, null);
        }

        /**
         * load service provider
         *
         * @param activateName the activate name
         * @return s s
         * @throws EnhancedServiceNotFoundException the enhanced service not found exception
         */
        private S load(String activateName) throws EnhancedServiceNotFoundException {
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
        private S load(String activateName, ClassLoader loader)
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
        private S load(String activateName, Object[] args)
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
        private S load(String activateName, Class[] argsType, Object[] args)
                throws EnhancedServiceNotFoundException {
            return loadExtension(activateName, findClassLoader(), argsType, args);
        }

        /**
         * get all implements
         *
         * @return list list
         */
        private List<S> loadAll() {
            return loadAll(null, null);
        }

        /**
         * get all implements
         *
         * @param argsType the args type
         * @param args     the args
         * @return list list
         */
        private List<S> loadAll(Class[] argsType, Object[] args) {
            List<S> allInstances = new ArrayList<>();
            List<Class> allClazzs = getAllExtensionClass();
            if (CollectionUtils.isEmpty(allClazzs)) {
                return allInstances;
            }
            try {
                for (Class clazz : allClazzs) {
                    ExtensionDefinition definition = classToDefinitionMap.get(clazz);
                    allInstances.add(getExtensionInstance(definition, findClassLoader(), argsType, args));
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
        private List<Class> getAllExtensionClass() {
            return loadAllExtensionClass(findClassLoader());
        }

        /**
         * Get all the extension classes, follow {@linkplain LoadLevel} defined and sort order
         *
         * @param loader the loader
         * @return all extension class
         */
        @SuppressWarnings("rawtypes")
        private List<Class> getAllExtensionClass(ClassLoader loader) {
            return loadAllExtensionClass(loader);
        }

        @SuppressWarnings("rawtypes")
        private S loadExtension(ClassLoader loader, Class[] argTypes,
                                Object[] args) {
            try {
                loadAllExtensionClass(loader);
                return getExtensionInstance(defaultExtensionDefinition, loader, argTypes, args);
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
                ExtensionDefinition cachedExtensionDefinition = getCachedExtensionDefinition(activateName);
                return getExtensionInstance(cachedExtensionDefinition, loader, argTypes, args);
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

        private S getExtensionInstance(ExtensionDefinition definition, ClassLoader loader, Class[] argTypes,
                                       Object[] args) {
            if (definition == null) {
                throw new EnhancedServiceNotFoundException("not found service provider for : " + type.getName());
            }
            if (Scope.SINGLETON.equals(definition.getScope())) {
                Holder<Object> holder = definitionToInstanceMap.get(definition);
                if (holder == null) {
                    definitionToInstanceMap.putIfAbsent(definition, new Holder<>());
                    holder = definitionToInstanceMap.get(definition);
                }
                Object instance = holder.get();
                if (instance == null) {
                    synchronized (holder) {
                        instance = holder.get();
                        if (instance == null) {
                            instance = createNewExtension(definition, loader, argTypes, args);
                            holder.set(instance);
                        }
                    }
                }
                return (S)instance;
            } else {
                return createNewExtension(definition, loader, argTypes, args);
            }
        }

        private S createNewExtension(ExtensionDefinition definition, ClassLoader loader, Class[] argTypes, Object[] args) {
            loadAllExtensionClass(loader);
            Class<?> clazz = definition.getServiceClass();
            try {
                S newInstance = initInstance(clazz, argTypes, args);
                return newInstance;
            } catch (Throwable t) {
                throw new IllegalStateException("Extension instance(definition: " + definition + ", class: " +
                        type + ")  could not be instantiated: " + t.getMessage(), t);
            }
        }

        private List<Class> loadAllExtensionClass(ClassLoader loader) {
            List<Class> result;
            List<ExtensionDefinition> definitions = definitionsHolder.get();
            if (definitions == null) {
                synchronized (definitionsHolder) {
                    definitions = definitionsHolder.get();
                    if (definitions == null) {
                        definitions = findAllExtensionDefinition(loader);
                        definitionsHolder.set(definitions);
                    }
                }
            }
            result = definitions.stream().map(def -> def.getServiceClass()).collect(Collectors.toList());
            return result;
        }

        @SuppressWarnings("rawtypes")
        private List<ExtensionDefinition> findAllExtensionDefinition(ClassLoader loader) {
            List<ExtensionDefinition> extensionDefinitions = new ArrayList<>();
            try {
                loadFile(SERVICES_DIRECTORY, loader, extensionDefinitions);
                loadFile(SEATA_DIRECTORY, loader, extensionDefinitions);
            } catch (IOException e) {
                throw new EnhancedServiceNotFoundException(e);
            }

            //After loaded all the extensions,sort the caches by order
            if (!nameToDefinitionsMap.isEmpty()) {
                for (List<ExtensionDefinition> definitions : nameToDefinitionsMap.values()) {
                    Collections.sort(definitions, (def1, def2) -> {
                        int o1 = def1.getOrder();
                        int o2 = def2.getOrder();
                        return Integer.compare(o1, o2);
                    });
                }
            }

            if (!extensionDefinitions.isEmpty()) {
                Collections.sort(extensionDefinitions, (definition1, definition2) -> {
                    int o1 = definition1.getOrder();
                    int o2 = definition2.getOrder();
                    return Integer.compare(o1, o2);
                });
            }

            return extensionDefinitions;
        }


        @SuppressWarnings("rawtypes")
        private void loadFile(String dir, ClassLoader classLoader, List<ExtensionDefinition> extensions)
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
                                    ExtensionDefinition extensionDefinition = getExtensionDefinition(clazz);
                                    extensions.add(extensionDefinition);
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

        private ExtensionDefinition getExtensionDefinition(Class<?> clazz) {
            String serviceName = null;
            Integer priority = 0;
            Scope scope = Scope.SINGLETON;
            LoadLevel loadLevel = clazz.getAnnotation(LoadLevel.class);
            if (loadLevel != null) {
                serviceName = loadLevel.name();
                priority = loadLevel.order();
                scope = loadLevel.scope();
            }
            ExtensionDefinition result = new ExtensionDefinition(serviceName, priority, scope, clazz);
            //do cache
            classToDefinitionMap.put(clazz, result);
            if (serviceName != null) {
                if (nameToDefinitionsMap.containsKey(serviceName)) {
                    List<ExtensionDefinition> definitions = nameToDefinitionsMap.get(serviceName);
                    definitions.add(result);
                } else {
                    List<ExtensionDefinition> definitions = new ArrayList<>();
                    definitions.add(result);
                    nameToDefinitionsMap.put(serviceName, definitions);
                }
            }
            if (defaultExtensionDefinition == null || priority >= highestLoadPriority) {
                //the highestLoadPriority matches the current class's defaultExtensionDefinition,cached them together
                highestLoadPriority = priority;
                defaultExtensionDefinition = result;
            }
            return result;
        }

        private ExtensionDefinition getCachedExtensionDefinition(String activateName) {
            if (nameToDefinitionsMap.containsKey(activateName)) {
                List<ExtensionDefinition> definitions = nameToDefinitionsMap.get(activateName);
                return definitions.get(definitions.size() - 1);
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
        private S initInstance(Class implClazz, Class[] argTypes, Object[] args)
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
        private static ClassLoader findClassLoader() {
            return EnhancedServiceLoader.class.getClassLoader();
        }


        /**
         * Helper Class for hold a value.
         * @param <T>
         */
        private static class Holder<T> {
            private volatile T value;

            private void set(T value) {
                this.value = value;
            }

            private T get() {
                return value;
            }
        }
    }


}