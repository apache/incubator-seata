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

package com.alibaba.fescar.common;

import java.util.Random;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Xid test.
 *
 * @author Otis.z
 * @date 2019 /2/22
 */
public class XIDTest {

    /**
     * Test set ip address.
     */
    @Test
    public void testSetIpAddress() {
        XID.setIpAddress("127.0.0.1");
        assertThat(XID.getIpAddress()).isEqualTo("127.0.0.1");
    }

    /**
     * Test set port.
     */
    @Test
    public void testSetPort() {
        XID.setPort(8080);
        assertThat(XID.getPort()).isEqualTo(8080);
    }

    /**
     * Test generate xid.
     */
    @Test
    public void testGenerateXID() {
        long tranId = new Random().nextLong();
        XID.setPort(8080);
        XID.setIpAddress("127.0.0.1");
        assertThat(XID.generateXID(tranId)).isEqualTo(XID.getIpAddress() + ":" + XID.getPort() + ":" + tranId);
    }

    /**
     * Test get server address.
     */
    @Test
    public void testGetServerAddress() {
        assertThat(XID.getServerAddress(null)).isNull();
        assertThat(XID.getServerAddress("127.0.0.1:8080:8577662204289747564")).isEqualTo("127.0.0.1:8080");
    }

    /**
     * Test get transaction id.
     */
    @Test
    public void testGetTransactionId() {
        assertThat(XID.getTransactionId(null)).isEqualTo(-1);
        assertThat(XID.getTransactionId("127.0.0.1:8080:8577662204289747564")).isEqualTo(8577662204289747564L);
    }
}
