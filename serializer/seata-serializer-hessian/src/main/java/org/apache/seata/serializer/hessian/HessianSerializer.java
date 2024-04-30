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
package org.apache.seata.serializer.hessian;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.core.serializer.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@LoadLevel(name = "HESSIAN")
@Deprecated
public class HessianSerializer implements Serializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(HessianSerializer.class);

    @Override
    public <T> byte[] serialize(T t) {
        byte[] stream = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Hessian2Output output = new Hessian2Output(baos);
            output.setSerializerFactory(HessianSerializerFactory.getInstance());
            output.writeObject(t);
            output.close();
            stream = baos.toByteArray();
        } catch (IOException e) {
            LOGGER.error("Hessian encode error:{}", e.getMessage(), e);
        }
        return stream;
    }

    @Override
    public <T> T deserialize(byte[] bytes) {
        T obj = null;
        try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
            Hessian2Input input = new Hessian2Input(is);
            input.setSerializerFactory(HessianSerializerFactory.getInstance());
            obj = (T)input.readObject();
            input.close();
        } catch (IOException e) {
            LOGGER.error("Hessian decode error:{}", e.getMessage(), e);
        }
        return obj;
    }
}
