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

package com.alibaba.fescar.core.protocol;

import java.io.Serializable;

/**
 * @Author: jimin.jm@alibaba-inc.com
 * @Project: fescar-all
 * @DateTime: 2018/9/14 15:32
 * @FileName: HeartbeatMessage
 * @Description:
 */
public class HeartbeatMessage implements Serializable {
    private static final long serialVersionUID = -985316399527884899L;
    private boolean pingOrPong = true;
    public static HeartbeatMessage PING = new HeartbeatMessage(true);
    public static HeartbeatMessage PONG = new HeartbeatMessage(false);

    private HeartbeatMessage(boolean pingOrPong) {
        this.pingOrPong = pingOrPong;
    }

    @Override
    public String toString() {
        return this.pingOrPong ? "fescar ping" : "fescar pong";
    }
}
