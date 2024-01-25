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
package org.apache.seata.core.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The DefaultAuthSigner Test
 */
public class DefaultAuthSignerTest {
    @Test
    public void testGetRamSignNotNull() {
        String data = "testGroup,127.0.0.1,1702564471650";
        String key = "exampleEncryptKey";
        String expectedSign = "6g9nMk6BRLFxl7bf5ZfWaEZvGdho3JBmwvx5rqgSUCE=";
        DefaultAuthSigner signer = new DefaultAuthSigner();
        String sign = signer.sign(data, key);
        Assertions.assertEquals(expectedSign, sign);
    }

    @Test
    public void testGetRamSignNull() {
        String data = null;
        String key = "exampleEncryptKey";
        DefaultAuthSigner signer = new DefaultAuthSigner();
        String sign = signer.sign(data, key);
        Assertions.assertNull(sign);
    }

    @Test
    public void testGetSignVersion() {
        DefaultAuthSigner signer = new DefaultAuthSigner();
        String expectedVersion = "V4";
        String actualVersion = signer.getSignVersion();

        // Assert the returned version matches the expected version
        Assertions.assertEquals(expectedVersion, actualVersion);
    }
}
