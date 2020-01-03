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
package io.seata.saga.engine.serializer.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import io.seata.saga.engine.serializer.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exception serializer
 *
 * @author lorne.cl
 */
public class ExceptionSerializer implements Serializer<Exception, byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionSerializer.class);

    public static byte[] serializeByObjectOutput(Object o) {

        byte[] result = null;
        if (o != null) {
            ObjectOutputStream oos = null;
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                oos = new ObjectOutputStream(baos);
                oos.writeObject(o);
                oos.flush();
                result = baos.toByteArray();
            } catch (IOException e) {
                LOGGER.error("serializer failed: {}", o.getClass(), e);
                throw new RuntimeException("IO Create Error", e);
            } finally {
                if (oos != null) {
                    try {
                        oos.close();
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            }
        }
        return result;
    }

    public static <T> T deserializeByObjectInputStream(byte[] bytes, Class<T> valueType) {

        if (bytes == null) {
            return null;
        }

        Object result = deserializeByObjectInputStream(bytes);
        return valueType.cast(result);
    }

    public static Object deserializeByObjectInputStream(byte[] bytes) {

        Object result = null;
        if (bytes != null) {
            ObjectInputStream ois = null;
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                ois = new ObjectInputStream(bais);
                result = ois.readObject();
            } catch (IOException e) {
                LOGGER.error("deserialize failed:", e);
                throw new RuntimeException("IO Create Error", e);
            } catch (ClassNotFoundException e) {
                LOGGER.error("deserialize failed:", e);
                throw new RuntimeException("Cannot find specified class", e);
            } finally {
                if (ois != null) {
                    try {
                        ois.close();
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public byte[] serialize(Exception object) {

        return serializeByObjectOutput(object);
    }

    @Override
    public Exception deserialize(byte[] bytes) {
        return deserializeByObjectInputStream(bytes, Exception.class);
    }
}
