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
package org.springframework.aot.hint;

import org.springframework.lang.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * ReflectionHints's fake
 *
 * @author wang.liang
 */
public class ReflectionHints {

//    public Stream<TypeHint> typeHints() {
//        return this.types.values().stream().map(Builder::build);
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
        return null;
    }

//    public ReflectionHints registerTypeIfPresent(@Nullable ClassLoader classLoader, String typeName, Consumer<Builder> typeHint) {
//        return this;
//    }

    public ReflectionHints registerTypeIfPresent(@Nullable ClassLoader classLoader, String typeName, MemberCategory... memberCategories) {
        return null;
    }

//    public ReflectionHints registerTypes(Iterable<TypeReference> types, Consumer<Builder> typeHint) {
//        return this;
//    }

    public ReflectionHints registerField(Field field) {
        return null;
    }

    public ReflectionHints registerConstructor(Constructor<?> constructor, ExecutableMode mode) {
        return null;
    }

    public ReflectionHints registerMethod(Method method, ExecutableMode mode) {
        return null;
    }

}
