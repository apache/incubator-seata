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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Geng Zhang
 */
public class CollectionUtilsTest {

    @Test
    public void isSizeEquals() {
        List<String> list0 = new ArrayList<>();
        List<String> list1 = new ArrayList<>();
        Assertions.assertTrue(CollectionUtils.isSizeEquals(null, null));
        Assertions.assertFalse(CollectionUtils.isSizeEquals(null, list0));
        Assertions.assertFalse(CollectionUtils.isSizeEquals(list1, null));
        Assertions.assertTrue(CollectionUtils.isSizeEquals(list0, list1));

        list0.add("111");
        Assertions.assertFalse(CollectionUtils.isSizeEquals(list0, list1));
        list1.add("111");
        Assertions.assertTrue(CollectionUtils.isSizeEquals(list0, list1));
    }
}