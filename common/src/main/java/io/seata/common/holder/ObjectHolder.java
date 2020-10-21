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
package io.seata.common.holder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.exception.ShouldNeverHappenException;

/**
 * @author xingfudeshi@gmail.com
 * The enum object holder
 */
public enum ObjectHolder {
    /**
     * singleton instance
     */
    INSTANCE;
    private static final int MAP_SIZE = 8;
    private static final Map<String, Object> OBJECT_MAP = new ConcurrentHashMap<>(MAP_SIZE);

    public Object getObject(String objectKey) {
        return OBJECT_MAP.get(objectKey);
    }

    public <T> T getObject(Class<T> clasz) {
        return clasz.cast(OBJECT_MAP.values().stream().filter(clasz::isInstance).findAny().orElseThrow(() -> new ShouldNeverHappenException("Can't find any object of class " + clasz.getName())));
    }

    /**
     * Sets object.
     *
     * @param objectKey the key
     * @param object    the object
     * @return the previous object with the key, or null
     */
    public Object setObject(String objectKey, Object object) {
        return OBJECT_MAP.putIfAbsent(objectKey, object);
    }
}
