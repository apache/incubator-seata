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
package org.apache.seata.serializer.fastjson2;

import com.alibaba.fastjson2.JSONB;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.core.serializer.Serializer;

/**
 * @Author GoodBoyCoder
 */
@LoadLevel(name = "FASTJSON2")
public class Fastjson2Serializer implements Serializer {

    @Override
    public <T> byte[] serialize(T t) {
        return JSONB.toBytes(t, Fastjson2SerializerFactory.getInstance().getJsonWriterFeatureList());
    }

    @Override
    public <T> T deserialize(byte[] bytes) {
        return (T) JSONB.parseObject(bytes, Object.class, Fastjson2SerializerFactory.getInstance().getFilter(), Fastjson2SerializerFactory.getInstance().getJsonReaderFeatureList());
    }
}
