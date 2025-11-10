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
package io.seata.integration.tx.api.remoting;

import org.apache.seata.common.exception.FrameworkException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for RemotingParser compatibility interface.
 */
public class RemotingParserTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                RemotingParser.class.isAnnotationPresent(Deprecated.class),
                "RemotingParser should be marked as @Deprecated");
    }

    @Test
    public void testExtendsApacheSeataInterface() {
        assertTrue(
                org.apache.seata.integration.tx.api.remoting.RemotingParser.class.isAssignableFrom(
                        RemotingParser.class),
                "RemotingParser should extend Apache Seata RemotingParser interface");
    }

    @Test
    public void testIsInterface() {
        assertTrue(RemotingParser.class.isInterface(), "RemotingParser should be an interface");
    }

    @Test
    public void testImplementationCanBeAssigned() {
        // Test that an implementation of Apache Seata RemotingParser can be used as io.seata RemotingParser
        org.apache.seata.integration.tx.api.remoting.RemotingParser apacheParser = new TestRemotingParser();
        assertTrue(
                apacheParser instanceof RemotingParser,
                "Apache Seata RemotingParser implementation should be assignable to io.seata RemotingParser");
    }

    // Test implementation
    static class TestRemotingParser implements RemotingParser {
        @Override
        public boolean isRemoting(Object bean, String beanName) {
            return false;
        }

        @Override
        public boolean isReference(Object bean, String beanName) {
            return false;
        }

        @Override
        public boolean isService(Object bean, String beanName) {
            return false;
        }

        @Override
        public boolean isService(Class<?> beanClass) throws FrameworkException {
            return false;
        }

        @Override
        public org.apache.seata.integration.tx.api.remoting.RemotingDesc getServiceDesc(Object bean, String beanName) {
            return null;
        }

        @Override
        public short getProtocol() {
            return 0;
        }
    }
}
