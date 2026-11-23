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
package org.apache.seata.spring.boot.autoconfigure.http;

import org.apache.seata.core.rpc.netty.http.ControllerManager;
import org.apache.seata.core.rpc.netty.http.HttpInvocation;
import org.apache.seata.core.rpc.netty.http.ParamMetaData;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.springframework.web.bind.annotation.ValueConstants.DEFAULT_NONE;

/**
 * Handles classes annotated with @RestController to establish a request path -> controller mapping relationship
 *
 * @see ControllerManager
 */
@Component
public class RestControllerBeanPostProcessor implements BeanPostProcessor {

    private static final List<Class<? extends Annotation>> MAPPING_CLASS = new ArrayList<>();
    private static final Map<Class<? extends Annotation>, ParamMetaData.ParamConvertType> MAPPING_PARAM_TYPE =
            new HashMap<>();
    private static final Set<Class<?>> SIMPLE_TYPE = new HashSet<>();
    private static final Set<Class<?>> SPECIAL_INJECTED_TYPE = new HashSet<>();

    static {
        MAPPING_CLASS.add(GetMapping.class);
        MAPPING_CLASS.add(PostMapping.class);
        MAPPING_CLASS.add(RequestMapping.class);
        MAPPING_CLASS.add(PutMapping.class);
        MAPPING_CLASS.add(DeleteMapping.class);

        MAPPING_PARAM_TYPE.put(RequestParam.class, ParamMetaData.ParamConvertType.REQUEST_PARAM);
        MAPPING_PARAM_TYPE.put(RequestBody.class, ParamMetaData.ParamConvertType.REQUEST_BODY);
        MAPPING_PARAM_TYPE.put(ModelAttribute.class, ParamMetaData.ParamConvertType.MODEL_ATTRIBUTE);

        SIMPLE_TYPE.add(String.class);
        SIMPLE_TYPE.add(Integer.class);
        SIMPLE_TYPE.add(int.class);
        SIMPLE_TYPE.add(Long.class);
        SIMPLE_TYPE.add(long.class);
        SIMPLE_TYPE.add(Boolean.class);
        SIMPLE_TYPE.add(boolean.class);
        SIMPLE_TYPE.add(Double.class);
        SIMPLE_TYPE.add(double.class);
        SIMPLE_TYPE.add(Float.class);
        SIMPLE_TYPE.add(float.class);
        SIMPLE_TYPE.add(Short.class);
        SIMPLE_TYPE.add(short.class);
        SIMPLE_TYPE.add(Byte.class);
        SIMPLE_TYPE.add(byte.class);
        SIMPLE_TYPE.add(Character.class);
        SIMPLE_TYPE.add(char.class);
        SIMPLE_TYPE.add(java.math.BigDecimal.class);
        SIMPLE_TYPE.add(java.math.BigInteger.class);
        SIMPLE_TYPE.add(java.util.Date.class);
        SIMPLE_TYPE.add(java.time.LocalDate.class);
        SIMPLE_TYPE.add(java.time.LocalDateTime.class);

        SPECIAL_INJECTED_TYPE.add(org.apache.seata.common.rpc.http.HttpContext.class);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!bean.getClass().isAnnotationPresent(RestController.class)) {
            return bean;
        }

        Class<?> httpControllerClass = bean.getClass();
        RequestMapping requestMapping = httpControllerClass.getAnnotation(RequestMapping.class);
        List<String> prePaths;
        if (requestMapping != null) {
            prePaths = Arrays.asList(requestMapping.value());
        } else {
            prePaths = new ArrayList<>();
        }
        Method[] methods = httpControllerClass.getDeclaredMethods();
        for (Method method : methods) {
            for (Class<? extends Annotation> annotationType : MAPPING_CLASS) {
                Annotation annotation = method.getAnnotation(annotationType);
                if (annotation != null) {
                    List<String> postPaths = getAnnotationValue(annotation);
                    addPathMapping(bean, prePaths, method, postPaths);
                }
            }
        }

        return bean;
    }

    private static List<String> getAnnotationValue(Annotation annotation) {
        try {
            Class<? extends Annotation> annotationClass = annotation.annotationType();
            Method valueMethod = annotationClass.getMethod("value");
            valueMethod.setAccessible(true);
            return Arrays.asList((String[]) valueMethod.invoke(annotation));
        } catch (Throwable e) {
            return new ArrayList<>();
        }
    }

    private static void addPathMapping(
            Object httpController, List<String> prePaths, Method method, List<String> postPaths) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        ParamMetaData[] paramMetaDatas = new ParamMetaData[parameterTypes.length];
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameterTypes.length; i++) {
            Annotation matchedAnnotation = null;
            Class<? extends Annotation> parameterAnnotationType = null;
            if (parameterAnnotations[i] != null && parameterAnnotations[i].length > 0) {
                for (Annotation annotation : parameterAnnotations[i]) {
                    if (MAPPING_PARAM_TYPE.containsKey(annotation.annotationType())) {
                        parameterAnnotationType = annotation.annotationType();
                        matchedAnnotation = annotation;
                        break;
                    }
                }
            }
            ParamMetaData paramMetaData =
                    buildParamMetaData(matchedAnnotation, parameterTypes[i], parameterAnnotationType, parameters[i]);
            paramMetaDatas[i] = paramMetaData;
        }
        int maxSize = Math.max(prePaths.size(), postPaths.size());
        for (int i = prePaths.size(); i < maxSize; i++) {
            prePaths.add("/");
        }

        for (int i = postPaths.size(); i < maxSize; i++) {
            postPaths.add("/");
        }

        for (String prePath : prePaths) {
            for (String postPath : postPaths) {
                String fullPath = (prePath + "/" + postPath).replaceAll("(/)+", "/");
                HttpInvocation httpInvocation = new HttpInvocation();
                httpInvocation.setMethod(method);
                httpInvocation.setController(httpController);
                httpInvocation.setPath(fullPath);
                httpInvocation.setParamMetaData(paramMetaDatas);
                ControllerManager.addHttpInvocation(httpInvocation);
            }
        }
    }

    private static ParamMetaData buildParamMetaData(
            Annotation matchedAnnotation,
            Class<?> parameterType,
            Class<? extends Annotation> parameterAnnotationType,
            Parameter parameter) {
        ParamMetaData paramMetaData = new ParamMetaData();

        // No annotation on the parameter: resolve the default annotation type based on the parameter type
        if (parameterAnnotationType == null) {
            parameterAnnotationType = resolveDefaultAnnotationType(parameterType);
            ParamMetaData.ParamConvertType paramConvertType = MAPPING_PARAM_TYPE.get(parameterAnnotationType);
            paramMetaData.setParamConvertType(paramConvertType);
            if (parameterAnnotationType == RequestParam.class) {
                paramMetaData.setParamName(parameter.getName());
                paramMetaData.setRequired(true);
                paramMetaData.setDefaultValue(DEFAULT_NONE);
            }
            // Annotation is present on the parameter; proceed with standard parsing logic
        } else {
            ParamMetaData.ParamConvertType paramConvertType = MAPPING_PARAM_TYPE.get(parameterAnnotationType);
            paramMetaData.setParamConvertType(paramConvertType);
            if (parameterAnnotationType == RequestParam.class) {
                RequestParam requestParam = (RequestParam) matchedAnnotation;
                boolean required = true;
                String defaultValue = null;
                String paramName = Optional.ofNullable(requestParam.name())
                        .filter(name -> !name.isEmpty())
                        .orElseGet(() -> {
                            String value = requestParam.value();
                            return !value.isEmpty() ? value : parameter.getName();
                        });

                required = requestParam.required();
                defaultValue = requestParam.defaultValue();

                if (!DEFAULT_NONE.equals(defaultValue)) {
                    required = false;
                }

                paramMetaData.setParamName(paramName);
                paramMetaData.setRequired(required);
                paramMetaData.setDefaultValue(defaultValue);
            }
        }

        return paramMetaData;
    }

    /**
     * Determines the default annotation type for a parameter based on its class.
     * Returns:
     * - null for special injected types (e.g., HttpContext),
     * - RequestParam for primitives, simple types, or MultipartFile,
     * - ModelAttribute for all others.
     */
    private static Class<? extends Annotation> resolveDefaultAnnotationType(Class<?> paramType) {
        if (SPECIAL_INJECTED_TYPE.stream().anyMatch(t -> t.isAssignableFrom(paramType))) {
            return null;
        } else if (paramType.isPrimitive()
                || SIMPLE_TYPE.contains(paramType)
                || org.springframework.web.multipart.MultipartFile.class.isAssignableFrom(paramType)) {
            return RequestParam.class;
        } else {
            return ModelAttribute.class;
        }
    }
}
