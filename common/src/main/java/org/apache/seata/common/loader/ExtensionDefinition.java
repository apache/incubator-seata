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
package org.apache.seata.common.loader;

import org.apache.seata.common.util.StringUtils;

import java.util.Objects;

/**
 * The type ExtensionDefinition
 *
 * @param <S> type of serviceClass
 */
final class ExtensionDefinition<S> {

    private final String name;
    private final Class<S> serviceClass;
    private final Integer order;
    private final Scope scope;

    public Integer getOrder() {
        return this.order;
    }

    public Class<S> getServiceClass() {
        return this.serviceClass;
    }

    public Scope getScope() {
        return this.scope;
    }

    public ExtensionDefinition(String name, Integer order, Scope scope, Class<S> clazz) {
        this.name = name;
        this.order = order;
        this.scope = scope;
        this.serviceClass = clazz;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, serviceClass, order, scope);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        ExtensionDefinition<?> that = (ExtensionDefinition<?>) obj;
        return StringUtils.equals(name, that.name)
                && Objects.equals(serviceClass, that.serviceClass)
                && Objects.equals(order, that.order)
                && Objects.equals(scope, that.scope);
    }
}
