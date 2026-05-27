/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.seata.namingserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowStaticJsonResourcesWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/version.json")).andExpect(status().isOk());
    }

    @Test
    void shouldAllowSagaDesignerResourcesWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/saga-statemachine-designer/index.html")).andExpect(result -> {
            int statusCode = result.getResponse().getStatus();
            assertTrue(
                    statusCode == 200 || statusCode == 404,
                    "Bypassed resources should return 200 or 404, but got: " + statusCode);
        });
    }

    @Test
    void shouldSecureProtectedApiEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/console/users")).andExpect(status().isUnauthorized());
    }
}
