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

import org.apache.seata.common.json.JsonAllowlistManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SeataJsonPropertiesTest {

    @AfterEach
    public void cleanup() {
        JsonAllowlistManager.getInstance().clearUserAllowlist();
    }

    @Test
    public void testSeataJsonProperties() {
        SeataJsonProperties props = new SeataJsonProperties();

        String serializerType = "jackson";
        String allowlist = "com.test.model.,com.test.dto.,com.test.SpecialBean";
        props.setSerializerType(serializerType);
        props.setAllowlist(allowlist);

        Assertions.assertEquals(serializerType, props.getSerializerType());
        Assertions.assertEquals(allowlist, props.getAllowlist());
    }

    @Test
    public void testAllowlistAppliedToManager() {
        SeataJsonProperties props = new SeataJsonProperties();
        props.setAllowlist("com.test.model.,com.test.dto.,com.test.SpecialBean");
        props.init();

        // Prefix match
        Assertions.assertTrue(JsonAllowlistManager.getInstance().isAllowed("com.test.model.User"));
        Assertions.assertTrue(JsonAllowlistManager.getInstance().isAllowed("com.test.dto.OrderDto"));

        // Exact match
        Assertions.assertTrue(JsonAllowlistManager.getInstance().isAllowed("com.test.SpecialBean"));

        // Not allowed
        Assertions.assertFalse(JsonAllowlistManager.getInstance().isAllowed("com.test.SpecialBeanExtra"));
        Assertions.assertFalse(JsonAllowlistManager.getInstance().isAllowed("com.unknown.MaliciousClass"));
    }

    @Test
    public void testEmptyAllowlist() {
        SeataJsonProperties props = new SeataJsonProperties();
        props.setAllowlist("");

        Assertions.assertEquals("", props.getAllowlist());
    }

    @Test
    public void testNullAllowlist() {
        SeataJsonProperties props = new SeataJsonProperties();
        props.setAllowlist(null);

        Assertions.assertNull(props.getAllowlist());
    }
}
