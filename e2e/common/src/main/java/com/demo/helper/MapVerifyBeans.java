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

package com.demo.helper;

import com.demo.helper.PressureTask;
import com.demo.model.HostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author xjl
 * @Description:
 */
public class MapVerifyBeans {

    private static final Logger LOGGER = LoggerFactory.getLogger(PressureTask.class);
    private Map<String, Object> map;
    private Object object;

    public MapVerifyBeans(Map<String, Object> map, Object object) {
        this.map = map;
        this.object = object;
    }

    /**
     * 根据属性名获取属性值
     */
    private Object getFieldValueByName(String fieldName, Object object) {
        try {
            String firstLetter = fieldName.substring(0, 1).toUpperCase();
            String getter = "get" + firstLetter + fieldName.substring(1);
            Method method = object.getClass().getMethod(getter, null);
            Object value = method.invoke(object, null);
            return value;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }


    /**
     * 利用toString()的值判断map的所有value是否与bean中的对应属性相等。
     *
     * @return
     */
    public boolean mapAllEqualBeanSome() {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object field = getFieldValueByName(key, object);
            if (field == null) {
                LOGGER.warn("This field {} not exists.", key);
                return false;
            } else {
                String mapValue = entry.getValue().toString();
                String fieldValue = field.toString();
                if (!fieldValue.equals(mapValue)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 利用toString()的值判断map的一个value是否与bean中的对应属性相等。
     *
     * @return
     */
    public boolean mapOneEqualBeanOne(String key) {
        Object o = map.get(key);
        Object field = getFieldValueByName(key, object);
        if (field == null) {
            LOGGER.warn("This field {} not exists.", key);
            return false;
        } else {
            String mapValue = o.toString();
            String fieldValue = field.toString();
            if (!fieldValue.equals(mapValue)) {
                return false;
            }
            return true;
        }
    }


}
