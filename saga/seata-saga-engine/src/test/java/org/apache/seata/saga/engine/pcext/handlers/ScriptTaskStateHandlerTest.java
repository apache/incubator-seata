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
package org.apache.seata.saga.engine.pcext.handlers;

import org.apache.seata.saga.engine.exception.EngineExecutionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScriptTaskStateHandlerTest {

    @Test
    void testValidateScriptSecurity_allowsNormalGroovyScript() {
        assertDoesNotThrow(() -> ScriptTaskStateHandler.validateScriptSecurity("groovy", "a + b"));
    }

    @Test
    void testValidateScriptSecurity_allowsJavascript() {
        assertDoesNotThrow(() -> ScriptTaskStateHandler.validateScriptSecurity("js", "var x = 1 + 2; x;"));
    }

    @Test
    void testValidateScriptSecurity_blocksDisallowedScriptType() {
        assertThrows(
                EngineExecutionException.class,
                () -> ScriptTaskStateHandler.validateScriptSecurity("python", "print('hello')"));
    }

    @Test
    void testValidateScriptSecurity_allowsRuntimeException() {
        assertDoesNotThrow(
                () -> ScriptTaskStateHandler.validateScriptSecurity("groovy", "throw new RuntimeException('test')"));
    }

    @Test
    void testValidateScriptSecurity_blocksGroovyExecute() {
        assertThrows(
                EngineExecutionException.class,
                () -> ScriptTaskStateHandler.validateScriptSecurity("groovy", "'whoami'.execute()"));
    }

    @Test
    void testValidateScriptSecurity_blocksProcessBuilder() {
        assertThrows(
                EngineExecutionException.class,
                () -> ScriptTaskStateHandler.validateScriptSecurity(
                        "groovy", "new ProcessBuilder(['whoami']).start()"));
    }

    @Test
    void testValidateScriptSecurity_blocksClassForName() {
        assertThrows(
                EngineExecutionException.class,
                () -> ScriptTaskStateHandler.validateScriptSecurity("groovy", "Class.forName('java.lang.Runtime')"));
    }

    @Test
    void testValidateScriptSecurity_blocksSystemExit() {
        assertThrows(
                EngineExecutionException.class,
                () -> ScriptTaskStateHandler.validateScriptSecurity("groovy", "System.exit(0)"));
    }

    @Test
    void testValidateScriptSecurity_blocksFileAccess() {
        assertThrows(
                EngineExecutionException.class,
                () -> ScriptTaskStateHandler.validateScriptSecurity("groovy", "new java.io.File('/etc/passwd').text"));
    }

    @Test
    void testValidateScriptSecurity_blocksNetworkAccess() {
        assertThrows(
                EngineExecutionException.class,
                () -> ScriptTaskStateHandler.validateScriptSecurity(
                        "groovy", "new java.net.URL('http://evil.com').text"));
    }

    @Test
    void testValidateScriptSecurity_blocksGroovyShell() {
        assertThrows(
                EngineExecutionException.class,
                () -> ScriptTaskStateHandler.validateScriptSecurity("groovy", "new GroovyShell().evaluate('1+1')"));
    }

    @Test
    void testValidateScriptSecurity_blocksClassLoader() {
        assertThrows(
                EngineExecutionException.class,
                () -> ScriptTaskStateHandler.validateScriptSecurity("groovy", "new ClassLoader(){}"));
    }
}
