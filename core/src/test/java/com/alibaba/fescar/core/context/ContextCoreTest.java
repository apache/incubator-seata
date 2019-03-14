/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.core.context;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Context core test.
 *
 * @author guoyao
 * @date 2019 /3/5
 */
public class ContextCoreTest {

    private final String FIRST_KEY = "first_key";
    private final String FIRST_VALUE = "first_value";
    private final String SECOND_KEY = "second_key";
    private final String SECOND_VALUE = "second_value";
    private final String NOT_EXIST_KEY = "not_exist_key";

    /**
     * Test put.
     */
    @Test
    public void testPut() {
        ContextCore load = ContextCoreLoader.load();
        assertThat(load.put(FIRST_KEY, FIRST_VALUE)).isNull();
        assertThat(load.put(SECOND_KEY, SECOND_VALUE)).isNull();
        assertThat(load.put(FIRST_KEY, SECOND_VALUE)).isEqualTo(FIRST_VALUE);
        assertThat(load.put(SECOND_KEY, FIRST_VALUE)).isEqualTo(SECOND_VALUE);
        //clear keys
        load.remove(FIRST_KEY);
        load.remove(SECOND_KEY);
    }

    /**
     * Test get.
     */
    @Test
    public void testGet() {
        ContextCore load = ContextCoreLoader.load();
        load.put(FIRST_KEY, FIRST_VALUE);
        load.put(SECOND_KEY, FIRST_VALUE);
        assertThat(load.get(FIRST_KEY)).isEqualTo(FIRST_VALUE);
        assertThat(load.get(SECOND_KEY)).isEqualTo(FIRST_VALUE);
        load.put(FIRST_KEY, SECOND_VALUE);
        load.put(SECOND_KEY, SECOND_VALUE);
        assertThat(load.get(FIRST_KEY)).isEqualTo(SECOND_VALUE);
        assertThat(load.get(SECOND_KEY)).isEqualTo(SECOND_VALUE);
        assertThat(load.get(NOT_EXIST_KEY)).isNull();
        //clear keys
        load.remove(FIRST_KEY);
        load.remove(SECOND_KEY);
        load.remove(NOT_EXIST_KEY);
    }

    /**
     * Test remove.
     */
    @Test
    public void testRemove() {
        ContextCore load = ContextCoreLoader.load();
        load.put(FIRST_KEY, FIRST_VALUE);
        load.put(SECOND_KEY, SECOND_VALUE);
        assertThat(load.remove(FIRST_KEY)).isEqualTo(FIRST_VALUE);
        assertThat(load.remove(SECOND_KEY)).isEqualTo(SECOND_VALUE);
        assertThat(load.remove(NOT_EXIST_KEY)).isNull();
    }

}
