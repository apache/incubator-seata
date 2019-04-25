/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.protobuf.AbstractIdentifyResponseProto;
import io.seata.core.protocol.protobuf.AbstractMessageProto;
import io.seata.core.protocol.protobuf.AbstractResultMessageProto;
import io.seata.core.protocol.protobuf.MessageTypeProto;
import io.seata.core.protocol.protobuf.RegisterRMResponseProto;
import io.seata.core.protocol.protobuf.ResultCodeProto;

/**
 * @author bystander
 * @version : RegisterRMResponseConvertor.java, v 0.1 2019年04月25日 08:51 bystander Exp $
 */
public class RegisterRMResponseConvertor implements PbConvertor<RegisterRMResponse, RegisterRMResponseProto> {
    @Override
    public RegisterRMResponseProto convert2Proto(RegisterRMResponse registerRMResponse) {
        final short typeCode = registerRMResponse.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final AbstractResultMessageProto abstractResultMessageProto = AbstractResultMessageProto.newBuilder().setMsg(
            registerRMResponse.getMsg())
            .setResultCode(ResultCodeProto.valueOf(registerRMResponse.getResultCode().name())).setAbstractMessage(
                abstractMessage).build();

        AbstractIdentifyResponseProto abstractIdentifyResponseProto = AbstractIdentifyResponseProto.newBuilder()
            .setAbstractResultMessage(abstractResultMessageProto)
            .setExtraData(registerRMResponse.getExtraData())
            .setVersion(registerRMResponse.getVersion())
            .setIdentified(registerRMResponse.isIdentified())
            .build();

        RegisterRMResponseProto result = RegisterRMResponseProto.newBuilder()
            .setAbstractIdentifyResponse(abstractIdentifyResponseProto).build();

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