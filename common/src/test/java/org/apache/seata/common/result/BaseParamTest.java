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
package org.apache.seata.common.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The type Base param test.
 */
public class BaseParamTest {

    @Test
    public void testDefaultConstructor() {
        BaseParam param = new BaseParam();
        assertEquals(0, param.getPageNum());
        assertEquals(0, param.getPageSize());
        assertNull(param.getTimeStart());
        assertNull(param.getTimeEnd());
    }

    @Test
    public void testGettersAndSetters() {
        BaseParam param = new BaseParam();

        param.setPageNum(1);
        assertEquals(1, param.getPageNum());

        param.setPageSize(10);
        assertEquals(10, param.getPageSize());

        param.setTimeStart(1000L);
        assertEquals(1000L, param.getTimeStart());

        param.setTimeEnd(2000L);
        assertEquals(2000L, param.getTimeEnd());
    }

    @Test
    public void testToString() {
        BaseParam param = new BaseParam();
        param.setPageNum(1);
        param.setPageSize(10);
        param.setTimeStart(1000L);
        param.setTimeEnd(2000L);

        String expected = "BaseParam{pageNum=1, pageSize=10, timeStart=1000, timeEnd=2000}";
        assertEquals(expected, param.toString());
    }

    @Test
    public void testSerializable() {
        assertTrue(java.io.Serializable.class.isAssignableFrom(BaseParam.class));
    }
}
