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
package io.seata.common;

import java.io.Serializable;

/**
 * The type ValueWrapper.
 *
 * @author wang.liang
 */
public class ValueWrapper implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final ValueWrapper NULL_VALUE_WRAPPER = new ValueWrapper(null);


    //region Class

    private final Object value;
    private final Serializable serializableValue;
    private final Class<?> type;


    private ValueWrapper(Object value) {
        this.value = value;
        this.type = value != null ? value.getClass() : null;
        this.serializableValue = value instanceof Serializable ? (Serializable)value : null;
    }


    public Object getValue() {
        return value;
    }

    public Class<?> getType() {
        return type;
    }

    public Serializable getSerializableValue() {
        return serializableValue;
    }

    //endregion


    //region static

    public static ValueWrapper create(Object value) {
        if (value instanceof ValueWrapper) {
            return (ValueWrapper)value;
        }

        if (value == null) {
            return NULL_VALUE_WRAPPER;
        }

        return new ValueWrapper(value);
    }

    public static boolean isNullValueWrapper(ValueWrapper valueWrapper) {
        return NULL_VALUE_WRAPPER == valueWrapper;
    }

    //endregion
}
