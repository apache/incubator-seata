/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.serializer.protobuf.convertor;

import org.apache.seata.serializer.protobuf.generated.AbstractGlobalEndRequestProto;
import org.apache.seata.serializer.protobuf.generated.AbstractMessageProto;
import org.apache.seata.serializer.protobuf.generated.AbstractTransactionRequestProto;
import org.apache.seata.serializer.protobuf.generated.GlobalReportRequestProto;
import org.apache.seata.serializer.protobuf.generated.GlobalStatusProto;
import org.apache.seata.serializer.protobuf.generated.MessageTypeProto;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.protocol.transaction.GlobalReportRequest;


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
