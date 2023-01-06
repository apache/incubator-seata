package org.springframework.aot.hint;

import org.springframework.lang.Nullable;

import java.util.List;

public interface TypeReference {

    String getName();

    String getCanonicalName();

    String getPackageName();

    String getSimpleName();

    @Nullable
    TypeReference getEnclosingType();

    static TypeReference of(Class<?> type) {
        return null;
    }

    static TypeReference of(String className) {
        return null;
    }

    static List<TypeReference> listOf(Class<?>... types) {
        return null;
    }

}
