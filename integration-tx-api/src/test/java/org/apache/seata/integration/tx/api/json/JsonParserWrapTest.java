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
package org.apache.seata.integration.tx.api.json;

import org.apache.seata.common.exception.JsonParseException;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.integration.tx.api.interceptor.TwoPhaseBusinessActionParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonParserWrapTest {

    private JsonParserWrap parserWrap;
    private final String jsonString = "{\"actionName\":\"business_action\",\"useCommonFence\":null,\"businessActionContext\":null," +
            "\"branchType\":\"TCC\",\"delayReport\":null}";


    @BeforeEach
    public void setUp() {
        parserWrap = new JsonParserWrap(new JsonParserImpl());
    }

    @Test
    public void testToJSONString() {
        TwoPhaseBusinessActionParam actionParam = new TwoPhaseBusinessActionParam();
        actionParam.setActionName("business_action");
        actionParam.setBranchType(BranchType.TCC);

        String resultString = parserWrap.toJSONString(actionParam);

        assertEquals(jsonString, resultString);
    }

    @Test
    public void testToJSONStringThrowsException() {
        Object mockItem = mock(Object.class);
        when(mockItem.toString()).thenReturn(mockItem.getClass().getName());
        assertThrows(JsonParseException.class, () -> parserWrap.toJSONString(mockItem));
    }

    @Test
    public void testParseObject() {
        TwoPhaseBusinessActionParam actionParam = parserWrap.parseObject(jsonString, TwoPhaseBusinessActionParam.class);

        assertEquals("business_action", actionParam.getActionName());
        assertEquals(BranchType.TCC, actionParam.getBranchType());
    }

    @Test
    public void testParseObjectThrowsException() {
        assertThrows(JsonParseException.class, () -> parserWrap.parseObject(jsonString, Integer.class));
    }

    @Test
    public void testGetName() {
        assertEquals("customParser", parserWrap.getName());
    }
}
