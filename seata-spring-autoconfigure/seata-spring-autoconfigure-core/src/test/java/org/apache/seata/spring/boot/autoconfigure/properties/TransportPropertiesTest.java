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
package org.apache.seata.spring.boot.autoconfigure.properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TransportPropertiesTest {

    @Test
    public void testTransportProperties() {
        TransportProperties transportProperties = new TransportProperties();
        transportProperties.setServer("server");
        transportProperties.setType("type");
        transportProperties.setSerialization("serialization");
        transportProperties.setCompressor("compressor");
        transportProperties.setHeartbeat(true);
        transportProperties.setEnableClientBatchSendRequest(true);
        transportProperties.setEnableRmClientBatchSendRequest(true);
        transportProperties.setEnableTmClientBatchSendRequest(true);
        transportProperties.setEnableTcServerBatchSendResponse(true);
        transportProperties.setRpcRmRequestTimeout(1);
        transportProperties.setRpcTmRequestTimeout(1);
        transportProperties.setRpcTcRequestTimeout(1);

        Assertions.assertEquals("server", transportProperties.getServer());
        Assertions.assertEquals("type", transportProperties.getType());
        Assertions.assertEquals("serialization", transportProperties.getSerialization());
        Assertions.assertEquals("compressor", transportProperties.getCompressor());
        Assertions.assertTrue(transportProperties.isHeartbeat());
        Assertions.assertTrue(transportProperties.isEnableClientBatchSendRequest());
        Assertions.assertTrue(transportProperties.isEnableRmClientBatchSendRequest());
        Assertions.assertTrue(transportProperties.isEnableTmClientBatchSendRequest());
        Assertions.assertTrue(transportProperties.isEnableTcServerBatchSendResponse());
        Assertions.assertEquals(1, transportProperties.getRpcRmRequestTimeout());
        Assertions.assertEquals(1, transportProperties.getRpcTmRequestTimeout());
        Assertions.assertEquals(1, transportProperties.getRpcTcRequestTimeout());
    }
}
