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
package io.seata.rm.datasource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author slievrly
 */
class SqlGenerateUtilsTest {


    @Test
    void testBuildWhereConditionByPKs() throws SQLException {
        List<String> pkNameList=new ArrayList<>();
        pkNameList.add("id");
        pkNameList.add("name");
        String result = SqlGenerateUtils.buildWhereConditionByPKs(pkNameList,4,"mysql",2);
        Assertions.assertEquals("(id,name) in ( (?,?),(?,?) ) or (id,name) in ( (?,?),(?,?) )", result);
        result = SqlGenerateUtils.buildWhereConditionByPKs(pkNameList,5,"mysql",2);
        Assertions.assertEquals("(id,name) in ( (?,?),(?,?) ) or (id,name) in ( (?,?),(?,?) ) or (id,name) in ( (?,?)"
                + " )",
            result);
    }
}
