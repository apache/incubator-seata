/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.core.protocol;

import java.nio.ByteBuffer;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link FragmentXID} Test
 *
 * @author fagongzi(zhangxu19830126 @ gmail.com)
 */
public class FragmentXIDTest {
    /**
     * Test FragmentXID codec with ipv4
     */
    @Test
    public void testCodecIPV4() {
        FragmentXID xid = FragmentXID.from(1L);
        ByteBuffer buf = ByteBuffer.allocate(25);
        CodecHelper.write(buf, xid);
        buf.flip();
        FragmentXID readXID = CodecHelper.readFragmentXID(buf);
        assertThat(readXID).isEqualTo(xid);
    }

    /**
     * Test FragmentXID codec with ipv4
     */
    @Test
    public void testCodecIPV6() {
        FragmentXID.setServerIPAddress("::1");
        FragmentXID xid = FragmentXID.from(1L);
        ByteBuffer buf = ByteBuffer.allocate(37);
        CodecHelper.write(buf, xid);
        buf.flip();
        FragmentXID readXID = CodecHelper.readFragmentXID(buf);
        assertThat(readXID).isEqualTo(xid);
    }
}
