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
package io.seata.common.loader;

import io.seata.common.util.StringUtils;

import java.util.Objects;

/**
 * The type ExtensionDefinition
 *
 * @param <S> type of serviceClass
 * @author haozhibei
 */
final class ExtensionDefinition<S> {

    private final String name;
    private final Class<S> serviceClass;
    private final Integer order;
    private final Scope scope;
    private boolean state;

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
        this.state = true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, serviceClass, order, scope, state);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        ExtensionDefinition<?> other = (ExtensionDefinition<?>) obj;
        if (!StringUtils.equals(name, other.name)) {
            return false;
        }
        if (!serviceClass.equals(other.serviceClass)) {
            return false;
        }
        if (!order.equals(other.order)) {
            return false;
        }
        if (state != other.state) {
            return false;
        }
        return scope.equals(other.scope);
    }

    public String getName() {
        return name;
    }

    public boolean isState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }
}
