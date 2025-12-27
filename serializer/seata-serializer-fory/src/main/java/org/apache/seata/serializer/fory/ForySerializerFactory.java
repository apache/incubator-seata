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

import org.apache.fory.Fory;
import org.apache.fory.ThreadLocalFory;
import org.apache.fory.ThreadSafeFory;
import org.apache.fory.config.CompatibleMode;
import org.apache.fory.config.Language;
import org.apache.seata.core.serializer.SerializerSecurityRegistry;

public class ForySerializerFactory {
    private static final ForySerializerFactory FACTORY = new ForySerializerFactory();

    private static final ThreadSafeFory FORY = new ThreadLocalFory(classLoader -> {
        Fory f = Fory.builder()
                .withLanguage(Language.JAVA)
                // In JAVA mode, classes cannot be registered by tag, and the different registration order between the
                // server and the client will cause deserialization failure
                // In XLANG cross-language mode has problems with Java class serialization, such as enum classes
                // [https://github.com/apache/fory/issues/1644].
                .requireClassRegistration(false)
                // enable reference tracking for shared/circular reference.
                .withRefTracking(true)
                .withClassLoader(classLoader)
                .withCompatibleMode(CompatibleMode.COMPATIBLE)
                .build();

        // register allow class
        f.getClassResolver()
                .setClassChecker((classResolver, className) ->
                        SerializerSecurityRegistry.getAllowClassPattern().contains(className));
        return f;
    });

    public static ForySerializerFactory getInstance() {
        return FACTORY;
    }

    public ThreadSafeFory get() {
        return FORY;
    }
}
