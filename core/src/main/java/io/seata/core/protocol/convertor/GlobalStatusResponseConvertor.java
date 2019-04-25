/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.GlobalStatus;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.protobuf.AbstractGlobalEndResponseProto;
import io.seata.core.protocol.protobuf.AbstractMessageProto;
import io.seata.core.protocol.protobuf.AbstractResultMessageProto;
import io.seata.core.protocol.protobuf.AbstractTransactionResponseProto;
import io.seata.core.protocol.protobuf.GlobalStatusProto;
import io.seata.core.protocol.protobuf.GlobalStatusResponseProto;
import io.seata.core.protocol.protobuf.MessageTypeProto;
import io.seata.core.protocol.protobuf.ResultCodeProto;
import io.seata.core.protocol.protobuf.TransactionExceptionCodeProto;
import io.seata.core.protocol.transaction.GlobalStatusResponse;

/**
 * @author bystander
 * @version : GlobalStatusResponseConvertor.java, v 0.1 2019年04月25日 08:50 bystander Exp $
 */
public class GlobalStatusResponseConvertor implements PbConvertor<GlobalStatusResponse, GlobalStatusResponseProto> {
    @Override
    public GlobalStatusResponseProto convert2Proto(GlobalStatusResponse globalStatusResponse) {
        final short typeCode = globalStatusResponse.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final AbstractResultMessageProto abstractResultMessageProto = AbstractResultMessageProto.newBuilder().setMsg(
            globalStatusResponse.getMsg())
            .setResultCode(ResultCodeProto.valueOf(globalStatusResponse.getResultCode().name())).setAbstractMessage(
                abstractMessage).build();

        AbstractTransactionResponseProto abstractTransactionResponseProto = AbstractTransactionResponseProto
            .newBuilder().setAbstractResultMessage(abstractResultMessageProto)
            .setTransactionExceptionCode(
                TransactionExceptionCodeProto.valueOf(globalStatusResponse.getTransactionExceptionCode().name()))
            .build();

        AbstractGlobalEndResponseProto abstractGlobalEndResponseProto = AbstractGlobalEndResponseProto.newBuilder()
            .setAbstractTransactionResponse(abstractTransactionResponseProto)
            .setGlobalStatus(GlobalStatusProto.valueOf(globalStatusResponse.getGlobalStatus().name()))
            .build();

        GlobalStatusResponseProto result = GlobalStatusResponseProto.newBuilder().setAbstractGlobalEndResponse(
            abstractGlobalEndResponseProto).build();
        return result;
    }

    @Override
    public GlobalStatusResponse convert2Model(GlobalStatusResponseProto globalStatusResponseProto) {
        GlobalStatusResponse branchRegisterResponse = new GlobalStatusResponse();
        final AbstractGlobalEndResponseProto abstractGlobalEndResponse = globalStatusResponseProto
            .getAbstractGlobalEndResponse();
        AbstractTransactionResponseProto abstractResultMessage = abstractGlobalEndResponse
            .getAbstractTransactionResponse();
        branchRegisterResponse.setMsg(
            abstractResultMessage.getAbstractResultMessage().getMsg());
        branchRegisterResponse.setResultCode(
            ResultCode.valueOf(abstractResultMessage.getAbstractResultMessage().getResultCode().name()));
        branchRegisterResponse.setTransactionExceptionCode(TransactionExceptionCode.valueOf(
            abstractResultMessage.getTransactionExceptionCode().name()));
        branchRegisterResponse.setGlobalStatus(
            GlobalStatus.valueOf(abstractGlobalEndResponse.getGlobalStatus().name()));

        return branchRegisterResponse;

    }
}