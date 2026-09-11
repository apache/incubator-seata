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
package io.seata.rm.datasource.undo.parser;

import java.lang.reflect.InvocationHandler;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;
import de.javakaffee.kryoserializers.JdkProxySerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jsbxyyx
 */
public class KryoSerializerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(KryoSerializerFactory.class);

    private static final KryoSerializerFactory FACTORY = new KryoSerializerFactory();

    private Pool<Kryo> pool = new Pool<Kryo>(true, true) {

        @Override
        public Kryo create() {
            Kryo kryo = new Kryo();
            kryo.setReferences(true);
            kryo.setRegistrationRequired(false);

            for (Map.Entry<Class, Serializer> entry : TYPE_MAP.entrySet()) {
                kryo.register(entry.getKey(), entry.getValue());
            }

            // support clob and blob
            kryo.register(SerialBlob.class, new BlobSerializer());
            kryo.register(SerialClob.class, new ClobSerializer());

            // register sql type
            kryo.register(Timestamp.class, new TimestampSerializer());
            kryo.register(InvocationHandler.class, new JdkProxySerializer());
            // register commonly class
            UndoLogSerializerClassRegistry.getRegisteredClasses().forEach((clazz, ser) -> {
                if (ser == null) {
                    kryo.register(clazz);
                } else {
                    kryo.register(clazz, (Serializer)ser);
                }
            });
            return kryo;
        }

    };

    private static final Map<Class, Serializer> TYPE_MAP = new ConcurrentHashMap<>();

    private KryoSerializerFactory() {}

    public static KryoSerializerFactory getInstance() {
        return FACTORY;
    }

    public KryoSerializer get() {
        return new KryoSerializer(pool.obtain());
    }

    public void returnKryo(KryoSerializer kryoSerializer) {
        if (kryoSerializer == null) {
            throw new IllegalArgumentException("kryoSerializer is null");
        }
        pool.free(kryoSerializer.getKryo());
    }

    public void registerSerializer(Class type, Serializer ser) {
        if (type != null && ser != null) {
            TYPE_MAP.put(type, ser);
        }
    }

    private static class BlobSerializer extends Serializer<Blob> {

        @Override
        public void write(Kryo kryo, Output output, Blob object) {
            try {
                byte[] bytes = object.getBytes(1L, (int)object.length());
                output.writeInt(bytes.length, true);
                output.write(bytes);
            } catch (SQLException e) {
                LOGGER.error("kryo write java.sql.Blob error: {}", e.getMessage(), e);
            }
        }

        @Override
        public Blob read(Kryo kryo, Input input, Class<? extends Blob> type) {
            int length = input.readInt(true);
            byte[] bytes = input.readBytes(length);
            try {
                return new SerialBlob(bytes);
            } catch (SQLException e) {
                LOGGER.error("kryo read java.sql.Blob error: {}", e.getMessage(), e);
            }
            return null;
        }

    }

    private static class ClobSerializer extends Serializer<Clob> {

        @Override
        public void write(Kryo kryo, Output output, Clob object) {
            try {
                String s = object.getSubString(1, (int)object.length());
                output.writeString(s);
            } catch (SQLException e) {
                LOGGER.error("kryo write java.sql.Clob error: {}", e.getMessage(), e);
            }
        }

        @Override
        public Clob read(Kryo kryo, Input input, Class<? extends Clob> type) {
            try {
                String s = input.readString();
                return new SerialClob(s.toCharArray());
            } catch (SQLException e) {
                LOGGER.error("kryo read java.sql.Clob error: {}", e.getMessage(), e);
            }
            return null;
        }

    }

    private class TimestampSerializer extends Serializer<Timestamp> {
        @Override
        public void write(Kryo kryo, Output output, Timestamp object) {
            output.writeLong(object.getTime(), true);
            output.writeInt(object.getNanos(), true);
        }

        @Override
        public Timestamp read(Kryo kryo, Input input, Class<? extends Timestamp> type) {
            Timestamp timestamp = new Timestamp(input.readLong(true));
            timestamp.setNanos(input.readInt(true));
            return timestamp;
        }
    }
}