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
package org.apache.seata.server.storage.file;

import org.apache.seata.server.BaseSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FlushDiskModeTest extends BaseSpringBootTest {

    @Test
    public void testFindDiskModeSync() {
        FlushDiskMode mode = FlushDiskMode.findDiskMode("sync");
        Assertions.assertEquals(FlushDiskMode.SYNC_MODEL, mode);
    }

    @Test
    public void testFindDiskModeAsync() {
        FlushDiskMode mode = FlushDiskMode.findDiskMode("async");
        Assertions.assertEquals(FlushDiskMode.ASYNC_MODEL, mode);
    }

    @Test
    public void testFindDiskModeDefault() {
        FlushDiskMode mode = FlushDiskMode.findDiskMode("unknown");
        Assertions.assertEquals(FlushDiskMode.ASYNC_MODEL, mode);
    }

    @Test
    public void testFindDiskModeWithNull() {
        FlushDiskMode mode = FlushDiskMode.findDiskMode(null);
        Assertions.assertEquals(FlushDiskMode.ASYNC_MODEL, mode);
    }

    @Test
    public void testEnumValues() {
        FlushDiskMode[] modes = FlushDiskMode.values();
        Assertions.assertEquals(2, modes.length);
        Assertions.assertEquals(FlushDiskMode.SYNC_MODEL, modes[0]);
        Assertions.assertEquals(FlushDiskMode.ASYNC_MODEL, modes[1]);
    }

    @Test
    public void testValueOf() {
        FlushDiskMode syncMode = FlushDiskMode.valueOf("SYNC_MODEL");
        Assertions.assertEquals(FlushDiskMode.SYNC_MODEL, syncMode);

        FlushDiskMode asyncMode = FlushDiskMode.valueOf("ASYNC_MODEL");
        Assertions.assertEquals(FlushDiskMode.ASYNC_MODEL, asyncMode);
    }
}
