/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.BranchStatus;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.protobuf.AbstractBranchEndResponseProto;
import io.seata.core.protocol.protobuf.AbstractMessageProto;
import io.seata.core.protocol.protobuf.AbstractResultMessageProto;
import io.seata.core.protocol.protobuf.AbstractTransactionResponseProto;
import io.seata.core.protocol.protobuf.BranchRollbackResponseProto;
import io.seata.core.protocol.protobuf.BranchStatusProto;
import io.seata.core.protocol.protobuf.MessageTypeProto;
import io.seata.core.protocol.protobuf.ResultCodeProto;
import io.seata.core.protocol.protobuf.TransactionExceptionCodeProto;
import io.seata.core.protocol.transaction.BranchRollbackResponse;

/**
 * @author bystander
 * @version : BranchRollbackResponseConvertor.java, v 0.1 2019年04月25日 08:50 bystander Exp $
 */
public class BranchRollbackResponseConvertor
    implements PbConvertor<BranchRollbackResponse, BranchRollbackResponseProto> {
    @Override
    public BranchRollbackResponseProto convert2Proto(BranchRollbackResponse branchRollbackResponse) {
        final short typeCode = branchRollbackResponse.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final AbstractResultMessageProto abstractResultMessageProto = AbstractResultMessageProto.newBuilder().setMsg(
            branchRollbackResponse.getMsg())
            .setResultCode(ResultCodeProto.valueOf(branchRollbackResponse.getResultCode().name())).setAbstractMessage(
                abstractMessage).build();

        final AbstractTransactionResponseProto abstractTransactionRequestProto = AbstractTransactionResponseProto
            .newBuilder()
            .setAbstractResultMessage(abstractResultMessageProto)
            .setTransactionExceptionCode(
                TransactionExceptionCodeProto.valueOf(branchRollbackResponse.getTransactionExceptionCode().name()))
            .build();

        final AbstractBranchEndResponseProto abstractBranchEndResponse = AbstractBranchEndResponseProto
            .newBuilder().
                setAbstractTransactionResponse(abstractTransactionRequestProto)
            .setXid(branchRollbackResponse.getXid())
            .setBranchId(branchRollbackResponse.getBranchId())
            .setBranchStatus(BranchStatusProto.forNumber(branchRollbackResponse.getBranchStatus().getCode()))
            .build();

        BranchRollbackResponseProto result = BranchRollbackResponseProto.newBuilder().setAbstractBranchEndResponse(
            abstractBranchEndResponse).build();

        return result;
    }

    @Override
    public BranchRollbackResponse convert2Model(BranchRollbackResponseProto branchRollbackResponseProto) {
        BranchRollbackResponse branchCommitResponse = new BranchRollbackResponse();
        branchCommitResponse.setBranchId(branchRollbackResponseProto.getAbstractBranchEndResponse().getBranchId());
        branchCommitResponse.setBranchStatus(
            BranchStatus.get(branchRollbackResponseProto.getAbstractBranchEndResponse().getBranchStatusValue()));
        branchCommitResponse.setXid(branchRollbackResponseProto.getAbstractBranchEndResponse().getXid());
        branchCommitResponse.setMsg(
            branchRollbackResponseProto.getAbstractBranchEndResponse().getAbstractTransactionResponse()
                .getAbstractResultMessage().getMsg());
        branchCommitResponse.setResultCode(ResultCode.valueOf(
            branchRollbackResponseProto.getAbstractBranchEndResponse().getAbstractTransactionResponse()
                .getAbstractResultMessage().getResultCode().name()));

        branchCommitResponse.setTransactionExceptionCode(TransactionExceptionCode.valueOf(
            branchRollbackResponseProto.getAbstractBranchEndResponse().getAbstractTransactionResponse()
                .getTransactionExceptionCode().name()));
        return branchCommitResponse;
    }
}