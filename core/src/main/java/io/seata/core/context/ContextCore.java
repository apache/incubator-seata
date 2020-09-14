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
package io.seata.core.context;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * The interface Context core.
 *
 * @author sharajava
 */
public interface ContextCore {

    /**
     * Put value.
     *
     * @param key   the key
     * @param value the value
     * @return the previous value associated with the key, or null if there was no mapping for the key
     */
    @Nullable
    Object put(String key, Object value);

    /**
     * Get value.
     *
     * @param key the key
     * @return the value
     */
    @Nullable
    Object get(String key);

    /**
     * Remove value.
     *
     * @param key the key
     * @return the removed value or null
     */
    @Nullable
    Object remove(String key);

    /**
     * entries
     *
     * @return the key-value map
     */
    Map<String, Object> entries();
}
