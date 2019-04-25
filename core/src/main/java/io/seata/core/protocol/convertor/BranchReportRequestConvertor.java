/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.protocol.protobuf.AbstractMessageProto;
import io.seata.core.protocol.protobuf.AbstractTransactionRequestProto;
import io.seata.core.protocol.protobuf.BranchReportRequestProto;
import io.seata.core.protocol.protobuf.BranchStatusProto;
import io.seata.core.protocol.protobuf.BranchTypeProto;
import io.seata.core.protocol.protobuf.MessageTypeProto;
import io.seata.core.protocol.transaction.BranchReportRequest;

/**
 * @author bystander
 * @version : BranchReportRequestConvertor.java, v 0.1 2019年04月25日 08:49 bystander Exp $
 */
public class BranchReportRequestConvertor implements PbConvertor<BranchReportRequest, BranchReportRequestProto> {
    @Override
    public BranchReportRequestProto convert2Proto(BranchReportRequest branchReportRequest) {
        final short typeCode = branchReportRequest.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final AbstractTransactionRequestProto abstractTransactionRequestProto = AbstractTransactionRequestProto
            .newBuilder().setAbstractMessage(
                abstractMessage).build();

        BranchReportRequestProto result = BranchReportRequestProto.newBuilder().setAbstractTransactionRequest(
            abstractTransactionRequestProto)
            .setXid(branchReportRequest.getXid())
            .setBranchId(branchReportRequest.getBranchId())
            .setBranchType(BranchTypeProto.valueOf(branchReportRequest.getBranchType().name()))
            .setApplicationData(branchReportRequest.getApplicationData())
            .setResourceId(branchReportRequest.getResourceId())
            .setStatus(BranchStatusProto.valueOf(branchReportRequest.getStatus().name()))
            .build();

        return result;
    }

    @Override
    public BranchReportRequest convert2Model(BranchReportRequestProto branchReportRequestProto) {
        BranchReportRequest branchReportRequest = new BranchReportRequest();
        branchReportRequest.setApplicationData(
            branchReportRequestProto.getApplicationData());
        branchReportRequest.setBranchId(branchReportRequestProto.getBranchId());
        branchReportRequest.setResourceId(branchReportRequestProto.getResourceId());
        branchReportRequest.setXid(branchReportRequestProto.getXid());
        branchReportRequest.setBranchType(
            BranchType.valueOf(branchReportRequestProto.getBranchType().name()));
        branchReportRequest.setStatus(BranchStatus.valueOf(branchReportRequestProto.getStatus().name()));
        return branchReportRequest;
    }
}