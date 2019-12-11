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

import io.seata.codec.protobuf.generated.AbstractIdentifyResponseProto;
import io.seata.codec.protobuf.generated.AbstractMessageProto;
import io.seata.codec.protobuf.generated.AbstractResultMessageProto;
import io.seata.codec.protobuf.generated.MessageTypeProto;
import io.seata.codec.protobuf.generated.RegisterRMResponseProto;
import io.seata.codec.protobuf.generated.ResultCodeProto;
import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.protocol.ResultCode;

/**
 * @author leizhiyuan
 */
public class RegisterRMResponseConvertor implements PbConvertor<RegisterRMResponse, RegisterRMResponseProto> {
    @Override
    public RegisterRMResponseProto convert2Proto(RegisterRMResponse registerRMResponse) {
        final short typeCode = registerRMResponse.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final String msg = registerRMResponse.getMsg();

        //for code
        if (registerRMResponse.getResultCode() == null) {
            if (registerRMResponse.isIdentified()) {
                registerRMResponse.setResultCode(ResultCode.Success);
            } else {
                registerRMResponse.setResultCode(ResultCode.Failed);

            }
        }

        final AbstractResultMessageProto abstractResultMessageProto = AbstractResultMessageProto.newBuilder().setMsg(
            msg == null ? "" : msg).setResultCode(ResultCodeProto.valueOf(registerRMResponse.getResultCode().name()))
            .setAbstractMessage(abstractMessage).build();

        final String extraData = registerRMResponse.getExtraData();
        AbstractIdentifyResponseProto abstractIdentifyResponseProto = AbstractIdentifyResponseProto.newBuilder()
            .setAbstractResultMessage(abstractResultMessageProto).setExtraData(extraData == null ? "" : extraData)
            .setVersion(registerRMResponse.getVersion()).setIdentified(registerRMResponse.isIdentified()).build();

        RegisterRMResponseProto result = RegisterRMResponseProto.newBuilder().setAbstractIdentifyResponse(
            abstractIdentifyResponseProto).build();

        return result;
    }

    @Override
    public RegisterRMResponse convert2Model(RegisterRMResponseProto registerRMResponseProto) {
        RegisterRMResponse registerRMRequest = new RegisterRMResponse();

        AbstractIdentifyResponseProto abstractIdentifyRequestProto = registerRMResponseProto
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