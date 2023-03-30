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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((serviceClass == null) ? 0 : serviceClass.hashCode());
        result = prime * result + ((order == null) ? 0 : order.hashCode());
        result = prime * result + ((scope == null) ? 0 : scope.hashCode());
        return result;
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
        return scope.equals(other.scope);
    }

    public String getName() {
        return name;
    }
}
