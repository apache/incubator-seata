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
 * The type URL
 *
 * @author: haozhibei
 */
public class ExtensionURL {
    private String name;
    private String typeName;
    private Integer order;
    private Scope scope;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getOrder() {
        return this.order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Scope getScope() {
        return this.scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public ExtensionURL(String name, String typeName, Integer order, Scope scope) {
        this.name = name;
        this.typeName = typeName;
        this.order = order;
        this.scope = scope;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
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
        ExtensionURL other = (ExtensionURL)obj;
        if (!StringUtils.equals(name, other.name)) {
            return false;
        }
        if (!StringUtils.equals(typeName, other.typeName)) {
            return false;
        }
        if (!order.equals(other.order)) {
            return false;
        }
        if (!scope.equals(other.scope)) {
            return false;
        }
        return true;
    }


}
