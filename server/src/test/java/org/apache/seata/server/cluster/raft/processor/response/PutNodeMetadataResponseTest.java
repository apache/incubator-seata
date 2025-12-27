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
package org.apache.seata.server.cluster.raft.processor.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PutNodeMetadataResponseTest {

    @Test
    public void testConstructorWithTrue() {
        PutNodeMetadataResponse response = new PutNodeMetadataResponse(true);
        assertTrue(response.isSuccess());
    }

    @Test
    public void testConstructorWithFalse() {
        PutNodeMetadataResponse response = new PutNodeMetadataResponse(false);
        assertFalse(response.isSuccess());
    }

    @Test
    public void testSetSuccess() {
        PutNodeMetadataResponse response = new PutNodeMetadataResponse(false);
        assertFalse(response.isSuccess());

        response.setSuccess(true);
        assertTrue(response.isSuccess());
    }

    @Test
    public void testToString() {
        PutNodeMetadataResponse response = new PutNodeMetadataResponse(true);
        String str = response.toString();

        assertNotNull(str);
        assertTrue(str.contains("success"));
        assertTrue(str.contains("true"));
    }

    @Test
    public void testToStringWithFalse() {
        PutNodeMetadataResponse response = new PutNodeMetadataResponse(false);
        String str = response.toString();

        assertNotNull(str);
        assertTrue(str.contains("success"));
        assertTrue(str.contains("false"));
    }
}
