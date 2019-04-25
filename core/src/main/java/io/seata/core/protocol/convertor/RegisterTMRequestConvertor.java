/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.protocol.protobuf.AbstractIdentifyRequestProto;
import io.seata.core.protocol.protobuf.AbstractMessageProto;
import io.seata.core.protocol.protobuf.MessageTypeProto;
import io.seata.core.protocol.protobuf.RegisterTMRequestProto;

/**
 * @author bystander
 * @version : RegisterTMRequestConvertor.java, v 0.1 2019年04月25日 08:51 bystander Exp $
 */
public class RegisterTMRequestConvertor implements PbConvertor<RegisterTMRequest, RegisterTMRequestProto> {
    @Override
    public RegisterTMRequestProto convert2Proto(RegisterTMRequest registerTMRequest) {
        final short typeCode = registerTMRequest.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        AbstractIdentifyRequestProto abstractIdentifyRequestProto = AbstractIdentifyRequestProto.newBuilder()
            .setAbstractMessage(abstractMessage)
            .setApplicationId(registerTMRequest.getApplicationId())
            .setExtraData(registerTMRequest.getExtraData())
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