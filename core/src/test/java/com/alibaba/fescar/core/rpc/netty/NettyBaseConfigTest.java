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

package com.alibaba.fescar.core.rpc.netty;

import org.junit.Test;

/**
 * @Author: jimin.jm@alibaba-inc.com
 * @Project: feats-all
 * @DateTime: 2019/1/7 2:04 PM
 * @FileName: NettyBaseConfigTest
 * @Description:
 * @date 2019/01/07
 */
public class NettyBaseConfigTest {
    @Test
    public void name() {
        NettyBaseConfig nettyBaseConfig = new NettyBaseConfig();
        System.out.print("test static .");
    }
}