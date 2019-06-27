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

import io.seata.core.protocol.RegisterTMRequest;
import io.seata.codec.protobuf.generated.AbstractIdentifyRequestProto;
import io.seata.codec.protobuf.generated.AbstractMessageProto;
import io.seata.codec.protobuf.generated.MessageTypeProto;
import io.seata.codec.protobuf.generated.RegisterTMRequestProto;

/**
 * @author leizhiyuan
 */
public class RegisterTMRequestConvertor implements PbConvertor<RegisterTMRequest, RegisterTMRequestProto> {
    @Override
    public RegisterTMRequestProto convert2Proto(RegisterTMRequest registerTMRequest) {
        final short typeCode = registerTMRequest.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final String extraData = registerTMRequest.getExtraData();
        AbstractIdentifyRequestProto abstractIdentifyRequestProto = AbstractIdentifyRequestProto.newBuilder()
            .setAbstractMessage(abstractMessage)
            .setApplicationId(registerTMRequest.getApplicationId())
            .setExtraData(extraData==null?"":extraData)
            .setTransactionServiceGroup(registerTMRequest.getTransactionServiceGroup())
            .setVersion(registerTMRequest.getVersion())
            .build();

        RegisterTMRequestProto result = RegisterTMRequestProto.newBuilder().setAbstractIdentifyRequest(
            abstractIdentifyRequestProto).build();

        return result;
    }

    @Override
    public RegisterTMRequest convert2Model(RegisterTMRequestProto registerTMRequestProto) {
        RegisterTMRequest registerRMRequest = new RegisterTMRequest();

        AbstractIdentifyRequestProto abstractIdentifyRequest = registerTMRequestProto.getAbstractIdentifyRequest();
        registerRMRequest.setApplicationId(abstractIdentifyRequest.getApplicationId());
        registerRMRequest.setExtraData(abstractIdentifyRequest.getExtraData());
        registerRMRequest.setTransactionServiceGroup(abstractIdentifyRequest.getTransactionServiceGroup());
        registerRMRequest.setVersion(abstractIdentifyRequest.getVersion());

        return registerRMRequest;
    }
}