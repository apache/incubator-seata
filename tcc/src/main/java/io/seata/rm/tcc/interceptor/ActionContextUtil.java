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
package io.seata.rm.tcc.interceptor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import com.alibaba.fastjson.JSON;
import io.seata.common.exception.FrameworkException;
import io.seata.common.util.StringUtils;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;

/**
 * Extracting TCC Context from Method
 *
 * @author zhangsen
 */
public final class ActionContextUtil {

    private ActionContextUtil() {
    }

    /**
     * Extracting context data from parameters
     *
     * @param targetParam the target param
     * @return map map
     */
    public static Map<String, Object> fetchContextFromObject(Object targetParam) {
        try {
            Map<String, Object> context = new HashMap<>(8);
            List<Field> fields = new ArrayList<>();
            getAllField(targetParam.getClass(), fields);

            for (Field f : fields) {
                String fieldName = f.getName();
                BusinessActionContextParameter annotation = f.getAnnotation(BusinessActionContextParameter.class);

                if (annotation != null) {
                    f.setAccessible(true);
                    Object paramObject = f.get(targetParam);
                    int index = annotation.index();
                    if (annotation.isParamInProperty()) {
                        if (index >= 0) {
                            @SuppressWarnings("unchecked")
                            Object targetObject = ((List<Object>) paramObject).get(index);
                            context.putAll(fetchContextFromObject(targetObject));
                        } else {
                            context.putAll(fetchContextFromObject(paramObject));
                        }
                    } else {
                        String paramName = annotation.paramName();
                        if (StringUtils.isBlank(paramName)) {
                            paramName = fieldName;
                        }
                        context.put(paramName, paramObject);
                    }
                }
            }
            return context;
        } catch (Throwable t) {
            throw new FrameworkException(t, "fetchContextFromObject failover");
        }
    }

    /**
     * Gets all field.
     *
     * @param interFace the inter face
     * @param fields    the fields
     */
    public static void getAllField(Class<?> interFace, List<Field> fields) {
        if (interFace == Object.class || interFace.isInterface()) {
            return;
        }
        Field[] field = interFace.getDeclaredFields();
        fields.addAll(Arrays.asList(field));
        getAllField(interFace.getSuperclass(), fields);
    }

    /**
     * put the action context after handle
     *
     * @param actionContext the action context
     * @param key           the actionContext's key
     * @param value         the actionContext's key
     * @return the action context is changed
     */
    public static boolean putActionContext(Map<String, Object> actionContext, String key, Object value) {
        if (value == null) {
            return false;
        }

        value = handleActionContext(value);
        Object previousValue = actionContext.put(key, value);
        return value != previousValue;
    }

    /**
     * put the action context after handle
     *
     * @param actionContext    the action context
     * @param actionContextMap the actionContextMap
     * @return the action context is changed
     */
    public static boolean putActionContext(Map<String, Object> actionContext, Map<String, Object> actionContextMap) {
        boolean isUpdated = false;
        for (Map.Entry<String, Object> entry : actionContextMap.entrySet()) {
            if (putActionContext(actionContext, entry.getKey(), entry.getValue())) {
                isUpdated = true;
            }
        }
        return isUpdated;
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
        if (actionContext instanceof CharSequence || actionContext instanceof Number || actionContext instanceof Boolean) {
            return actionContext;
        } else {
            return JSON.toJSONString(actionContext);
        }
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
    public static <T> T convertActionContext(String key, Object value, @Nonnull Class<T> targetClazz) {
        if (value == null) {
            return null;
        }

        // same class, or super class, can cast directly
        if (targetClazz.isAssignableFrom(value.getClass())) {
            return (T)value;
        }

        // String class
        if (String.class.equals(targetClazz)) {
            return (T)value.toString();
        }

        try {
            try {
                return (T)value;
            } catch (ClassCastException ignore) {
                return JSON.parseObject(value.toString(), targetClazz);
            }
        } catch (RuntimeException e) {
            String errorMsg = String.format("Failed to convert the action context with key '%s' from '%s' to '%s'.",
                    key, value.getClass().getName(), targetClazz.getName());
            throw new FrameworkException(e, errorMsg);
        }
    }
}
