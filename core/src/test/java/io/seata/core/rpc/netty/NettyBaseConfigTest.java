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
package io.seata.core.rpc.netty;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type Netty base config test.
 *
 * @author slievrly
 * @author wang.liang
 */
class NettyBaseConfigTest {
    /**
     * Name.
     */
    @Test
    void name() {
        NettyBaseConfig nettyBaseConfig = new NettyBaseConfig();
        System.out.print("test static .");
    }

    @Test
    void test_enum_WorkThreadMode_getModeByName() {
        for (NettyBaseConfig.WorkThreadMode value : NettyBaseConfig.WorkThreadMode.values()) {
            Assertions.assertEquals(NettyBaseConfig.WorkThreadMode.getModeByName(value.name().toLowerCase()), value);
        }
        Assertions.assertNull(NettyBaseConfig.WorkThreadMode.getModeByName(null));
        Assertions.assertNull(NettyBaseConfig.WorkThreadMode.getModeByName("null"));
    }
}
