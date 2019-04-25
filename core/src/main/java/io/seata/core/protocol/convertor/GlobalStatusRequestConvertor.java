/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.protobuf.AbstractGlobalEndRequestProto;
import io.seata.core.protocol.protobuf.AbstractMessageProto;
import io.seata.core.protocol.protobuf.AbstractTransactionRequestProto;
import io.seata.core.protocol.protobuf.GlobalStatusRequestProto;
import io.seata.core.protocol.protobuf.MessageTypeProto;
import io.seata.core.protocol.transaction.GlobalStatusRequest;

/**
 * @author bystander
 * @version : GlobalStatusRequestConvertor.java, v 0.1 2019年04月25日 08:50 bystander Exp $
 */
public class GlobalStatusRequestConvertor implements PbConvertor<GlobalStatusRequest, GlobalStatusRequestProto> {
    @Override
    public GlobalStatusRequestProto convert2Proto(GlobalStatusRequest globalStatusRequest) {
        final short typeCode = globalStatusRequest.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final AbstractTransactionRequestProto abstractTransactionRequestProto = AbstractTransactionRequestProto
            .newBuilder().setAbstractMessage(
                abstractMessage).build();

        AbstractGlobalEndRequestProto abstractGlobalEndRequestProto = AbstractGlobalEndRequestProto.newBuilder()
            .setAbstractTransactionRequest(abstractTransactionRequestProto)
            .setXid(globalStatusRequest.getXid())
            .setExtraData(globalStatusRequest.getExtraData())
            .build();

        GlobalStatusRequestProto result = GlobalStatusRequestProto.newBuilder().setAbstractGlobalEndRequest(
            abstractGlobalEndRequestProto).build();

        return result;
    }

    @Override
    public GlobalStatusRequest convert2Model(GlobalStatusRequestProto globalStatusRequestProto) {
        GlobalStatusRequest branchCommitRequest = new GlobalStatusRequest();
        branchCommitRequest.setExtraData(globalStatusRequestProto.getAbstractGlobalEndRequest().getExtraData());
        branchCommitRequest.setXid(globalStatusRequestProto.getAbstractGlobalEndRequest().getXid());
        return branchCommitRequest;
    }
}