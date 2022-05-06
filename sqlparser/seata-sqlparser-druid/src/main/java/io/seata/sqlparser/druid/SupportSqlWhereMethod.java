/*
 * Copyright 1999-2019 Seata.io Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.seata.sqlparser.druid;

import java.util.ArrayList;
import java.util.List;

/**
 * author: doubleDimple lovele.cn@gmail.com
 */
public class SupportSqlWhereMethod {

    public static final List<String> supportSqlMethods = new ArrayList<>();

    static {
        supportSqlMethods.add("FIND_IN_SET");
    }

    /**
     * 
     * @param methodName
     * @return boolean
     */
    public static boolean checkIsSupport(String methodName) {
        int size = (int)supportSqlMethods.stream().filter(sqlMethod -> sqlMethod.equalsIgnoreCase(methodName)).count();
        if (size > 0) {
            return true;
        }
        return false;
    }
}
