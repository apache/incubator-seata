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
package io.seata.codec.seata;

import io.seata.common.loader.LoadLevel;
import io.seata.core.codec.Codec;

/**
 * The type Seata codec.
 *
 * @author zhangsen
 * @data 2019 /5/6
 */
@LoadLevel(name="seata", order = 0)
public class SeataCodec implements Codec {

    @Override
    public <T> byte[] encode(T t) {
        return new byte[0];
    }

    @Override
    public <T> T decode(byte[] bytes) {
        return null;
    }

}
