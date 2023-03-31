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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.springframework.lang.Nullable;

/**
 * ReflectionHints's fake
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public class ReflectionHints {

//    public Stream<TypeHint> typeHints() {
//        return null;
//    }
//
//    @Nullable
//    public TypeHint getTypeHint(TypeReference type) {
//        return null;
//    }
//
//    @Nullable
//    public TypeHint getTypeHint(Class<?> type) {
//        return null;
//    }
//
//    public ReflectionHints registerType(TypeReference type, Consumer<Builder> typeHint) {
//        return this;
//    }

    public ReflectionHints registerType(TypeReference type, MemberCategory... memberCategories) {
        return this;
    }

//    public ReflectionHints registerType(Class<?> type, Consumer<Builder> typeHint) {
//        return this;
//    }

    public ReflectionHints registerType(Class<?> type, MemberCategory... memberCategories) {
        return this;
    }

//    public ReflectionHints registerTypeIfPresent(@Nullable ClassLoader classLoader,
//            String typeName, Consumer<Builder> typeHint) {
//        return this;
//    }

    public ReflectionHints registerTypeIfPresent(@Nullable ClassLoader classLoader,
            String typeName, MemberCategory... memberCategories) {
        return this;
    }

//    public ReflectionHints registerTypes(Iterable<TypeReference> types, Consumer<Builder> typeHint) {
//        return this;
//    }

    public ReflectionHints registerField(Field field) {
        return this;
    }

    public ReflectionHints registerConstructor(Constructor<?> constructor, ExecutableMode mode) {
        return this;
    }

    public ReflectionHints registerMethod(Method method, ExecutableMode mode) {
        return this;
    }

}
