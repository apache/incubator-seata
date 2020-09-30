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
package io.seata.common.holder;

import io.seata.common.exception.Message;
import io.seata.common.exception.ShouldNeverHappenException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The test for {@link ObjectHolder}
 *
 * @author wang.liang
 */
public class ObjectHolderTest {
    private static final String KEY = "test";
    private static final String KEY2 = "test2";

    @Test
    void test_getObject_by_key() {
        // case 1: null
        Assertions.assertNull(ObjectHolder.INSTANCE.getObject(KEY));

        // case 2: not null
        Object obj = new Object();
        ObjectHolder.INSTANCE.setObject(KEY, obj);
        Object obj2 = ObjectHolder.INSTANCE.getObject(KEY);
        Assertions.assertSame(obj, obj2);
        Object obj3 = new Object();
        Assertions.assertNotSame(obj3, obj2);
    }

    @Test
    void test_getObject_by_class() {
        // case 1: null
        Assertions.assertThrows(ShouldNeverHappenException.class, () -> {
            ObjectHolder.INSTANCE.getObject(Message.class);
        });

        // case 2: not null
        ObjectHolder.INSTANCE.setObject(KEY2, new Message());
        Object obj = ObjectHolder.INSTANCE.getObject(Message.class);
        Assertions.assertEquals(obj.getClass(), Message.class);
    }
}
