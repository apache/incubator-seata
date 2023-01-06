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

import static org.springframework.aot.hint.MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;

/**
 * The AOT utils
 *
 * @author wang.liang
 */
public class AotUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AotUtils.class);


    public static final String SPRING_AOT_PROCESSING = "spring.aot.processing";
    public static final String SPRING_AOT_ENABLED = "spring.aot.enabled";

    public static final MemberCategory[] ALL_MEMBER_CATEGORIES = MemberCategory.values();

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
    public static void registerAllOfClass(boolean registerSelf, ReflectionHints reflectionHints, Class<?> clazz, MemberCategory... memberCategories) {
        registerAllOfClassInternal(new HashSet<>(), registerSelf, reflectionHints, clazz, memberCategories);
    }

    private static void registerAllOfClassInternal(@NonNull Set<Class<?>> cache, boolean registerSelf, ReflectionHints reflectionHints, Class<?> clazz, MemberCategory... memberCategories) {
        if (clazz == null) {
            return;
        }

        if (clazz.isPrimitive() || clazz.isEnum() || clazz.isAnnotation()) {
            return;
        }

        if (clazz.isArray()) {
            registerAllOfClassInternal(cache, true, reflectionHints, clazz.getComponentType(), memberCategories);
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
            registerType(reflectionHints, clazz, memberCategories);
        }

        // register the interfaces
        Set<Class<?>> interfaceClasses = ReflectionUtil.getInterfaces(clazz);
        for (Class<?> interfaceClass : interfaceClasses) {
            if (!interfaceClass.equals(clazz)) {
                registerAllOfClassInternal(cache, true, reflectionHints, interfaceClass, memberCategories);
            }
        }

        // register the supper class
        registerAllOfClassInternal(cache, true, reflectionHints, clazz.getSuperclass(), memberCategories);

        // register the fields
        Field[] fields = ReflectionUtil.getAllFields(clazz);
        for (Field field : fields) {
            registerAllOfClassInternal(cache, true, reflectionHints, field.getType(), memberCategories);
        }

        // register the parameters of methods
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (Class<?> parameterType : parameterTypes) {
                registerAllOfClassInternal(cache, true, reflectionHints, parameterType, memberCategories);
            }
        }
    }


    public static void registerType(ReflectionHints reflectionHints, Class<?> clazz, MemberCategory... memberCategories) {
        reflectionHints.registerType(clazz, memberCategories);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.info("Register reflection type '{}' with member categories {}", clazz.getName(), memberCategories);
        }
    }

    public static void registerTypes(ReflectionHints reflectionHints, MemberCategory[] memberCategories, String... classNames) {
        for (String className : classNames) {
            try {
                registerType(reflectionHints, Class.forName(className), memberCategories);
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                LOGGER.warn("Register reflection type failed: class not found '{}'.", className);
            }
        }
    }

    public static void registerTypes(ReflectionHints reflectionHints, MemberCategory[] memberCategories, Class<?>... classes) {
        for (Class<?> clazz : classes) {
            registerType(reflectionHints, clazz, memberCategories);
        }
    }


    //region ## register services

    public static void registerServices(ReflectionHints reflectionHints, @Nullable Predicate<Resource> predicate, MemberCategory... memberCategories) {
        Resource[] resources = ResourceUtil.getResources("classpath*:META-INF/services/*");
        for (Resource resource : resources) {
            if (predicate != null && !predicate.test(resource)) {
                continue;
            }

            try (InputStreamReader isr = new InputStreamReader(resource.getInputStream());
                 BufferedReader br = new BufferedReader(isr)) {
                br.lines().forEach(className -> {
                    AotUtils.registerTypes(reflectionHints, memberCategories, className);
                });
            } catch (IOException e) {
                LOGGER.error("Register services '{}' fail: {}", resource.getFilename(), e.getMessage(), e);
            }
        }
    }

    public static void registerServices(ReflectionHints reflectionHints, MemberCategory... memberCategories) {
        registerServices(reflectionHints, null, memberCategories);
    }

    //endregion ##

    //endregion #
}
