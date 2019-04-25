/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.protobuf.AbstractIdentifyRequestProto;
import io.seata.core.protocol.protobuf.AbstractMessageProto;
import io.seata.core.protocol.protobuf.MessageTypeProto;
import io.seata.core.protocol.protobuf.RegisterRMRequestProto;

/**
 * @author bystander
 * @version : RegisterRMRequestConvertor.java, v 0.1 2019年04月25日 08:51 bystander Exp $
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