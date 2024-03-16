/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.seata.spi;

import org.apache.seata.common.Constants;
import org.apache.seata.common.executor.Initialize;
import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.loader.Scope;
import org.apache.seata.common.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

class InnerEnhancedServiceLoader<S> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InnerEnhancedServiceLoader.class);
    private static final String SERVICES_DIRECTORY = "META-INF/services/";
    private static final String SEATA_DIRECTORY = "META-INF/seata/";

    private static final String APACHE_SEATA_PACKAGE_NAME = "org.apache.seata";
    private static final String IO_SEATA_PACKAGE_NAME = "io.seata";

    private final Class<S> type;
    private final Holder<List<ExtensionDefinition<S>>> definitionsHolder = new Holder<>();
    private final ConcurrentMap<ExtensionDefinition<S>, Holder<Object>> definitionToInstanceMap =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<ExtensionDefinition<S>>> nameToDefinitionsMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, ExtensionDefinition<S>> classToDefinitionMap = new ConcurrentHashMap<>();

    public InnerEnhancedServiceLoader(Class<S> type) {
        this.type = type;
    }

    public List<S> loadAll(ClassLoader loader) {
        return loadAll(null, null, loader);
    }

    public List<S> loadAll(Class<?>[] argsType, Object[] args, ClassLoader loader) {
        List<S> allInstances = new ArrayList<>();
        List<Class<S>> allClazzs = getAllExtensionClass(loader);
        if (CollectionUtils.isEmpty(allClazzs)) {
            return allInstances;
        }
        try {
            for (Class<S> clazz : allClazzs) {
                ExtensionDefinition<S> definition = classToDefinitionMap.get(clazz);
                allInstances.add(getExtensionInstance(definition, loader, argsType, args));
            }
        } catch (Throwable t) {
            throw new EnhancedServiceNotFoundException(t);
        }
        return allInstances;
    }

    private List<Class<S>> getAllExtensionClass(ClassLoader loader) {
        return loadAllExtensionClass(loader);
    }

    private S getExtensionInstance(ExtensionDefinition<S> definition, ClassLoader loader, Class<?>[] argTypes,
                                   Object[] args) {
        if (definition == null) {
            throw new EnhancedServiceNotFoundException("not found service provider for : " + type.getName());
        }
        if (Scope.SINGLETON.equals(definition.getScope())) {
            Holder<Object> holder = CollectionUtils.computeIfAbsent(definitionToInstanceMap, definition,
                    key -> new Holder<>());
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
            return (S) instance;
        } else {
            return createNewExtension(definition, loader, argTypes, args);
        }
    }

    private S createNewExtension(ExtensionDefinition<S> definition, ClassLoader loader, Class<?>[] argTypes, Object[] args) {
        Class<S> clazz = definition.getServiceClass();
        try {
            return initInstance(clazz, argTypes, args);
        } catch (Throwable t) {
            throw new IllegalStateException("Extension instance(definition: " + definition + ", class: " +
                    type + ")  could not be instantiated: " + t.getMessage(), t);
        }
    }

    private List<Class<S>> loadAllExtensionClass(ClassLoader loader) {
        List<ExtensionDefinition<S>> definitions = definitionsHolder.get();
        if (definitions == null) {
            synchronized (definitionsHolder) {
                definitions = definitionsHolder.get();
                if (definitions == null) {
                    definitions = findAllExtensionDefinition(loader);
                    definitionsHolder.set(definitions);
                }
            }
        }
        return definitions.stream().map(ExtensionDefinition::getServiceClass).collect(Collectors.toList());
    }

    private List<ExtensionDefinition<S>> findAllExtensionDefinition(ClassLoader loader) {
        List<ExtensionDefinition<S>> extensionDefinitions = new ArrayList<>();
        try {
            loadFile(SERVICES_DIRECTORY, type, loader, extensionDefinitions);
            loadFile(SEATA_DIRECTORY, type, loader, extensionDefinitions);

            @SuppressWarnings("rawtypes") Class compatibleService = getCompatibleService(type);
            if (compatibleService != null) {
                if (type.isAssignableFrom(compatibleService)) {
                    LOGGER.info("Load compatible class {}", compatibleService.getName());
                    loadFile(SERVICES_DIRECTORY, compatibleService, loader, extensionDefinitions);
                    loadFile(SEATA_DIRECTORY, compatibleService, loader, extensionDefinitions);
                } else {
                    LOGGER.info("Ignore load compatible class {}, because is not assignable from origin type {}", compatibleService.getName(), type.getName());
                }
            }

        } catch (IOException e) {
            throw new EnhancedServiceNotFoundException(e);
        }

        //After loaded all the extensions,sort the caches by order
        if (!nameToDefinitionsMap.isEmpty()) {
            for (List<ExtensionDefinition<S>> definitions : nameToDefinitionsMap.values()) {
                definitions.sort((def1, def2) -> {
                    int o1 = def1.getOrder();
                    int o2 = def2.getOrder();
                    return Integer.compare(o1, o2);
                });
            }
        }

        if (!extensionDefinitions.isEmpty()) {
            extensionDefinitions.sort((def1, def2) -> {
                int o1 = def1.getOrder();
                int o2 = def2.getOrder();
                return Integer.compare(o1, o2);
            });
        }

        return extensionDefinitions;
    }

    private static Class getCompatibleService(Class originType) {
        String ioSeataType = originType.getName().replace(APACHE_SEATA_PACKAGE_NAME, IO_SEATA_PACKAGE_NAME);
        try {
            return Class.forName(ioSeataType);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }


    private void loadFile(String dir, Class type, ClassLoader loader, List<ExtensionDefinition<S>> extensions)
            throws IOException {
        String fileName = dir + type.getName();
        Enumeration<URL> urls;
        if (loader != null) {
            urls = loader.getResources(fileName);
        } else {
            urls = ClassLoader.getSystemResources(fileName);
        }
        if (urls != null) {
            boolean hasServiceFile = false;
            boolean hasClasses = false;
            while (urls.hasMoreElements()) {
                hasServiceFile = true;
                java.net.URL url = urls.nextElement();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), Constants.DEFAULT_CHARSET))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final int ci = line.indexOf('#');
                        if (ci >= 0) {
                            line = line.substring(0, ci);
                        }
                        line = line.trim();
                        if (line.length() > 0) {
                            hasClasses = true;
                            try {
                                ExtensionDefinition<S> extensionDefinition = getUnloadedExtensionDefinition(line, loader);
                                if (extensionDefinition == null) {
                                    if (LOGGER.isDebugEnabled()) {
                                        LOGGER.debug("The same extension {} has already been loaded, skipped", line);
                                    }
                                    continue;
                                }
                                extensions.add(extensionDefinition);
                            } catch (LinkageError | ClassNotFoundException e) {
                                LOGGER.warn("Load [{}] class fail: {}", line, e.getMessage());
                            } catch (ClassCastException e) {
                                LOGGER.error("Load [{}] class fail, please make sure the extension" +
                                        " config in {} implements {}.", line, fileName, type.getName());
                            }
                        }
                    }
                } catch (Throwable e) {
                    LOGGER.warn("load class instance error:", e);
                }
            }

            if (LOGGER.isDebugEnabled()) {
                if (!hasServiceFile) {
                    LOGGER.warn("Load [{}] class fail: no service files found in '{}'.", type.getName(), dir);
                } else if (!hasClasses) {
                    LOGGER.warn("Load [{}] class fail: the service files in '{}' is all empty.", type.getName(), dir);
                }
            }
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.warn("Load [{}] class fail: no urls found in '{}'.", type.getName(), dir);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private ExtensionDefinition<S> getUnloadedExtensionDefinition(String className, ClassLoader loader)
            throws ClassNotFoundException, ClassCastException {
        //Check whether the definition has been loaded
        if (!isDefinitionContainsClazz(className, loader)) {
            Class<?> clazz = Class.forName(className, true, loader);
            if (!type.isAssignableFrom(clazz)) {
                LOGGER.error("can't cast {} to {}", clazz.getName(), type.getName());
                throw new ClassCastException();
            }
            Class<S> enhancedServiceClass = (Class<S>) clazz;
            String serviceName = null;
            int priority = 0;
            Scope scope = Scope.SINGLETON;
            LoadLevel loadLevel = clazz.getAnnotation(LoadLevel.class);
            if (loadLevel != null) {
                serviceName = loadLevel.name();
                priority = loadLevel.order();
                scope = loadLevel.scope();
            }
            ExtensionDefinition<S> result = new ExtensionDefinition<>(serviceName, priority, scope, enhancedServiceClass);
            classToDefinitionMap.put(clazz, result);
            if (serviceName != null) {
                CollectionUtils.computeIfAbsent(nameToDefinitionsMap, serviceName, e -> new ArrayList<>())
                        .add(result);
            }
            return result;
        }
        return null;
    }

    private boolean isDefinitionContainsClazz(String className, ClassLoader loader) {
        for (Map.Entry<Class<?>, ExtensionDefinition<S>> entry : classToDefinitionMap.entrySet()) {
            if (!entry.getKey().getName().equals(className)) {
                continue;
            }
            if (Objects.equals(entry.getValue().getServiceClass().getClassLoader(), loader)) {
                return true;
            }
        }
        return false;
    }

    private ExtensionDefinition<S> getDefaultExtensionDefinition() {
        List<ExtensionDefinition<S>> currentDefinitions = definitionsHolder.get();
        return CollectionUtils.getLast(currentDefinitions);
    }

    private ExtensionDefinition<S> getCachedExtensionDefinition(String activateName) {
        List<ExtensionDefinition<S>> definitions = nameToDefinitionsMap.get(activateName);
        return CollectionUtils.getLast(definitions);
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
    private S initInstance(Class<S> implClazz, Class<?>[] argTypes, Object[] args)
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
            ((Initialize) s).init();
        }
        return s;
    }

    /**
     * Helper Class for hold a value.
     *
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