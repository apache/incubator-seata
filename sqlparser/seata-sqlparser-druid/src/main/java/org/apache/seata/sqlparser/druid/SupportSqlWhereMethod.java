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
package org.apache.seata.sqlparser.druid;

import java.util.Set;
import java.util.TreeSet;

public class SupportSqlWhereMethod {

    public SupportSqlWhereMethod() {
        add("FIND_IN_SET");
    }

    private final Set<String> supportMethodNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    private static class SupportSqlWhereMethodHolder {
        private static final SupportSqlWhereMethod INSTANCE = new SupportSqlWhereMethod();
    }

    public static SupportSqlWhereMethod getInstance() {
        return SupportSqlWhereMethodHolder.INSTANCE;
    }

    public void add(String methodName) {
        supportMethodNames.add(methodName);
    }

    /**
     * 
     * @param methodName
     * @return boolean
     */
    public boolean checkIsSupport(String methodName) {
        return supportMethodNames.contains(methodName);
    }
}
