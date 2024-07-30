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
package org.apache.seata.integration.tx.api.remoting.parser;

import org.apache.seata.integration.tx.api.remoting.Protocols;
import org.apache.seata.integration.tx.api.remoting.RemotingDesc;
import org.apache.seata.integration.tx.api.remoting.RemotingParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultRemotingParserTest {

    private static final DefaultRemotingParser remotingParser = DefaultRemotingParser.get();

    @BeforeAll
    public static void setUp() {
        remotingParser.registerRemotingParser(new SimpleRemotingParser());
    }

    @Test
    public void testIsRemoting() {
        SimpleRemoteBean remoteBean = new SimpleRemoteBean();
        RemotingParser parser = remotingParser.isRemoting(remoteBean, remoteBean.getClass().getName());
        assertInstanceOf(SimpleRemotingParser.class, parser);
    }

    @Test
    public void testIsRemotingFail() {
        SimpleBean remoteBean = new SimpleBean();
        assertNull(remotingParser.isRemoting(remoteBean, remoteBean.getClass().getName()));
    }

    @Test
    public void testIsReference() {
        SimpleRemoteBean remoteBean = new SimpleRemoteBean();
        assertTrue(remotingParser.isReference(remoteBean, remoteBean.getClass().getName()));
    }

    @Test
    public void testIsReferenceFail() {
        SimpleBean remoteBean = new SimpleBean();
        assertFalse(remotingParser.isReference(remoteBean, remoteBean.getClass().getName()));
    }

    @Test
    public void testIsServiceFromObject() {
        SimpleRemoteBean remoteBean = new SimpleRemoteBean();
        assertTrue(remotingParser.isService(remoteBean, remoteBean.getClass().getName()));
    }

    @Test
    public void testIsServiceFromObjectFail() {
        SimpleBean remoteBean = new SimpleBean();
        assertFalse(remotingParser.isService(remoteBean, remoteBean.getClass().getName()));
    }

    @Test
    public void testIsServiceFromClass() {
        assertTrue(remotingParser.isService(SimpleRemoteBean.class));
    }

    @Test
    public void testIsServiceFromClassFail() {
        assertFalse(remotingParser.isService(SimpleBean.class));
    }

    @Test
    public void testGetServiceDesc() {
        SimpleRemoteBean remoteBean = new SimpleRemoteBean();
        RemotingDesc desc = remotingParser.getServiceDesc(remoteBean, remoteBean.getClass().getName());
        assertEquals(Protocols.IN_JVM, desc.getProtocol());
        assertEquals(SimpleRemoteBean.class, desc.getServiceClass());
    }

    @Test
    public void testGetServiceDescFail() {
        SimpleBean simpleBean = new SimpleBean();
        assertNull(remotingParser.getServiceDesc(simpleBean, simpleBean.getClass().getName()));
    }

    @Test
    public void testParserRemotingServiceInfo() {
        SimpleRemoteBean remoteBean = new SimpleRemoteBean();
        SimpleRemotingParser parser = new SimpleRemotingParser();
        RemotingDesc desc = remotingParser.parserRemotingServiceInfo(remoteBean, remoteBean.getClass().getName(),
                parser);

        assertEquals(desc, remotingParser.parserRemotingServiceInfo(remoteBean, remoteBean.getClass().getName(),
                parser));
        assertEquals(Protocols.IN_JVM, desc.getProtocol());
        assertEquals(SimpleRemoteBean.class, desc.getServiceClass());
    }

    @Test
    public void testParserRemotingServiceInfoFail() {
        SimpleBean simpleBean = new SimpleBean();
        assertNull(remotingParser.parserRemotingServiceInfo(simpleBean, simpleBean.getClass().getName(),
                new SimpleRemotingParser()));
    }

    @Test
    public void testGetRemotingBeanDesc() {
        SimpleRemoteBean remoteBean = new SimpleRemoteBean();
        remotingParser.parserRemotingServiceInfo(remoteBean, remoteBean.getClass().getName(),
                new SimpleRemotingParser());

        assertNotNull(remotingParser.getRemotingBeanDesc(remoteBean));
    }

    @Test
    public void testGetRemotingDeanDescFail() {
        SimpleBean simpleBean = new SimpleBean();
        assertNull(remotingParser.getRemotingBeanDesc(simpleBean));
    }
}
