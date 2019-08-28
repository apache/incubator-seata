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
package io.seata.common.util;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;

import javax.sql.rowset.serial.SerialBlob;

import io.seata.common.Constants;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The type Blob utils test.
 *
 * @author Otis.z
 * @author Geng Zhang
 * @date 2019 /2/26
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
        assertThat(BlobUtils.string2blob("123abc")).isEqualTo(
            new SerialBlob("123abc".getBytes(Constants.DEFAULT_CHARSET)));
    }

    /**
     * Test blob 2 string.
     *
     * @throws SQLException the sql exception
     */
    @Test
    public void testBlob2string() throws SQLException {
        assertNull(BlobUtils.blob2string(null));
        assertThat(BlobUtils.blob2string(new SerialBlob("123absent".getBytes(Constants.DEFAULT_CHARSET)))).isEqualTo(
            "123absent");
    }

    @Test
    void bytes2Blob() throws UnsupportedEncodingException, SQLException {
        assertNull(BlobUtils.bytes2Blob(null));
        byte[] bs = "xxaaadd".getBytes(Constants.DEFAULT_CHARSET_NAME);
        assertThat(BlobUtils.bytes2Blob(bs)).isEqualTo(
                new SerialBlob(bs));
    }

    @Test
    void blob2Bytes() throws UnsupportedEncodingException, SQLException {
        assertNull(BlobUtils.blob2Bytes(null));
        byte[] bs = "xxaaadd".getBytes(Constants.DEFAULT_CHARSET_NAME);
        assertThat(BlobUtils.blob2Bytes(new SerialBlob(bs))).isEqualTo(
                bs);
    }
}
