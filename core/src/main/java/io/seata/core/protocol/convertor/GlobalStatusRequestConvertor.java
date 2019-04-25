/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.core.protocol.convertor;

import io.seata.core.protocol.protobuf.AbstractGlobalEndRequestProto;
import io.seata.core.protocol.protobuf.AbstractMessageProto;
import io.seata.core.protocol.protobuf.AbstractTransactionRequestProto;
import io.seata.core.protocol.protobuf.GlobalStatusRequestProto;
import io.seata.core.protocol.protobuf.MessageTypeProto;
import io.seata.core.protocol.transaction.GlobalStatusRequest;

/**
 * @author leizhiyuan
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