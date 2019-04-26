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
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.RegisterTMResponse;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.protobuf.AbstractIdentifyResponseProto;
import io.seata.core.protocol.protobuf.AbstractMessageProto;
import io.seata.core.protocol.protobuf.AbstractResultMessageProto;
import io.seata.core.protocol.protobuf.MessageTypeProto;
import io.seata.core.protocol.protobuf.RegisterTMResponseProto;
import io.seata.core.protocol.protobuf.ResultCodeProto;

/**
 * @author leizhiyuan
 */
public class RegisterTMResponseConvertor implements PbConvertor<RegisterTMResponse, RegisterTMResponseProto> {
    @Override
    public RegisterTMResponseProto convert2Proto(RegisterTMResponse registerTMResponse) {
        final short typeCode = registerTMResponse.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final AbstractResultMessageProto abstractResultMessageProto = AbstractResultMessageProto.newBuilder().setMsg(
            registerTMResponse.getMsg())
            .setResultCode(ResultCodeProto.valueOf(registerTMResponse.getResultCode().name())).setAbstractMessage(
                abstractMessage).build();

        AbstractIdentifyResponseProto abstractIdentifyResponseProto = AbstractIdentifyResponseProto.newBuilder()
            .setAbstractResultMessage(abstractResultMessageProto)
            .setExtraData(registerTMResponse.getExtraData())
            .setVersion(registerTMResponse.getVersion())
            .setIdentified(registerTMResponse.isIdentified())
            .build();

        RegisterTMResponseProto result = RegisterTMResponseProto.newBuilder().setAbstractIdentifyResponse(
            abstractIdentifyResponseProto).build();

        return result;
    }

    @Override
    public RegisterTMResponse convert2Model(RegisterTMResponseProto registerTMResponseProto) {
        RegisterTMResponse registerRMRequest = new RegisterTMResponse();

        AbstractIdentifyResponseProto abstractIdentifyRequestProto = registerTMResponseProto
            .getAbstractIdentifyResponse();
        registerRMRequest.setExtraData(abstractIdentifyRequestProto.getExtraData());
        registerRMRequest.setVersion(abstractIdentifyRequestProto.getVersion());
        registerRMRequest.setIdentified(abstractIdentifyRequestProto.getIdentified());

        registerRMRequest.setMsg(abstractIdentifyRequestProto.getAbstractResultMessage().getMsg());
        registerRMRequest.setResultCode(
            ResultCode.valueOf(abstractIdentifyRequestProto.getAbstractResultMessage().getResultCode().name()));

        return registerRMRequest;

    }
}