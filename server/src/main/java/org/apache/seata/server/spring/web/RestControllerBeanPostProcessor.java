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
package org.apache.seata.server.spring.web;

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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RestControllerBeanPostProcessor implements BeanPostProcessor {

    private static final List<Class<? extends Annotation>> MAPPING_CLASS = new ArrayList<>();
    private static final Map<Class<? extends Annotation>, ParamMetaData.ParamConvertType> MAPPING_PARAM_TYPE = new HashMap<>();

    static {
        MAPPING_CLASS.add(GetMapping.class);
        MAPPING_CLASS.add(PostMapping.class);
        MAPPING_CLASS.add(RequestMapping.class);
        MAPPING_CLASS.add(PutMapping.class);
        MAPPING_CLASS.add(DeleteMapping.class);

        MAPPING_PARAM_TYPE.put(RequestParam.class, ParamMetaData.ParamConvertType.REQUEST_PARAM);
        MAPPING_PARAM_TYPE.put(RequestBody.class, ParamMetaData.ParamConvertType.REQUEST_BODY);
        MAPPING_PARAM_TYPE.put(ModelAttribute.class, ParamMetaData.ParamConvertType.MODEL_ATTRIBUTE);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!bean.getClass().isAnnotationPresent(RestController.class)) {
            return bean;
        }

        Class<?> httpControllerClass = bean.getClass();
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
                    addPathMapping(bean, prePaths, method, postPaths);
                }
            }
        }

        return bean;
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
        Class<?>[] parameterTypes = method.getParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        ParamMetaData[] paramMetaDatas = new ParamMetaData[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<? extends Annotation> parameterAnnotationType = null;
            if (parameterAnnotations[i] != null && parameterAnnotations[i].length > 0) {
                parameterAnnotationType = parameterAnnotations[i][0].annotationType();
            }

            if (parameterAnnotationType == null) {
                parameterAnnotationType = RequestParam.class;
            }

            ParamMetaData paramMetaData = new ParamMetaData();
            ParamMetaData.ParamConvertType paramConvertType = MAPPING_PARAM_TYPE.get(parameterAnnotationType);
            paramMetaData.setParamConvertType(paramConvertType);
            paramMetaDatas[i] = paramMetaData;
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


}
