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
package io.seata.integration.tx.api.interceptor;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.rm.tcc.api.ParamType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extracting TCC Context from Method
 */
@Deprecated
public final class ActionContextUtil {

    private ActionContextUtil() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionContextUtil.class);

    /**
     * Extracting context data from parameters
     *
     * @param targetParam the target param
     * @return map the context
     */
    public static Map<String, Object> fetchContextFromObject(@Nonnull Object targetParam) {
        return org.apache.seata.integration.tx.api.interceptor.ActionContextUtil.fetchContextFromObject(targetParam);
    }

    /**
     * load param by the config of annotation, and then put into the action context
     *
     * @param paramType     the param type, 'param' or 'field'
     * @param paramName     the param name
     * @param paramValue    the param value
     * @param annotation    the annotation on the param or field
     * @param actionContext the action context
     */
    public static void loadParamByAnnotationAndPutToContext(@Nonnull final ParamType paramType, @Nonnull String paramName, Object paramValue,
                                                            @Nonnull final BusinessActionContextParameter annotation, @Nonnull final Map<String, Object> actionContext) {
        if (paramValue == null) {
            return;
        }

        // If {@code index >= 0}, get by index from the list param or field
        int index = annotation.index();
        if (index >= 0) {
            paramValue = getByIndex(paramType, paramName, paramValue, index);
            if (paramValue == null) {
                return;
            }
        }

        // if {@code isParamInProperty == true}, fetch context from paramValue
        if (annotation.isParamInProperty()) {
            Map<String, Object> paramContext = fetchContextFromObject(paramValue);
            if (CollectionUtils.isNotEmpty(paramContext)) {
                actionContext.putAll(paramContext);
            }
        } else {
            // get param name from the annotation
            String paramNameFromAnnotation = getParamNameFromAnnotation(annotation);
            if (StringUtils.isNotBlank(paramNameFromAnnotation)) {
                paramName = paramNameFromAnnotation;
            }
            putActionContextWithoutHandle(actionContext, paramName, paramValue);
        }
    }

    @Nullable
    private static Object getByIndex(@Nonnull ParamType paramType, @Nonnull String paramName, @Nonnull Object paramValue, int index) {
        if (paramValue instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) paramValue;
            if (list.isEmpty()) {
                return null;
            }
            if (list.size() <= index) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("The index '{}' is out of bounds for the list {} named '{}'," +
                            " whose size is '{}', so pass this {}", index, paramType.getCode(), paramName, list.size(), paramType.getCode());
                }
                return null;
            }
            paramValue = list.get(index);
        } else {
            LOGGER.warn("the {} named '{}' is not a `List`, so the 'index' field of '@{}' cannot be used on it",
                    paramType.getCode(), paramName, BusinessActionContextParameter.class.getSimpleName());
        }

        return paramValue;
    }

    public static String getParamNameFromAnnotation(@Nonnull BusinessActionContextParameter annotation) {
        String paramName = annotation.paramName();
        if (StringUtils.isBlank(paramName)) {
            paramName = annotation.value();
        }
        return paramName;
    }

    /**
     * put the action context after handle
     *
     * @param actionContext the action context
     * @param key           the actionContext's key
     * @param value         the actionContext's value
     * @return the action context is changed
     */
    public static boolean putActionContext(Map<String, Object> actionContext, String key, Object value) {
        return org.apache.seata.integration.tx.api.interceptor.ActionContextUtil.putActionContext(actionContext, key, value);
    }

    /**
     * put the action context after handle
     *
     * @param actionContext    the action context
     * @param actionContextMap the actionContextMap
     * @return the action context is changed
     */
    public static boolean putActionContext(Map<String, Object> actionContext, @Nonnull Map<String, Object> actionContextMap) {
        return org.apache.seata.integration.tx.api.interceptor.ActionContextUtil.putActionContext(actionContext, actionContextMap);
    }

    /**
     * put the action context without handle
     *
     * @param actionContext the action context
     * @param key           the actionContext's key
     * @param value         the actionContext's value
     * @return the action context is changed
     */
    public static boolean putActionContextWithoutHandle(@Nonnull final Map<String, Object> actionContext, String key, Object value) {
        return org.apache.seata.integration.tx.api.interceptor.ActionContextUtil.putActionContextWithoutHandle(actionContext, key, value);
    }

    /**
     * put the action context without handle
     *
     * @param actionContext    the action context
     * @param actionContextMap the actionContextMap
     * @return the action context is changed
     */
    public static boolean putActionContextWithoutHandle(Map<String, Object> actionContext, @Nonnull Map<String, Object> actionContextMap) {
        return org.apache.seata.integration.tx.api.interceptor.ActionContextUtil.putActionContextWithoutHandle(actionContext, actionContextMap);
    }

    /**
     * Handle the action context.
     * It is convenient to convert type in phase 2.
     *
     * @param actionContext the action context
     * @return the action context or JSON string
     * @see #convertActionContext(String, Object, Class)
     * @see BusinessActionContext#getActionContext(String, Class)
     */
    public static Object handleActionContext(@Nonnull Object actionContext) {
        return org.apache.seata.integration.tx.api.interceptor.ActionContextUtil.handleActionContext(actionContext);
    }

    /**
     * Convert action context
     *
     * @param key         the actionContext's key
     * @param value       the actionContext's value
     * @param targetClazz the target class
     * @param <T>         the target type
     * @return the action context of the target type
     */
    @SuppressWarnings("unchecked")
    public static <T> T convertActionContext(String key, @Nullable Object value, @Nonnull Class<T> targetClazz) {
        return org.apache.seata.integration.tx.api.interceptor.ActionContextUtil.convertActionContext(key, value, targetClazz);
    }
}
