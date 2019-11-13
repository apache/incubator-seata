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
package io.seata.saga.engine.store.utils;

import java.lang.reflect.Field;

import io.seata.common.util.ReflectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bean utils
 *
 * @author lorne.cl
 */
public class BeanUtils {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BeanUtils.class);

    public static String beanToString(Object o) {
        if (o == null) {
            return null;
        }

        Field[] fields = o.getClass().getDeclaredFields();
        StringBuffer buffer = new StringBuffer();
        buffer.append("[");
        for (Field field : fields) {
            Object val = null;
            try {
                val = ReflectionUtil.getFieldValue(o, field.getName());
            } catch (NoSuchFieldException e) {
                LOGGER.warn(e.getMessage(), e);
            } catch (IllegalAccessException e) {
                LOGGER.warn(e.getMessage(), e);
            }
            if (val != null) {
                buffer.append(field.getName()).append("=").append(val).append(", ");
            }
        }
        if (buffer.length() > 2) {
            buffer.delete(buffer.length() - 2, buffer.length());
        }
        buffer.append("]");
        return buffer.toString();
    }
}