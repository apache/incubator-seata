/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aot.hint;

import java.io.Serializable;

/**
 * SerializationHints's fake
 *
 * @author Stephane Nicoll
 * @since 6.0
 * @see Serializable
 */
public class SerializationHints {

//    public Stream<JavaSerializationHint> javaSerializationHints() {
//        return null;
//    }
//
//    public SerializationHints registerType(TypeReference type, @Nullable Consumer<JavaSerializationHint.Builder> serializationHint) {
//        return this;
//    }

    public SerializationHints registerType(TypeReference type) {
        return this;
    }

//    public SerializationHints registerType(Class<? extends Serializable> type, @Nullable Consumer<JavaSerializationHint.Builder> serializationHint) {
//        return this;
//    }

    public SerializationHints registerType(Class<? extends Serializable> type) {
        return this;
    }

}
