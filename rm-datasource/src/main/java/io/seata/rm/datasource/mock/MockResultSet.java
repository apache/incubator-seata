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
package io.seata.rm.datasource.mock;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * @author will.zjw
 * @date 2019/8/14
 */
public class MockResultSet extends com.alibaba.druid.mock.MockResultSet {

    private List<String> columnLabels;

    /**
     * Instantiates a new Mock result set.
     *
     * @param statement    the statement
     * @param columnLabels the column labels
     */
    public MockResultSet(Statement statement, List<String> columnLabels) {
        super(statement);
        this.columnLabels = new ArrayList<>(columnLabels);
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        return columnLabels.indexOf(columnLabel) + 1;
    }
}
