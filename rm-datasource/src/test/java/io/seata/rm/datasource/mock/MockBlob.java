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

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;

/**
 * @author will
 */
public class MockBlob implements Blob {

    public MockBlob() {
    }

    @Override
    public long length() throws SQLException {
        return 0;
    }

    @Override
    public byte[] getBytes(long pos, int length) throws SQLException {
        return new byte[0];
    }

    @Override
    public InputStream getBinaryStream() throws SQLException {
        return null;
    }

    @Override
    public long position(byte[] pattern, long start) throws SQLException {
        return 0;
    }

    @Override
    public long position(Blob pattern, long start) throws SQLException {
        return 0;
    }

    @Override
    public int setBytes(long pos, byte[] bytes) throws SQLException {
        return 0;
    }

    @Override
    public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
        return 0;
    }

    @Override
    public OutputStream setBinaryStream(long pos) throws SQLException {
        return null;
    }

    @Override
    public void truncate(long len) throws SQLException {

    }

    @Override
    public void free() throws SQLException {

    }

    @Override
    public InputStream getBinaryStream(long pos, long length) throws SQLException {
        return null;
    }
}
