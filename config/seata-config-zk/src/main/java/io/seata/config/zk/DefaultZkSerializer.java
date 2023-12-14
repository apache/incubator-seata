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
package io.seata.config.zk;

import java.nio.charset.StandardCharsets;

/**
 * Default zk serializer.
 * <p>
 * If the user is not configured in config.zk.serializer configuration item, then use default serializer.
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.3.0
 */
public class DefaultZkSerializer {

    public static byte[] serialize(Object data) {
        return String.valueOf(data).getBytes(StandardCharsets.UTF_8);
    }

    public static String deserialize(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
