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
package org.apache.seata.common.metadata;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;


public class NodeTest {

    @Test
    public void testNode() {
        Node node = new Node();
        node.setMetadata(new HashMap<>());
        Assertions.assertEquals(new HashMap<>(), node.getMetadata());

        Node.Endpoint endpoint = node.createEndpoint("127.0.0.1", 80, "get");
        node.setControl(endpoint);
        Assertions.assertEquals(endpoint, node.getControl());

        node.setTransaction(endpoint);
        Assertions.assertEquals(endpoint, node.getTransaction());

        Node.Endpoint endpoint1 = new Node.Endpoint();
        endpoint1.setHost("127.0.0.1");
        Assertions.assertEquals("127.0.0.1", endpoint1.getHost());
        endpoint1.setPort(80);
        Assertions.assertEquals(80, endpoint1.getPort());
        Assertions.assertEquals("127.0.0.1:80", endpoint1.createAddress());
        Assertions.assertEquals("Endpoint{host='127.0.0.1', port=80}", endpoint1.toString());
        Assertions.assertEquals("Endpoint{host='127.0.0.1', port=80}", new Node.Endpoint("127.0.0.1", 80).toString());

    }
}
