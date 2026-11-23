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
package org.apache.seata.common.json;

/**
 * Minimal JSON codec SPI for common module code that cannot depend on json-common-core.
 */
public interface JsonCodec {

    /**
     * Serialize the given object to JSON string.
     *
     * @param object the object to serialize
     * @return JSON string
     */
    String toJSONString(Object object);

    /**
     * Deserialize JSON string to the given type.
     *
     * @param text JSON string
     * @param clazz target class
     * @param <T> target type
     * @return parsed object
     */
    <T> T parseObject(String text, Class<T> clazz);
}
