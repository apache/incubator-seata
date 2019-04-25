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
import io.seata.core.protocol.protobuf.BranchRegisterResponseProto;
import io.seata.core.protocol.protobuf.MessageTypeProto;
import io.seata.core.protocol.protobuf.ResultCodeProto;
import io.seata.core.protocol.protobuf.TransactionExceptionCodeProto;
import io.seata.core.protocol.transaction.BranchRegisterResponse;

/**
 * @author bystander
 * @version : BranchRegisterResponseConvertor.java, v 0.1 2019年04月25日 08:49 bystander Exp $
 */
public class BranchRegisterResponseConvertor
    implements PbConvertor<BranchRegisterResponse, BranchRegisterResponseProto> {
    @Override
    public BranchRegisterResponseProto convert2Proto(BranchRegisterResponse branchRegisterResponse) {
        final short typeCode = branchRegisterResponse.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final AbstractResultMessageProto abstractResultMessageProto = AbstractResultMessageProto.newBuilder().setMsg(
            branchRegisterResponse.getMsg())
            .setResultCode(ResultCodeProto.valueOf(branchRegisterResponse.getResultCode().name())).setAbstractMessage(
                abstractMessage).build();

        AbstractTransactionResponseProto abstractTransactionResponseProto = AbstractTransactionResponseProto
            .newBuilder().setAbstractResultMessage(abstractResultMessageProto)
            .setTransactionExceptionCode(
                TransactionExceptionCodeProto.valueOf(branchRegisterResponse.getTransactionExceptionCode().name()))
            .build();

        BranchRegisterResponseProto result = BranchRegisterResponseProto.newBuilder().setAbstractTransactionResponse(
            abstractTransactionResponseProto)
            .setBranchId(branchRegisterResponse.getBranchId()).build();

        return result;
    }

    @Override
    public BranchRegisterResponse convert2Model(BranchRegisterResponseProto branchRegisterResponseProto) {
        BranchRegisterResponse branchRegisterResponse = new BranchRegisterResponse();
        branchRegisterResponse.setBranchId(branchRegisterResponseProto.getBranchId());
        final AbstractResultMessageProto abstractResultMessage = branchRegisterResponseProto
            .getAbstractTransactionResponse().getAbstractResultMessage();
        branchRegisterResponse.setMsg(
            abstractResultMessage.getMsg());
        branchRegisterResponse.setResultCode(ResultCode.valueOf(abstractResultMessage.getResultCode().name()));
        branchRegisterResponse.setTransactionExceptionCode(TransactionExceptionCode.valueOf(
            branchRegisterResponseProto.getAbstractTransactionResponse().getTransactionExceptionCode().name()));

        return branchRegisterResponse;
    }
}