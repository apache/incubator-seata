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
package org.apache.seata.mcp.tools;

import org.apache.seata.mcp.service.ConsoleApiService;
import org.apache.seata.mcp.service.ModifyConfirmService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SupportToolsBehaviorTest {
    @Test
    void namespaceResponsesHandlePresentMissingNullAndInvalidData() {
        ConsoleApiService remote = mock(ConsoleApiService.class);
        NameSpaceTools tools = new NameSpaceTools(remote, new ObjectMapper());
        when(remote.getCallNameSpace(anyString()))
                .thenReturn("{\"data\":[\"public\"]}", "{}", "{\"data\":null}", "bad-json");
        assertEquals(
                Map.of("namespaces", "[\"public\"]"), tools.getTCNameSpaces().getData());
        assertEquals(Map.of(), tools.getTCNameSpaces().getData());
        assertEquals(Map.of(), tools.getTCNameSpaces().getData());
        assertNull(tools.getTCNameSpaces().getData());
    }

    @Test
    void confirmationRejectsMissingConsentBeforeIssuingKey() {
        ModifyConfirmService service = mock(ModifyConfirmService.class);
        ModifyConfirmTools tools = new ModifyConfirmTools(service);
        for (String input : new String[] {null, "", " ", "delete transaction"}) {
            assertThrows(IllegalArgumentException.class, () -> tools.confirmAndGetKey(input));
        }
        verifyNoInteractions(service);
        Map<String, String> key = Map.of("modify_key", "key");
        when(service.confirmAndGetKey()).thenReturn(key);
        assertSame(key, tools.confirmAndGetKey("confirm delete transaction"));
        assertSame(key, tools.confirmAndGetKey("确认删除事务"));
        verify(service, times(2)).confirmAndGetKey();
    }
}
