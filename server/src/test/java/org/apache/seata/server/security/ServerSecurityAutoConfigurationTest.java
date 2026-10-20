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
package org.apache.seata.server.security;

class ServerSecurityAutoConfigurationTest {
    @org.junit.jupiter.api.Test
    void omittedPermissionsLoadAsDenyAll() {
        ServerSecurityProperties props = new ServerSecurityProperties();
        ServerSecurityProperties.CallerConfig cfg = new ServerSecurityProperties.CallerConfig();
        cfg.setId("empty");
        cfg.setSecretRef("test-secret");
        props.setAllowedCallers(java.util.Collections.singletonList(cfg));
        ServerSecretResolver resolver = org.mockito.Mockito.mock(ServerSecretResolver.class);
        org.mockito.Mockito.when(resolver.resolve("test-secret")).thenReturn(new byte[32]);
        org.junit.jupiter.api.Assertions.assertFalse(new ServerSecurityAutoConfiguration()
                .allowedCallerRegistry(props, resolver)
                .find("empty")
                .get()
                .hasPermission(CallerPermission.VGROUP_WRITE));
    }
}
