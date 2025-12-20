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
package org.apache.seata.serializer.fury;

import org.apache.seata.core.serializer.Serializer;

public class DynamicSerializerFactory {
    private static final DynamicSerializerFactory FACTORY = new DynamicSerializerFactory();

    private final Serializer furyDelegate;

    private DynamicSerializerFactory() {
        this.furyDelegate = createFuryDelegate();
    }

    public static DynamicSerializerFactory getInstance() {
        return FACTORY;
    }

    public Serializer get() {
        return furyDelegate;
    }

    private Serializer createFuryDelegate() {
        // First, try to load seata-serializer-fory.
        try {
            Class.forName("org.apache.seata.serializer.fory.ForySerializer");
            return new ForySerializerDelegate();
        } catch (ClassNotFoundException e) {
        }
        // Second, try to load Apache Fory.
        try {
            Class.forName("org.apache.fory.Fury");
            return new ThreadSafeForyDelegate();
        } catch (ClassNotFoundException e) {
        }
        // If Fory is unavailable, fallback to Fury.
        try {
            Class.forName("org.apache.fury.Fury");
            return new ThreadSafeFuryDelegate();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Neither Apache Fory nor Apache Fury found in classpath", e);
        }
    }
}
