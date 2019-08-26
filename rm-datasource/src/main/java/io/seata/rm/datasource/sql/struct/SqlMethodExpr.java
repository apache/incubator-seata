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
package io.seata.rm.datasource.sql.struct;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 * sql method invoke expression
 * @author jsbxyyx
 */
public class SqlMethodExpr {

    private String name;
    private List<Object> parameters = new ArrayList<>();

    public SqlMethodExpr() {}

    public SqlMethodExpr(String name, List<Object> parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    /**
     * get name
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * set name
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * get parameters
     * @return the parameters
     */
    public List<Object> getParameters() {
        return parameters;
    }

    /**
     * set parameters
     * @param parameters the parameters
     */
    public void setParameters(List<Object> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return "SqlMethod{" +
                "name='" + name + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}
