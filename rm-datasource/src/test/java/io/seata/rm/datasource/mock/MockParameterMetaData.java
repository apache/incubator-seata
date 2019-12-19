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

import java.sql.ParameterMetaData;
import java.sql.SQLException;

/**
 * @author will
 */
public class MockParameterMetaData implements ParameterMetaData {

    private int parameterCount;

    public MockParameterMetaData(String sql) {
        for (int i = 0; i < sql.length(); i++) {
            if (sql.charAt(i) == '?') {
                parameterCount++;
            }
        }
    }

    @Override
    public int getParameterCount() throws SQLException {
        return parameterCount;
    }

    @Override
    public int isNullable(int param) throws SQLException {
        return 0;
    }

    @Override
    public boolean isSigned(int param) throws SQLException {
        return false;
    }

    @Override
    public int getPrecision(int param) throws SQLException {
        return 0;
    }

    @Override
    public int getScale(int param) throws SQLException {
        return 0;
    }

    @Override
    public int getParameterType(int param) throws SQLException {
        return 0;
    }

    @Override
    public String getParameterTypeName(int param) throws SQLException {
        return null;
    }

    @Override
    public String getParameterClassName(int param) throws SQLException {
        return null;
    }

    @Override
    public int getParameterMode(int param) throws SQLException {
        return 0;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
