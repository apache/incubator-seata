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
package org.apache.seata.serializer.kryo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Objects;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;


public class KryoInnerSerializer implements AutoCloseable {

    private final Kryo kryo;

    public KryoInnerSerializer(Kryo kryo) {
        this.kryo = Objects.requireNonNull(kryo);
    }

    public Kryo getKryo() {
        return kryo;
    }

    public <T> byte[] serialize(T t) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryo.writeClassAndObject(output, t);
        output.close();
        return baos.toByteArray();
    }

    public <T> T deserialize(byte[] bytes) {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        Input input = new Input(bais);
        input.close();
        return (T) kryo.readClassAndObject(input);
    }

    @Override
    public void close() {
        KryoSerializerFactory.getInstance().returnKryo(this);
    }

}
