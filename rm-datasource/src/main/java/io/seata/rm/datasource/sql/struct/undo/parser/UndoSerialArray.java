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

package io.seata.rm.datasource.sql.struct.undo.parser;

import javax.sql.rowset.serial.SerialArray;
import javax.sql.rowset.serial.SerialException;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * The SerialArray used for undo log parser.
 *
 * @author jsbxyyx
 */
public class UndoSerialArray extends SerialArray {

    /**
     * used for serializer framework.
     *
     * @throws SerialException
     * @throws SQLException
     */
    public UndoSerialArray() throws SerialException, SQLException {
        super(new EmptyArray());
    }

    public UndoSerialArray(Array array) throws SerialException, SQLException {
        super(array);
    }

    @Override
    public ResultSet getResultSet() throws SerialException {
        // don't throws exception.
        return null;
    }

    @Override
    public ResultSet getResultSet(Map<String, Class<?>> map) throws SerialException {
        // don't throws exception.
        return null;
    }

    @Override
    public ResultSet getResultSet(long index, int count) throws SerialException {
        // don't throws exception.
        return null;
    }

    @Override
    public ResultSet getResultSet(long index, int count, Map<String, Class<?>> map) throws SerialException {
        // don't throws exception.
        return null;
    }

    /**
     * used for no constructor.
     */
    private static class EmptyArray implements java.sql.Array {

        private final Object[] array = new Object[]{};

        @Override
        public String getBaseTypeName() throws SQLException {
            return null;
        }

        @Override
        public int getBaseType() throws SQLException {
            return 0;
        }

        @Override
        public Object getArray() throws SQLException {
            return array;
        }

        @Override
        public Object getArray(Map<String, Class<?>> map) throws SQLException {
            return array;
        }

        @Override
        public Object getArray(long index, int count) throws SQLException {
            return array;
        }

        @Override
        public Object getArray(long index, int count, Map<String, Class<?>> map) throws SQLException {
            return array;
        }

        @Override
        public ResultSet getResultSet() throws SQLException {
            return null;
        }

        @Override
        public ResultSet getResultSet(Map<String, Class<?>> map) throws SQLException {
            return null;
        }

        @Override
        public ResultSet getResultSet(long index, int count) throws SQLException {
            return null;
        }

        @Override
        public ResultSet getResultSet(long index, int count, Map<String, Class<?>> map) throws SQLException {
            return null;
        }

        @Override
        public void free() throws SQLException {

        }
    }

}
