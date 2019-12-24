/*
 *  Copyright 1999-2019 Seata.io Group.
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
package io.seata.codec.protobuf.convertor;

import io.seata.codec.protobuf.generated.AbstractGlobalEndRequestProto;
import io.seata.codec.protobuf.generated.AbstractMessageProto;
import io.seata.codec.protobuf.generated.AbstractTransactionRequestProto;
import io.seata.codec.protobuf.generated.GlobalStatusRequestProto;
import io.seata.codec.protobuf.generated.MessageTypeProto;
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
            .newBuilder().setAbstractMessage(abstractMessage).build();

        final String extraData = globalStatusRequest.getExtraData();
        AbstractGlobalEndRequestProto abstractGlobalEndRequestProto = AbstractGlobalEndRequestProto.newBuilder()
            .setAbstractTransactionRequest(abstractTransactionRequestProto).setXid(globalStatusRequest.getXid())
            .setExtraData(extraData == null ? "" : extraData).build();

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