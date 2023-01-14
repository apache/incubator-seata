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
package io.seata.integrationapi.util;

import io.seata.integrationapi.json.DefaultJsonParser;

/**
 * @author leezongjie
 * @date 2023/1/13
 */
public class JsonUtil {

    public static String toJSONString(Object object) {
        return DefaultJsonParser.get().toJSONString(object);
    }

    public static <T> T parseObject(String text, Class<T> clazz) {
        return DefaultJsonParser.get().parseObject(text, clazz);
    }

}
