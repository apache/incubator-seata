package com.alibaba.fescar.rm.tcc.interceptor;


import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fescar.common.exception.FrameworkException;
import com.alibaba.fescar.common.util.StringUtils;
import com.alibaba.fescar.rm.tcc.api.BusinessActionContextParameter;

/**
 * 从方法中提取TCC上下文信息
 */
public class ActionContextUtil {

    /**
     * 从参数中提取数据
     * @param targetParam
     * @param filter
     * @return
     */
    public static Map<String, Object> fetchContextFromObject(Object targetParam, ActionContextFilter filter) {
        try {
            Map<String, Object> context = new HashMap<String, Object>();
            List<Field> fields = new ArrayList<Field>();
            getAllField(targetParam.getClass(), fields);
            for (Field f : fields) {
                String fieldName = f.getName();
                //1、获取属性上的指定类型的注释
                Annotation annotation = f.getAnnotation(BusinessActionContextParameter.class);

                //打了注解
                if (annotation != null) {
                    BusinessActionContextParameter param = (BusinessActionContextParameter) annotation;
                    if (filter != null && filter.needFilter(param)) {
                        continue;
                    }
                    f.setAccessible(true);
                    Object paramObject = f.get(targetParam);
                    int index = param.index();
                    //如果是，则找到特定参数
                    if (index >= 0) {
                        @SuppressWarnings("unchecked")
						Object targetObject = ((List<Object>) paramObject).get(index);
                        if (param.isParamInProperty()) {
                            context.putAll(fetchContextFromObject(targetObject, null));
                        } else {
                            if (StringUtils.isBlank(param.paramName())) {
                                context.put(fieldName, paramObject);
                            } else {
                                context.put(param.paramName(), paramObject);
                            }
                        }
                    } else {
                        if (param.isParamInProperty()) {
                            context.putAll(fetchContextFromObject(paramObject, null));
                        } else {
                            if (StringUtils.isBlank(param.paramName())) {
                                context.put(fieldName, paramObject);
                            } else {
                                context.put(param.paramName(), paramObject);
                            }
                        }
                    }
                }
            }
            return context;
        } catch (Throwable t) {
            throw new FrameworkException(t, "fetchContextFromObject failover");
        }
    }

    public static void getAllField(Class<?> interFace, List<Field> fields) {
        if (interFace == Object.class || interFace.isInterface()) {
            return;
        }
        Field[] field = interFace.getDeclaredFields();
        if (field != null) {
            fields.addAll(Arrays.asList(field));
        }
        getAllField(interFace.getSuperclass(), fields);
    }


}
