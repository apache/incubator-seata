/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2019 All Rights Reserved.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.protobuf.AbstractMessageProto;
import io.seata.core.protocol.protobuf.AbstractTransactionRequestProto;
import io.seata.core.protocol.protobuf.GlobalBeginRequestProto;
import io.seata.core.protocol.protobuf.MessageTypeProto;
import io.seata.core.protocol.transaction.GlobalBeginRequest;

/**
 * @author bystander
 * @version : GlobalBeginRequestConvertor.java, v 0.1 2019年04月23日 19:42 bystander Exp $
 */
public class GlobalBeginRequestConvertor implements PbConvertor<GlobalBeginRequest, GlobalBeginRequestProto> {

    @Override
    public GlobalBeginRequestProto convert2Proto(GlobalBeginRequest globalBeginRequest) {
        final short typeCode = globalBeginRequest.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final AbstractTransactionRequestProto abstractTransactionRequestProto = AbstractTransactionRequestProto
            .newBuilder().setAbstractMessage(
                abstractMessage).build();

        GlobalBeginRequestProto result = GlobalBeginRequestProto.newBuilder()
            .setTimeout(globalBeginRequest.getTimeout())
            .setTransactionName(globalBeginRequest.getTransactionName())
            .setAbstractTransactionRequest(abstractTransactionRequestProto)
            .build();
        return result;
    }

    @Override
    public GlobalBeginRequest convert2Model(GlobalBeginRequestProto globalBeginRequestProto) {
        GlobalBeginRequest globalBeginRequest = new GlobalBeginRequest();
        globalBeginRequest.setTimeout(globalBeginRequestProto.getTimeout());
        globalBeginRequest.setTransactionName(globalBeginRequestProto.getTransactionName());
        return globalBeginRequest;
    }
}