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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.util.Pool;
import org.apache.seata.core.serializer.SerializerSecurityRegistry;


public class KryoSerializerFactory {

    private static final KryoSerializerFactory FACTORY = new KryoSerializerFactory();

    private Pool<Kryo> pool = new Pool<Kryo>(true, true) {

        @Override
        protected Kryo create() {
            Kryo kryo = new Kryo();
            kryo.setReferences(true);

            //Serialization whitelist
            kryo.setRegistrationRequired(true);

            // register allow class
            SerializerSecurityRegistry.getAllowClassType().forEach(kryo::register);
            return kryo;
        }
    };

    private KryoSerializerFactory() {}

    public static KryoSerializerFactory getInstance() {
        return FACTORY;
    }

    public KryoInnerSerializer get() {
        return new KryoInnerSerializer(pool.obtain());
    }

    public void returnKryo(KryoInnerSerializer kryoSerializer) {
        if (kryoSerializer == null) {
            throw new IllegalArgumentException("kryoSerializer is null");
        }
        pool.free(kryoSerializer.getKryo());
    }

}
