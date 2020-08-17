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

import io.seata.common.util.ClassUtils;

/**
 * @author wang.liang
 */
class AutoDefaultValues {

    private AutoDefaultValues() {
    }

    public static String autoDefaultSagaJsonParser() {
        if (hasFastjson()) {
            return "fastjson";
        }
        if (hasJackson()) {
            return "jackson";
        }
        return "fastjson";
    }

    public static String autoDefaultUndoLogSerialization() {
        if (hasJackson()) {
            return "jackson";
        }
        if (hasFastjson()) {
            return "fastjson";
        }
        if (hasKryo()) {
            return "kryo";
        }
        if (hasProtostuff()) {
            return "protostuff";
        }
        return "jackson";
    }

    private static boolean hasFastjson() {
        return ClassUtils.isPresent("com.alibaba.fastjson.JSON");
    }

    private static boolean hasJackson() {
        return ClassUtils.isPresent("com.fasterxml.jackson.core.JsonParser");
    }

    private static boolean hasKryo() {
        return ClassUtils.isPresent("com.esotericsoftware.kryo.Kryo");
    }

    private static boolean hasProtostuff() {
        return ClassUtils.isPresent("io.protostuff.Schema");
    }
}
