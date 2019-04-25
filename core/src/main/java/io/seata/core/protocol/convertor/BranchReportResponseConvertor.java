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
import io.seata.core.protocol.protobuf.BranchReportResponseProto;
import io.seata.core.protocol.protobuf.MessageTypeProto;
import io.seata.core.protocol.protobuf.ResultCodeProto;
import io.seata.core.protocol.protobuf.TransactionExceptionCodeProto;
import io.seata.core.protocol.transaction.BranchReportResponse;

/**
 * @author bystander
 * @version : BranchReportResponseConvertor.java, v 0.1 2019年04月25日 08:49 bystander Exp $
 */
public class BranchReportResponseConvertor implements PbConvertor<BranchReportResponse, BranchReportResponseProto> {
    @Override
    public BranchReportResponseProto convert2Proto(BranchReportResponse branchReportResponse) {
        final short typeCode = branchReportResponse.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final AbstractResultMessageProto abstractResultMessageProto = AbstractResultMessageProto.newBuilder().setMsg(
            branchReportResponse.getMsg())
            .setResultCode(ResultCodeProto.valueOf(branchReportResponse.getResultCode().name())).setAbstractMessage(
                abstractMessage).build();

        AbstractTransactionResponseProto abstractTransactionResponseProto = AbstractTransactionResponseProto
            .newBuilder().setAbstractResultMessage(abstractResultMessageProto)
            .setTransactionExceptionCode(
                TransactionExceptionCodeProto.valueOf(branchReportResponse.getTransactionExceptionCode().name()))
            .build();

        BranchReportResponseProto result = BranchReportResponseProto.newBuilder()
            .setAbstractTransactionResponse(abstractTransactionResponseProto).build();

        return result;
    }

    @Override
    public BranchReportResponse convert2Model(BranchReportResponseProto branchReportResponseProto) {
        BranchReportResponse branchRegisterResponse = new BranchReportResponse();
        final AbstractResultMessageProto abstractResultMessage = branchReportResponseProto
            .getAbstractTransactionResponse().getAbstractResultMessage();
        branchRegisterResponse.setMsg(
            abstractResultMessage.getMsg());
        branchRegisterResponse.setResultCode(ResultCode.valueOf(abstractResultMessage.getResultCode().name()));
        branchRegisterResponse.setTransactionExceptionCode(TransactionExceptionCode.valueOf(
            branchReportResponseProto.getAbstractTransactionResponse().getTransactionExceptionCode().name()));

        return branchRegisterResponse;
    }
}