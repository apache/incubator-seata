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
package io.seata.codec.protobuf.convertor;

import io.seata.codec.protobuf.generated.HeartbeatMessageProto;
import io.seata.core.protocol.HeartbeatMessage;

/**
 * @author leizhiyuan
 */
public class HeartbeatMessageConvertor implements PbConvertor<HeartbeatMessage, HeartbeatMessageProto> {
    @Override
    public HeartbeatMessageProto convert2Proto(HeartbeatMessage heartbeatMessage) {
        HeartbeatMessageProto result = HeartbeatMessageProto.newBuilder().setPing(heartbeatMessage.isPing()).build();
        return result;
    }

    @Override
    public HeartbeatMessage convert2Model(HeartbeatMessageProto heartbeatMessageProto) {
        return heartbeatMessageProto.getPing() ? HeartbeatMessage.PING : HeartbeatMessage.PONG;
    }
}