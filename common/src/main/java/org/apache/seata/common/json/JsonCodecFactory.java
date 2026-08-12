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

import org.apache.seata.common.exception.JsonParseException;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.loader.EnhancedServiceNotFoundException;

/**
 * Factory for the common-module JSON codec SPI.
 */
public final class JsonCodecFactory {

    private static volatile JsonCodec codec;

    private JsonCodecFactory() {}

    /**
     * Get the configured JSON codec provider.
     *
     * @return JSON codec provider
     */
    public static JsonCodec getCodec() {
        JsonCodec result = codec;
        if (result == null) {
            synchronized (JsonCodecFactory.class) {
                result = codec;
                if (result == null) {
                    try {
                        result = EnhancedServiceLoader.load(JsonCodec.class);
                        codec = result;
                    } catch (EnhancedServiceNotFoundException e) {
                        throw new JsonParseException(
                                "No JsonCodec provider found. Please add json-common-core to the runtime classpath.",
                                e);
                    }
                }
            }
        }
        return result;
    }
}
