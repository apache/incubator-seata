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
package org.apache.seata.saga.engine.serializer.impl;

import org.apache.seata.common.exception.ErrorCode;
import org.apache.seata.common.exception.SeataRuntimeException;
import org.apache.seata.saga.engine.serializer.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.util.Arrays;
import java.util.List;

/**
 * Exception serializer
 *
 */
public class ExceptionSerializer implements Serializer<Exception, byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionSerializer.class);

    private static final List<String> ALLOWED_CLASS_PREFIXES =
            Arrays.asList("java.", "javax.", "org.apache.seata.", "[B", "io.seata.", "org.springframework.");

    static boolean isClassAllowed(String className) {
        if (className.startsWith("[")) {
            String stripped = className;
            while (stripped.startsWith("[")) {
                stripped = stripped.substring(1);
            }
            if (stripped.length() == 1) {
                return true;
            }
            if (stripped.startsWith("L") && stripped.endsWith(";")) {
                return isClassAllowed(stripped.substring(1, stripped.length() - 1));
            }
            return false;
        }
        for (String prefix : ALLOWED_CLASS_PREFIXES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public static byte[] serializeByObjectOutput(Object o) {

        byte[] result = null;
        if (o != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(o);
                oos.flush();
                result = baos.toByteArray();
            } catch (IOException e) {
                LOGGER.error("serializer failed: {}", o.getClass(), e);
                throw new RuntimeException("IO Create Error", e);
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
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            try (ObjectInputStream ois = new ObjectInputStream(bais) {
                @Override
                protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                    if (!isClassAllowed(desc.getName())) {
                        throw new SeataRuntimeException(
                                ErrorCode.ERR_DESERIALIZATION_SECURITY,
                                "Failed to deserialize object: " + desc.getName() + " is not permitted");
                    }
                    return super.resolveClass(desc);
                }
            }) {
                result = ois.readObject();
            } catch (IOException e) {
                LOGGER.error("deserialize failed:", e);
                throw new RuntimeException("IO Create Error", e);
            } catch (ClassNotFoundException e) {
                LOGGER.error("deserialize failed:", e);
                throw new RuntimeException("Cannot find specified class", e);
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
