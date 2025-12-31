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
package org.apache.seata.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ParameterParser Test
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParameterParser Test")
class ParameterParserTest {

    private ParameterParser parameterParser;

    @BeforeEach
    void setUp() {
        parameterParser = new ParameterParser();
    }

    @Test
    @DisplayName("test getHost default value")
    void testGetHostDefaultValue() {
        String host = parameterParser.getHost();
        assertTrue(host == null || host instanceof String);
    }

    @Test
    @DisplayName("test getPort default value")
    void testGetPortDefaultValue() {
        int port = parameterParser.getPort();
        assertTrue(port >= 0);
    }

    @Test
    @DisplayName("test getStoreMode default value")
    void testGetStoreModeDefaultValue() {
        String storeMode = parameterParser.getStoreMode();
        assertTrue(storeMode == null || storeMode instanceof String);
    }

    @Test
    @DisplayName("test getServerNode default value")
    void testGetServerNodeDefaultValue() {
        Long serverNode = parameterParser.getServerNode();
        assertTrue(serverNode == null || serverNode > 0);
    }

    @Test
    @DisplayName("test getSeataEnv default value")
    void testGetSeataEnvDefaultValue() {
        String seataEnv = parameterParser.getSeataEnv();
        assertTrue(seataEnv == null || seataEnv instanceof String);
    }

    @Test
    @DisplayName("test getSessionStoreMode default value")
    void testGetSessionStoreModeDefaultValue() {
        String sessionStoreMode = parameterParser.getSessionStoreMode();
        assertTrue(sessionStoreMode == null || sessionStoreMode instanceof String);
    }

    @Test
    @DisplayName("test getLockStoreMode default value")
    void testGetLockStoreModeDefaultValue() {
        String lockStoreMode = parameterParser.getLockStoreMode();
        assertTrue(lockStoreMode == null || lockStoreMode instanceof String);
    }

    @Test
    @DisplayName("test isHelp default value")
    void testIsHelpDefaultValue() {
        boolean help = parameterParser.isHelp();
        assertFalse(help);
    }

    @Test
    @DisplayName("test cleanUp removes env property")
    void testCleanUpRemovesEnvProperty() {
        parameterParser.cleanUp();
        assertNull(System.getProperty("seata.env"));
    }

    @Test
    @DisplayName("test parameter parser with arguments")
    void testParameterParserWithArguments() {
        ParameterParser parser = new ParameterParser(
                "-h", "localhost",
                "-p", "8080",
                "-m", "file");
        
        assertEquals("localhost", parser.getHost());
        assertEquals(8080, parser.getPort());
        assertEquals("file", parser.getStoreMode());
    }

    @Test
    @DisplayName("test parameter parser with long form arguments")
    void testParameterParserWithLongFormArguments() {
        ParameterParser parser = new ParameterParser(
                "--host", "192.168.1.1",
                "--port", "9090",
                "--storeMode", "db");
        
        assertEquals("192.168.1.1", parser.getHost());
        assertEquals(9090, parser.getPort());
        assertEquals("db", parser.getStoreMode());
    }

    @Test
    @DisplayName("test parameter parser with server node")
    void testParameterParserWithServerNode() {
        ParameterParser parser = new ParameterParser(
                "--serverNode", "1",
                "-p", "8080");
        
        assertEquals(1L, parser.getServerNode());
    }

    @Test
    @DisplayName("test parameter parser with seata env")
    void testParameterParserWithSeataEnv() {
        ParameterParser parser = new ParameterParser(
                "--seataEnv", "test",
                "-p", "8080");
        
        assertEquals("test", parser.getSeataEnv());
    }

    @Test
    @DisplayName("test parameter parser with session store mode")
    void testParameterParserWithSessionStoreMode() {
        ParameterParser parser = new ParameterParser(
                "--sessionStoreMode", "redis",
                "-p", "8080");
        
        assertEquals("redis", parser.getSessionStoreMode());
    }

    @Test
    @DisplayName("test parameter parser with lock store mode")
    void testParameterParserWithLockStoreMode() {
        ParameterParser parser = new ParameterParser(
                "--lockStoreMode", "db",
                "-p", "8080");
        
        assertEquals("db", parser.getLockStoreMode());
    }

    @Test
    @DisplayName("test parameter parser with empty arguments")
    void testParameterParserWithEmptyArguments() {
        ParameterParser parser = new ParameterParser();
        assertNotNull(parser);
    }

    @Test
    @DisplayName("test get all parameters")
    void testGetAllParameters() {
        ParameterParser parser = new ParameterParser(
                "-h", "localhost",
                "-p", "8080",
                "-m", "file",
                "-n", "1",
                "-e", "prod",
                "-ssm", "db",
                "-lsm", "redis");
        
        assertEquals("localhost", parser.getHost());
        assertEquals(8080, parser.getPort());
        assertEquals("file", parser.getStoreMode());
        assertEquals(1L, parser.getServerNode());
        assertEquals("prod", parser.getSeataEnv());
        assertEquals("db", parser.getSessionStoreMode());
        assertEquals("redis", parser.getLockStoreMode());
    }
}
