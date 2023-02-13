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

import java.util.List;

/**
 * TypeReference's fake
 *
 * @author wang.liang
 */
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
