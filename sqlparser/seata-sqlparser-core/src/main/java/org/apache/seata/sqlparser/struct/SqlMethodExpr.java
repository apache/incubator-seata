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
package org.apache.seata.sqlparser.struct;

/**
 * TODO
 * sql method invoke expression
 */
public class SqlMethodExpr {

    private static final SqlMethodExpr INSTANCE = new SqlMethodExpr(0);

    private final int placeholderCount;

    /**
     * Get SqlMethodExpr.
     *
     * @return the SqlMethodExpr
     */
    public static SqlMethodExpr get() {
        return INSTANCE;
    }

    /**
     * Instantiates a new SqlMethodExpr with a specific placeholder count.
     *
     * @param placeholderCount the number of placeholders inside the method
     */
    public SqlMethodExpr(int placeholderCount) {
        this.placeholderCount = placeholderCount;
    }

    public int getPlaceholderCount() {
        return placeholderCount;
    }

    @Override
    public String toString() {
        return "SQL_METHOD";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SqlMethodExpr)) {
            return false;
        }
        SqlMethodExpr other = (SqlMethodExpr) obj;
        return placeholderCount == other.placeholderCount;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(placeholderCount);
    }
}
