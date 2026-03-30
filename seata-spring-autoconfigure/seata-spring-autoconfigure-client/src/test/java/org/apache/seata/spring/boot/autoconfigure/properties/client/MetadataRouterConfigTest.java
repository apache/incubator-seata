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
package org.apache.seata.spring.boot.autoconfigure.properties.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MetadataRouterConfigTest {

    @Test
    public void testDefaultValues() {
        MetadataRouterConfig config = new MetadataRouterConfig();

        assertTrue(config.isEnabled());
        assertEquals("", config.getExpression());
    }

    @Test
    public void testEnabledConfiguration() {
        MetadataRouterConfig config = new MetadataRouterConfig();

        config.setEnabled(false);
        assertFalse(config.isEnabled());

        config.setEnabled(true);
        assertTrue(config.isEnabled());
    }

    @Test
    public void testExpressionConfiguration() {
        MetadataRouterConfig config = new MetadataRouterConfig();

        String expression = "env == prod && region == cn-north";
        config.setExpression(expression);

        assertEquals(expression, config.getExpression());
    }

    @Test
    public void testComplexExpressionConfiguration() {
        MetadataRouterConfig config = new MetadataRouterConfig();

        String complexExpression = "(env == prod && region == cn-north) || (env == staging && region == us-east)";
        config.setExpression(complexExpression);

        assertEquals(complexExpression, config.getExpression());
    }

    @Test
    public void testVersionExpressionConfiguration() {
        MetadataRouterConfig config = new MetadataRouterConfig();

        String versionExpression = "version >= 2.0 && weight >= 100";
        config.setExpression(versionExpression);

        assertEquals(versionExpression, config.getExpression());
    }
}
