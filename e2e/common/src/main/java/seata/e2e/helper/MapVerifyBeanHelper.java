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

package seata.e2e.helper;

import java.lang.reflect.Method;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verify whether the value in the map is equal to the corresponding field in an object
 *
 * @author jingliu_xiong@foxmail.com
 */
public class MapVerifyBeanHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(PressureTask.class);
    private Map<String, Object> map;
    private Object object;

    public MapVerifyBeanHelper(Map<String, Object> map, Object object) {
        this.map = map;
        this.object = object;
    }

    /**
     * Gets the field value based on the field name
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
     * Use the value of toString () to determine whether all values of the map are equal
     * to the corresponding field in the bean.
     * @return
     */
    public boolean mapAllEqualBeanSome() {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object field = getFieldValueByName(key, object);
            if (field == null) {
                LOGGER.warn("this field {} not exists.", key);
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
     * Use the value of toString () to determine whether a value of the map is equal
     * to the corresponding field in the bean.
     * @return
     */
    public boolean mapOneEqualBeanOne(String key) {
        Object o = map.get(key);
        Object field = getFieldValueByName(key, object);
        if (field == null) {
            LOGGER.warn("this field {} not exists.", key);
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
