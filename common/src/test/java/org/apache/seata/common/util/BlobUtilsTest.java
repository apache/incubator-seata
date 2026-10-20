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
package org.apache.seata.common.util;

import org.apache.seata.common.Constants;
import org.junit.jupiter.api.Test;

import javax.sql.rowset.serial.SerialBlob;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The type Blob utils test.
 *
 */
public class BlobUtilsTest {

    /**
     * Test string 2 blob.
     *
     * @throws SQLException the sql exception
     */
    @Test
    public void testString2blob() throws SQLException {
        assertNull(BlobUtils.string2blob(null));
        assertThat(BlobUtils.string2blob("123abc"))
                .isEqualTo(new SerialBlob("123abc".getBytes(Constants.DEFAULT_CHARSET)));
    }

    /**
     * Test blob 2 string.
     *
     * @throws SQLException the sql exception
     */
    @Test
    public void testBlob2string() throws SQLException {
        assertNull(BlobUtils.blob2string(null));
        assertThat(BlobUtils.blob2string(new SerialBlob("123absent".getBytes(Constants.DEFAULT_CHARSET))))
                .isEqualTo("123absent");
    }

    @Test
    public void testBytes2Blob() throws UnsupportedEncodingException, SQLException {
        assertNull(BlobUtils.bytes2Blob(null));
        byte[] bs = "xxaaadd".getBytes(Constants.DEFAULT_CHARSET_NAME);
        assertThat(BlobUtils.bytes2Blob(bs)).isEqualTo(new SerialBlob(bs));
    }

    @Test
    public void testBlob2Bytes() throws UnsupportedEncodingException, SQLException {
        assertNull(BlobUtils.blob2Bytes(null));
        byte[] bs = "xxaaadd".getBytes(Constants.DEFAULT_CHARSET_NAME);
        assertThat(BlobUtils.blob2Bytes(new SerialBlob(bs))).isEqualTo(bs);
    }

    @Test
    public void testEmptyStringRoundTrip() throws SQLException {
        Blob blob = BlobUtils.string2blob("");

        assertThat(blob).isNotNull();
        assertThat(blob.length()).isZero();
        assertThat(BlobUtils.blob2string(blob)).isEmpty();
    }

    @Test
    public void testEmptyByteArrayRoundTrip() throws SQLException {
        Blob blob = BlobUtils.bytes2Blob(new byte[0]);

        assertThat(blob).isNotNull();
        assertThat(blob.length()).isZero();
        assertThat(BlobUtils.blob2Bytes(blob)).isEmpty();
    }

    @Test
    public void testUtf8StringRoundTrip() throws SQLException {
        String value = "Seata-事务-€-🙂";
        byte[] expectedBytes = value.getBytes(StandardCharsets.UTF_8);
        Blob blob = BlobUtils.string2blob(value);

        assertThat(blob.length()).isEqualTo(expectedBytes.length);
        assertThat(BlobUtils.blob2Bytes(blob)).containsExactly(expectedBytes);
        assertThat(BlobUtils.blob2string(blob)).isEqualTo(value);
    }

    @Test
    public void testBinaryByteArrayRoundTrip() throws SQLException {
        byte[] value = {0, 1, 0x7f, (byte) 0x80, (byte) 0xff};
        Blob blob = BlobUtils.bytes2Blob(value);

        assertThat(blob.length()).isEqualTo(value.length);
        assertThat(BlobUtils.blob2Bytes(blob)).containsExactly(value);
    }
}
