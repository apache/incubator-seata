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
package org.apache.seata.core.rpc.netty.http;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

public class ControllerManager {
    private static final Map<String, Object> HTTP_CONTROLLER_MAP = new ConcurrentHashMap<>();

    private static final Map<String, Method> REQUEST_METHOD_MAP = new ConcurrentHashMap<>();

    private static final List<Class<? extends Annotation>> MAPPING_CLASS = new ArrayList<>();

    static {
        MAPPING_CLASS.add(GetMapping.class);
        MAPPING_CLASS.add(PostMapping.class);
        MAPPING_CLASS.add(RequestMapping.class);
        MAPPING_CLASS.add(PutMapping.class);
        MAPPING_CLASS.add(DeleteMapping.class);
    }

    public static Object getHttpController(String path) {
        return HTTP_CONTROLLER_MAP.get(path);
    }

    public static Method getHandleMethod(String path) {
        return REQUEST_METHOD_MAP.get(path);
    }

    public static void addHttpController(Object httpController) {
        Class<?> httpControllerClass = httpController.getClass();
        RequestMapping requestMapping = httpControllerClass.getAnnotation(RequestMapping.class);
        String[] prePaths;
        if (requestMapping != null) {
            prePaths = requestMapping.value();
        } else {
            prePaths = new String[]{""};
        }
        Method[] methods = httpControllerClass.getMethods();
        for (Method method : methods) {
            for (Class<? extends Annotation> annotationType : MAPPING_CLASS) {
                Annotation annotation = method.getAnnotation(annotationType);
                if (annotation != null) {
                    String[] postPaths = getAnnotationValue(annotation);
                    addPathMapping(httpController, prePaths, method, postPaths);
                }
            }
        }
    }

    private static String[] getAnnotationValue(Annotation annotation) {
        try {
            Class<? extends Annotation> annotationClass = annotation.getClass();
            Field valueField = annotationClass.getDeclaredField("value");
            valueField.setAccessible(true);
            return (String[]) valueField.get(annotation);
        } catch (Throwable e) {
            return new String[]{};
        }
    }

    private static void addPathMapping(Object httpController, String[] prePaths, Method method, String[] postPaths) {
        for (String prePath : prePaths) {
            for (String postPath : postPaths) {
                REQUEST_METHOD_MAP.put((prePath + "/" + postPath).replaceAll("(/)+", "/"), method);
                HTTP_CONTROLLER_MAP.put((prePath + "/" + postPath).replaceAll("(/)+", "/"), httpController);
            }
        }
    }
}