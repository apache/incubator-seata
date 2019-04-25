/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.protobuf.AbstractGlobalEndRequestProto;
import io.seata.core.protocol.protobuf.AbstractMessageProto;
import io.seata.core.protocol.protobuf.AbstractTransactionRequestProto;
import io.seata.core.protocol.protobuf.GlobalCommitRequestProto;
import io.seata.core.protocol.protobuf.MessageTypeProto;
import io.seata.core.protocol.transaction.GlobalCommitRequest;

/**
 * @author bystander
 * @version : GlobalCommitRequestConvertor.java, v 0.1 2019年04月25日 08:50 bystander Exp $
 */
public class GlobalCommitRequestConvertor implements PbConvertor<GlobalCommitRequest, GlobalCommitRequestProto> {
    @Override
    public GlobalCommitRequestProto convert2Proto(GlobalCommitRequest globalCommitRequest) {
        final short typeCode = globalCommitRequest.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final AbstractTransactionRequestProto abstractTransactionRequestProto = AbstractTransactionRequestProto
            .newBuilder().setAbstractMessage(
                abstractMessage).build();

        AbstractGlobalEndRequestProto abstractGlobalEndRequestProto = AbstractGlobalEndRequestProto.newBuilder()
            .setAbstractTransactionRequest(abstractTransactionRequestProto)
            .setXid(globalCommitRequest.getXid())
            .setExtraData(globalCommitRequest.getExtraData())
            .build();

        GlobalCommitRequestProto result = GlobalCommitRequestProto.newBuilder().setAbstractGlobalEndRequest(
            abstractGlobalEndRequestProto).build();

        return result;

    }

    @Override
    public GlobalCommitRequest convert2Model(GlobalCommitRequestProto globalCommitRequestProto) {
        GlobalCommitRequest branchCommitRequest = new GlobalCommitRequest();
        branchCommitRequest.setExtraData(globalCommitRequestProto.getAbstractGlobalEndRequest().getExtraData());
        branchCommitRequest.setXid(globalCommitRequestProto.getAbstractGlobalEndRequest().getXid());
        return branchCommitRequest;
    }
}