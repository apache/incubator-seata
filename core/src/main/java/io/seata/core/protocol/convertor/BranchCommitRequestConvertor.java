/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.model.BranchType;
import io.seata.core.protocol.protobuf.AbstractBranchEndRequestProto;
import io.seata.core.protocol.protobuf.AbstractMessageProto;
import io.seata.core.protocol.protobuf.AbstractTransactionRequestProto;
import io.seata.core.protocol.protobuf.BranchCommitRequestProto;
import io.seata.core.protocol.protobuf.BranchTypeProto;
import io.seata.core.protocol.protobuf.MessageTypeProto;
import io.seata.core.protocol.transaction.BranchCommitRequest;

/**
 * @author bystander
 * @version : BranchCommitRequestConvertor.java, v 0.1 2019年04月25日 08:49 bystander Exp $
 */
public class BranchCommitRequestConvertor implements PbConvertor<BranchCommitRequest, BranchCommitRequestProto> {
    @Override
    public BranchCommitRequestProto convert2Proto(BranchCommitRequest branchCommitRequest) {
        final short typeCode = branchCommitRequest.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final AbstractTransactionRequestProto abstractTransactionRequestProto = AbstractTransactionRequestProto
            .newBuilder().setAbstractMessage(
                abstractMessage).build();

        final AbstractBranchEndRequestProto abstractBranchEndRequestProto = AbstractBranchEndRequestProto
            .newBuilder().
                setAbstractTransactionRequest(abstractTransactionRequestProto)
            .setXid(branchCommitRequest.getXid())
            .setBranchId(branchCommitRequest.getBranchId())
            .setBranchType(BranchTypeProto.valueOf(branchCommitRequest.getBranchType().name()))
            .setApplicationData(branchCommitRequest.getApplicationData())
            .setResourceId(branchCommitRequest.getResourceId())
            .build();

        BranchCommitRequestProto result = BranchCommitRequestProto.newBuilder()
            .setAbstractBranchEndRequest(abstractBranchEndRequestProto)
            .build();
        return result;
    }

    @Override
    public BranchCommitRequest convert2Model(BranchCommitRequestProto branchCommitRequestProto) {
        BranchCommitRequest branchCommitRequest = new BranchCommitRequest();
        branchCommitRequest.setApplicationData(
            branchCommitRequestProto.getAbstractBranchEndRequest().getApplicationData());
        branchCommitRequest.setBranchId(branchCommitRequestProto.getAbstractBranchEndRequest().getBranchId());
        branchCommitRequest.setResourceId(branchCommitRequestProto.getAbstractBranchEndRequest().getResourceId());
        branchCommitRequest.setXid(branchCommitRequestProto.getAbstractBranchEndRequest().getXid());
        branchCommitRequest.setBranchType(
            BranchType.valueOf(branchCommitRequestProto.getAbstractBranchEndRequest().getBranchType().name()));

        return branchCommitRequest;
    }
}