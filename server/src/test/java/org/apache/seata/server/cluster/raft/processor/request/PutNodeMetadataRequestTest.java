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
package org.apache.seata.server.cluster.raft.processor.request;

import org.apache.seata.common.metadata.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PutNodeMetadataRequestTest {

    @Test
    public void testDefaultConstructor() {
        PutNodeMetadataRequest request = new PutNodeMetadataRequest();
        assertNotNull(request);
        assertNull(request.getNode());
    }

    @Test
    public void testConstructorWithNode() {
        Node node = new Node();
        PutNodeMetadataRequest request = new PutNodeMetadataRequest(node);

        assertNotNull(request);
        assertEquals(node, request.getNode());
    }

    @Test
    public void testSetAndGetNode() {
        PutNodeMetadataRequest request = new PutNodeMetadataRequest();
        Node node = new Node();
        request.setNode(node);

        assertEquals(node, request.getNode());
    }

    @Test
    public void testSetNodeToNull() {
        Node node = new Node();
        PutNodeMetadataRequest request = new PutNodeMetadataRequest(node);
        assertNotNull(request.getNode());

        request.setNode(null);
        assertNull(request.getNode());
    }
}
