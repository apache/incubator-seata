/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
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
 * @author bystander
 * @version : RegisterTMResponseConvertor.java, v 0.1 2019年04月25日 08:51 bystander Exp $
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