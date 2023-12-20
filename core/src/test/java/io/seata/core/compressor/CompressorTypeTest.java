/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.core.compressor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The CompressorType Test
 *
 * @author zhongxiang.wang
 */
public class CompressorTypeTest {
    @Test
    public void testGetByCode() {
        int code = 1;
        CompressorType expectedType = CompressorType.GZIP;

        CompressorType actualType = CompressorType.getByCode(code);

        // Assert the returned type matches the expected type
        Assertions.assertEquals(expectedType, actualType);
    }

    @Test
    public void testGetByName() {
        String name = "gzip";
        CompressorType expectedType = CompressorType.GZIP;

        CompressorType actualType = CompressorType.getByName(name);

        // Assert the returned type matches the expected type
        Assertions.assertEquals(expectedType, actualType);
    }
}
