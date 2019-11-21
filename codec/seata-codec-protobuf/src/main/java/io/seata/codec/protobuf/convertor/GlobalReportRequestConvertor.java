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
import io.seata.codec.protobuf.generated.GlobalReportRequestProto;
import io.seata.codec.protobuf.generated.GlobalStatusProto;
import io.seata.codec.protobuf.generated.MessageTypeProto;
import io.seata.core.model.GlobalStatus;
import io.seata.core.protocol.transaction.GlobalReportRequest;

/**
 * @author lorne.cl
 */
public class GlobalReportRequestConvertor implements PbConvertor<GlobalReportRequest, GlobalReportRequestProto> {
    @Override
    public GlobalReportRequestProto convert2Proto(GlobalReportRequest globalReportRequest) {
        final short typeCode = globalReportRequest.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final AbstractTransactionRequestProto abstractTransactionRequestProto = AbstractTransactionRequestProto
            .newBuilder().setAbstractMessage(abstractMessage).build();

        final String extraData = globalReportRequest.getExtraData();
        AbstractGlobalEndRequestProto abstractGlobalEndRequestProto = AbstractGlobalEndRequestProto.newBuilder()
            .setAbstractTransactionRequest(abstractTransactionRequestProto).setXid(globalReportRequest.getXid())
            .setExtraData(extraData == null ? "" : extraData).build();

        GlobalReportRequestProto result = GlobalReportRequestProto.newBuilder().setAbstractGlobalEndRequest(
            abstractGlobalEndRequestProto).setGlobalStatus(
            GlobalStatusProto.valueOf(globalReportRequest.getGlobalStatus().name())).build();

        return result;
    }

    @Override
    public GlobalReportRequest convert2Model(GlobalReportRequestProto globalReportRequestProto) {
        GlobalReportRequest globalReportRequest = new GlobalReportRequest();
        globalReportRequest.setExtraData(globalReportRequestProto.getAbstractGlobalEndRequest().getExtraData());
        globalReportRequest.setXid(globalReportRequestProto.getAbstractGlobalEndRequest().getXid());
        globalReportRequest.setGlobalStatus(GlobalStatus.valueOf(globalReportRequestProto.getGlobalStatus().name()));
        return globalReportRequest;
    }
}