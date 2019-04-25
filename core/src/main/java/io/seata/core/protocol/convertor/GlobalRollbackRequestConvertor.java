/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.protobuf.AbstractGlobalEndRequestProto;
import io.seata.core.protocol.protobuf.AbstractMessageProto;
import io.seata.core.protocol.protobuf.AbstractTransactionRequestProto;
import io.seata.core.protocol.protobuf.GlobalRollbackRequestProto;
import io.seata.core.protocol.protobuf.MessageTypeProto;
import io.seata.core.protocol.transaction.GlobalRollbackRequest;

/**
 * @author bystander
 * @version : GlobalRollbackRequestConvertor.java, v 0.1 2019年04月25日 08:50 bystander Exp $
 */
public class GlobalRollbackRequestConvertor implements PbConvertor<GlobalRollbackRequest, GlobalRollbackRequestProto> {
    @Override
    public GlobalRollbackRequestProto convert2Proto(GlobalRollbackRequest globalRollbackRequest) {
        final short typeCode = globalRollbackRequest.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final AbstractTransactionRequestProto abstractTransactionRequestProto = AbstractTransactionRequestProto
            .newBuilder().setAbstractMessage(
                abstractMessage).build();

        AbstractGlobalEndRequestProto abstractGlobalEndRequestProto = AbstractGlobalEndRequestProto.newBuilder()
            .setAbstractTransactionRequest(abstractTransactionRequestProto)
            .setXid(globalRollbackRequest.getXid())
            .setExtraData(globalRollbackRequest.getExtraData())
            .build();

        GlobalRollbackRequestProto result = GlobalRollbackRequestProto.newBuilder().setAbstractGlobalEndRequest(
            abstractGlobalEndRequestProto).build();

        return result;
    }

    @Override
    public GlobalRollbackRequest convert2Model(GlobalRollbackRequestProto globalRollbackRequestProto) {
        GlobalRollbackRequest branchCommitRequest = new GlobalRollbackRequest();
        branchCommitRequest.setExtraData(globalRollbackRequestProto.getAbstractGlobalEndRequest().getExtraData());
        branchCommitRequest.setXid(globalRollbackRequestProto.getAbstractGlobalEndRequest().getXid());
        return branchCommitRequest;
    }
}