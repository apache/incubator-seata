/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.core.context;

/**
 * The interface Context core.
 */
public interface ContextCore {

    /**
     * Put string.
     *
     * @param key   the key
     * @param value the value
     * @return the string
     */
    String put(String key, String value);

    /**
     * Get string.
     *
     * @param key the key
     * @return the string
     */
    String get(String key);

    /**
     * Remove string.
     *
     * @param key the key
     * @return the string
     */
    String remove(String key);
}
