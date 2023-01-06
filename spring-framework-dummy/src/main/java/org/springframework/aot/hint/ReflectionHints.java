package org.springframework.aot.hint;

import org.springframework.lang.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
