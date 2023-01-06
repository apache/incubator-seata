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
package io.seata.spring.aot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import io.seata.common.util.ReflectionUtil;
import io.seata.spring.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.core.NativeDetector;
import org.springframework.core.SpringProperties;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import static org.springframework.aot.hint.MemberCategory.DECLARED_CLASSES;
import static org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS;
import static org.springframework.aot.hint.MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INTROSPECT_DECLARED_METHODS;
import static org.springframework.aot.hint.MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INTROSPECT_PUBLIC_METHODS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;
import static org.springframework.aot.hint.MemberCategory.PUBLIC_CLASSES;
import static org.springframework.aot.hint.MemberCategory.PUBLIC_FIELDS;

/**
 * The AOT utils
 *
 * @author wang.liang
 */
public class AotUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AotUtils.class);


    public static final String SPRING_AOT_PROCESSING = "spring.aot.processing";
    public static final String SPRING_AOT_ENABLED = "spring.aot.enabled";

    public static final MemberCategory[] ALL_MEMBER_CATEGORIES = new MemberCategory[]{
        INTROSPECT_PUBLIC_CONSTRUCTORS, INVOKE_PUBLIC_CONSTRUCTORS,
        INTROSPECT_DECLARED_CONSTRUCTORS, INVOKE_DECLARED_CONSTRUCTORS,
        PUBLIC_FIELDS, DECLARED_FIELDS,
        INTROSPECT_PUBLIC_METHODS, INVOKE_PUBLIC_METHODS,
        INTROSPECT_DECLARED_METHODS, INVOKE_DECLARED_METHODS,
        PUBLIC_CLASSES, DECLARED_CLASSES
    };

    public static final MemberCategory[] MEMBER_CATEGORIES_FOR_INSTANTIATE = new MemberCategory[]{
        INTROSPECT_DECLARED_CONSTRUCTORS, INVOKE_DECLARED_CONSTRUCTORS
    };

    public static final MemberCategory[] MEMBER_CATEGORIES_FOR_INSTANTIATE_AND_INVOKE = new MemberCategory[]{
        INTROSPECT_DECLARED_CONSTRUCTORS, INVOKE_DECLARED_CONSTRUCTORS,
        INVOKE_PUBLIC_METHODS, INVOKE_DECLARED_METHODS
    };


    /**
     * Whether AOT processing
     *
     * @return the boolean
     */
    public static boolean isAotProcessing() {
        return "true".equalsIgnoreCase(System.getProperty(SPRING_AOT_PROCESSING));
    }

    /**
     * Whether AOT enabled
     *
     * @return the boolean
     */
    public static boolean isAotEnabled() {
        return SpringProperties.getFlag(SPRING_AOT_ENABLED);
    }

    /**
     * Whether run in native-image
     *
     * @return the boolean
     */
    public static boolean inNativeImage() {
        return NativeDetector.inNativeImage();
    }


    //region # register reflection hints

    /**
     * Recursively register the class and its supper classes, interfaces, fields, and the parameters of methods to the reflection hints.
     *
     * @param clazz            the class
     * @param registerSelf     whether register self
     * @param reflectionHints  the reflection hints
     * @param memberCategories the member categories
     */
    public static void registerAllOfClass(Class<?> clazz, boolean registerSelf, ReflectionHints reflectionHints, MemberCategory... memberCategories) {
        registerAllOfClassInternal(new HashSet<>(), clazz, registerSelf, reflectionHints, memberCategories);
    }

    private static void registerAllOfClassInternal(@NonNull Set<Class<?>> cache, Class<?> clazz, boolean registerSelf, ReflectionHints reflectionHints, MemberCategory... memberCategories) {
        if (clazz == null) {
            return;
        }

        if (clazz.isPrimitive() || clazz.isEnum() || clazz.isAnnotation()) {
            return;
        }

        if (clazz.isArray()) {
            registerAllOfClassInternal(cache, clazz.getComponentType(), true, reflectionHints, memberCategories);
            return;
        }

        if (ReflectionUtil.isJavaClass(clazz)) {
            return;
        }

        // Cached to prevent endless loops
        if (cache.contains(clazz)) {
            return;
        }
        cache.add(clazz);

        // register self
        if (registerSelf) {
            reflectionHints.registerType(clazz, memberCategories);
            LOGGER.debug("Register class '{}' to reflection hints with member categories: {}", clazz.getName(), memberCategories);
        }

        // register the interfaces
        Set<Class<?>> interfaceClasses = ReflectionUtil.getInterfaces(clazz);
        for (Class<?> interfaceClass : interfaceClasses) {
            if (!interfaceClass.equals(clazz)) {
                registerAllOfClassInternal(cache, interfaceClass, true, reflectionHints, memberCategories);
            }
        }

        // register the supper class
        registerAllOfClassInternal(cache, clazz.getSuperclass(), true, reflectionHints, memberCategories);

        // register the fields
        Field[] fields = ReflectionUtil.getAllFields(clazz);
        for (Field field : fields) {
            registerAllOfClassInternal(cache, field.getType(), true, reflectionHints, memberCategories);
        }

        // register the parameters of methods
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (Class<?> parameterType : parameterTypes) {
                registerAllOfClassInternal(cache, parameterType, true, reflectionHints, memberCategories);
            }
        }
    }


    public static void registerReflectionType(ReflectionHints reflectionHints, MemberCategory[] memberCategories, String... classNames) {
        for (String className : classNames) {
            try {
                registerReflectionType(reflectionHints, memberCategories, Class.forName(className));
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                LOGGER.warn("Register reflection type failed: class not found '{}'.", className);
            }
        }
    }

    public static void registerReflectionType(ReflectionHints reflectionHints, MemberCategory[] memberCategories, Class<?>... classes) {
        for (Class<?> clazz : classes) {
            registerReflectionType(reflectionHints, memberCategories, clazz);
        }
    }

    private static void registerReflectionType(ReflectionHints reflectionHints, MemberCategory[] memberCategories, Class<?> clazz) {
        reflectionHints.registerType(clazz, memberCategories);
        LOGGER.debug("Register reflection type: {}", clazz.getName());
    }


    //region ## register services

    public static void registerReflectionServices(ReflectionHints reflectionHints, @Nullable Predicate<Resource> predicate, MemberCategory... memberCategories) {
        Resource[] resources = ResourceUtil.getResources("classpath*:META-INF/services/*");
        for (Resource resource : resources) {
            if (predicate != null && !predicate.test(resource)) {
                continue;
            }

            try (InputStreamReader isr = new InputStreamReader(resource.getInputStream());
                 BufferedReader br = new BufferedReader(isr)) {
                br.lines().forEach(className -> {
                    AotUtils.registerReflectionType(reflectionHints, memberCategories, className);
                });
            } catch (IOException e) {
                LOGGER.error("Register services '{}' fail: {}", resource.getFilename(), e.getMessage(), e);
            }
        }
    }

    public static void registerReflectionServices(ReflectionHints reflectionHints, MemberCategory... memberCategories) {
        registerReflectionServices(reflectionHints, null, memberCategories);
    }

    //endregion ##

    //endregion #
}
