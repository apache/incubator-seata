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
package io.seata.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type {@link EnumUtils} test.
 *
 * @author wang.liang
 */
public class EnumUtilsTest {

    enum MockEnum {
        MOCK_ENUM1,
        MOCK_ENUM2;
    }

    enum MockEmptyEnum {
    }

    @Test
    public void test_getEnum() {
        MockEnum e = EnumUtils.getEnum(MockEnum.class, "mock_enum1");
        Assertions.assertEquals(MockEnum.MOCK_ENUM1, e);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            EnumUtils.getEnum(MockEnum.class, "mock_enum_xxxx");
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            EnumUtils.getEnum(MockEmptyEnum.class, "xxxxxxxxx");
        });
    }
}
