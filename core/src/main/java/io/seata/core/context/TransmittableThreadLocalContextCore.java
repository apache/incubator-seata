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

import com.alibaba.ttl.TransmittableThreadLocal;
import io.seata.common.loader.LoadLevel;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * The type TransmittableThreadLocalContextCore context core.
 *
 * @author linkedme@qq.com
 */
@LoadLevel(name = "TransmittableThreadLocalContextCore", order = Integer.MIN_VALUE + 2)
public class TransmittableThreadLocalContextCore implements ContextCore {

    private TransmittableThreadLocal<Map<String, Object>> transmittableThreadLocal =
            new TransmittableThreadLocal<Map<String, Object>>() {
                @Override
                protected Map<String, Object> initialValue() {
                    return new HashMap<>();
                }
            };

    @Nullable
    @Override
    public Object put(String key, Object value) {
        return transmittableThreadLocal.get().put(key, value);
    }

    @Nullable
    @Override
    public Object get(String key) {
        return transmittableThreadLocal.get().get(key);
    }

    @Nullable
    @Override
    public Object remove(String key) {
        return transmittableThreadLocal.get().remove(key);
    }

    @Override
    public Map<String, Object> entries() {
        return transmittableThreadLocal.get();
    }
}
