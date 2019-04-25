/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.model.BranchType;
import io.seata.core.protocol.protobuf.AbstractMessageProto;
import io.seata.core.protocol.protobuf.AbstractTransactionRequestProto;
import io.seata.core.protocol.protobuf.BranchRegisterRequestProto;
import io.seata.core.protocol.protobuf.BranchTypeProto;
import io.seata.core.protocol.protobuf.MessageTypeProto;
import io.seata.core.protocol.transaction.BranchRegisterRequest;

/**
 * @author bystander
 * @version : BranchRegisterRequestConvertor.java, v 0.1 2019年04月25日 08:49 bystander Exp $
 */
public class BranchRegisterRequestConvertor implements PbConvertor<BranchRegisterRequest, BranchRegisterRequestProto> {
    @Override
    public BranchRegisterRequestProto convert2Proto(BranchRegisterRequest branchRegisterRequest) {
        final short typeCode = branchRegisterRequest.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final AbstractTransactionRequestProto abstractTransactionRequestProto = AbstractTransactionRequestProto
            .newBuilder().setAbstractMessage(
                abstractMessage).build();

        BranchRegisterRequestProto result = BranchRegisterRequestProto.newBuilder()
            .setAbstractTransactionRequest(abstractTransactionRequestProto)
            .setApplicationData(branchRegisterRequest.getApplicationData())
            .setBranchType(BranchTypeProto.valueOf(branchRegisterRequest.getBranchType().name()))
            .setLockKey(branchRegisterRequest.getLockKey())
            .setResourceId(branchRegisterRequest.getResourceId())
            .setXid(branchRegisterRequest.getXid())
            .build();
        return result;
    }

    @Override
    public BranchRegisterRequest convert2Model(BranchRegisterRequestProto branchRegisterRequestProto) {
        BranchRegisterRequest branchRegisterRequest = new BranchRegisterRequest();
        branchRegisterRequest.setApplicationData(branchRegisterRequestProto.getApplicationData());
        branchRegisterRequest.setBranchType(BranchType.valueOf(branchRegisterRequestProto.getBranchType().name()));
        branchRegisterRequest.setLockKey(branchRegisterRequestProto.getLockKey());
        branchRegisterRequest.setResourceId(branchRegisterRequestProto.getResourceId());
        branchRegisterRequest.setXid(branchRegisterRequestProto.getXid());
        return branchRegisterRequest;
    }
}