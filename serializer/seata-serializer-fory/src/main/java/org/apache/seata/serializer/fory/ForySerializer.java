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
package org.apache.seata.serializer.fory;

import org.apache.fory.ThreadSafeFory;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.core.protocol.AbstractMessage;
import org.apache.seata.core.serializer.Serializer;

@LoadLevel(name = "FORY")
public class ForySerializer implements Serializer {
    @Override
    public <T> byte[] serialize(T t) {
        if (!(t instanceof AbstractMessage)) {
            throw new IllegalArgumentException("AbstractMessage isn't available.");
        }

        ThreadSafeFory threadSafeFory = ForySerializerFactory.getInstance().get();
        return threadSafeFory.serialize(t);
    }

    @Override
    public <T> T deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("bytes is null");
        }
        ThreadSafeFory threadSafeFory = ForySerializerFactory.getInstance().get();
        return (T) threadSafeFory.deserialize(bytes);
    }
}
