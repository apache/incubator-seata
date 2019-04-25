/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.HeartbeatMessage;
import io.seata.core.protocol.protobuf.HeartbeatMessageProto;

/**
 * @author bystander
 * @version : HeartbeatMessageConvertor.java, v 0.1 2019年04月25日 08:50 bystander Exp $
 */
public class HeartbeatMessageConvertor implements PbConvertor<HeartbeatMessage, HeartbeatMessageProto> {
    @Override
    public HeartbeatMessageProto convert2Proto(HeartbeatMessage heartbeatMessage) {
        HeartbeatMessageProto result = HeartbeatMessageProto.newBuilder().setPingOrPong(heartbeatMessage.isPingOrPong())
            .build();
        return result;
    }

    @Override
    public HeartbeatMessage convert2Model(HeartbeatMessageProto heartbeatMessageProto) {
        if (heartbeatMessageProto.getPingOrPong()) {
            return HeartbeatMessage.PING;
        } else {
            return HeartbeatMessage.PONG;
        }
    }
}