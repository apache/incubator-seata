/*
 * Copyright 1999-2019 Seata.io Group.
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
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.protobuf.AbstractIdentifyRequestProto;
import io.seata.core.protocol.protobuf.AbstractMessageProto;
import io.seata.core.protocol.protobuf.MessageTypeProto;
import io.seata.core.protocol.protobuf.RegisterRMRequestProto;

/**
 * @author leizhiyuan
 */
public class RegisterRMRequestConvertor implements PbConvertor<RegisterRMRequest, RegisterRMRequestProto> {
    @Override
    public RegisterRMRequestProto convert2Proto(RegisterRMRequest registerRMRequest) {
        final short typeCode = registerRMRequest.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        AbstractIdentifyRequestProto abstractIdentifyRequestProto = AbstractIdentifyRequestProto.newBuilder()
            .setAbstractMessage(abstractMessage)
            .setApplicationId(registerRMRequest.getApplicationId())
            .setExtraData(registerRMRequest.getExtraData())
            .setTransactionServiceGroup(registerRMRequest.getTransactionServiceGroup())
            .setVersion(registerRMRequest.getVersion())
            .build();
        RegisterRMRequestProto result = RegisterRMRequestProto.newBuilder().setAbstractIdentifyRequest(
            abstractIdentifyRequestProto)
            .setResourceIds(registerRMRequest.getResourceIds()).build();

        return result;
    }

    @Override
    public RegisterRMRequest convert2Model(RegisterRMRequestProto registerRMRequestProto) {
        RegisterRMRequest registerRMRequest = new RegisterRMRequest();

        AbstractIdentifyRequestProto abstractIdentifyRequest = registerRMRequestProto.getAbstractIdentifyRequest();
        registerRMRequest.setResourceIds(registerRMRequestProto.getResourceIds());
        registerRMRequest.setApplicationId(abstractIdentifyRequest.getApplicationId());
        registerRMRequest.setExtraData(abstractIdentifyRequest.getExtraData());
        registerRMRequest.setTransactionServiceGroup(abstractIdentifyRequest.getTransactionServiceGroup());
        registerRMRequest.setVersion(abstractIdentifyRequest.getVersion());

        return registerRMRequest;
    }
}