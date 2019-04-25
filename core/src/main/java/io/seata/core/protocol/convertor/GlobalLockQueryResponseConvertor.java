/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.protobuf.AbstractMessageProto;
import io.seata.core.protocol.protobuf.AbstractResultMessageProto;
import io.seata.core.protocol.protobuf.AbstractTransactionResponseProto;
import io.seata.core.protocol.protobuf.GlobalLockQueryResponseProto;
import io.seata.core.protocol.protobuf.MessageTypeProto;
import io.seata.core.protocol.protobuf.ResultCodeProto;
import io.seata.core.protocol.protobuf.TransactionExceptionCodeProto;
import io.seata.core.protocol.transaction.GlobalLockQueryResponse;

/**
 * @author bystander
 * @version : GlobalLockQueryResponseConvertor.java, v 0.1 2019年04月25日 08:50 bystander Exp $
 */
public class GlobalLockQueryResponseConvertor
    implements PbConvertor<GlobalLockQueryResponse, GlobalLockQueryResponseProto> {
    @Override
    public GlobalLockQueryResponseProto convert2Proto(GlobalLockQueryResponse globalLockQueryResponse) {
        final short typeCode = globalLockQueryResponse.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final AbstractResultMessageProto abstractResultMessageProto = AbstractResultMessageProto.newBuilder().setMsg(
            globalLockQueryResponse.getMsg())
            .setResultCode(ResultCodeProto.valueOf(globalLockQueryResponse.getResultCode().name())).setAbstractMessage(
                abstractMessage).build();

        AbstractTransactionResponseProto abstractTransactionResponseProto = AbstractTransactionResponseProto
            .newBuilder().setAbstractResultMessage(abstractResultMessageProto)
            .setTransactionExceptionCode(
                TransactionExceptionCodeProto.valueOf(globalLockQueryResponse.getTransactionExceptionCode().name()))
            .build();

        GlobalLockQueryResponseProto result = GlobalLockQueryResponseProto.newBuilder().setLockable(
            globalLockQueryResponse.isLockable())
            .setAbstractTransactionResponse(abstractTransactionResponseProto).build();

        return result;
    }

    @Override
    public GlobalLockQueryResponse convert2Model(GlobalLockQueryResponseProto globalLockQueryResponseProto) {

        GlobalLockQueryResponse branchRegisterResponse = new GlobalLockQueryResponse();
        AbstractTransactionResponseProto branchRegisterResponseProto = globalLockQueryResponseProto
            .getAbstractTransactionResponse();
        final AbstractResultMessageProto abstractResultMessage = branchRegisterResponseProto.getAbstractResultMessage();
        branchRegisterResponse.setMsg(
            abstractResultMessage.getMsg());
        branchRegisterResponse.setResultCode(ResultCode.valueOf(abstractResultMessage.getResultCode().name()));
        branchRegisterResponse.setTransactionExceptionCode(TransactionExceptionCode.valueOf(
            branchRegisterResponseProto.getTransactionExceptionCode().name()));
        branchRegisterResponse.setLockable(globalLockQueryResponseProto.getLockable());
        return branchRegisterResponse;
    }
}