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

import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.serializer.protobuf.generated.AbstractMessageProto;
import org.apache.seata.serializer.protobuf.generated.AbstractResultMessageProto;
import org.apache.seata.serializer.protobuf.generated.AbstractTransactionResponseProto;
import org.apache.seata.serializer.protobuf.generated.BranchRegisterResponseProto;
import org.apache.seata.serializer.protobuf.generated.MessageTypeProto;
import org.apache.seata.serializer.protobuf.generated.ResultCodeProto;
import org.apache.seata.serializer.protobuf.generated.TransactionExceptionCodeProto;
import org.apache.seata.core.protocol.transaction.BranchRegisterResponse;


public class BranchRegisterResponseConvertor
    implements PbConvertor<BranchRegisterResponse, BranchRegisterResponseProto> {
    @Override
    public BranchRegisterResponseProto convert2Proto(BranchRegisterResponse branchRegisterResponse) {
        final short typeCode = branchRegisterResponse.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final String msg = branchRegisterResponse.getMsg();
        final AbstractResultMessageProto abstractResultMessageProto = AbstractResultMessageProto.newBuilder().setMsg(
            msg == null ? "" : msg).setResultCode(
            ResultCodeProto.valueOf(branchRegisterResponse.getResultCode().name())).setAbstractMessage(abstractMessage)
            .build();

        AbstractTransactionResponseProto abstractTransactionResponseProto = AbstractTransactionResponseProto
            .newBuilder().setAbstractResultMessage(abstractResultMessageProto).setTransactionExceptionCode(
                TransactionExceptionCodeProto.valueOf(branchRegisterResponse.getTransactionExceptionCode().name()))
            .build();

        BranchRegisterResponseProto result = BranchRegisterResponseProto.newBuilder().setAbstractTransactionResponse(
            abstractTransactionResponseProto).setBranchId(branchRegisterResponse.getBranchId()).build();

        return result;
    }

    @Override
    public BranchRegisterResponse convert2Model(BranchRegisterResponseProto branchRegisterResponseProto) {
        BranchRegisterResponse branchRegisterResponse = new BranchRegisterResponse();
        branchRegisterResponse.setBranchId(branchRegisterResponseProto.getBranchId());
        final AbstractResultMessageProto abstractResultMessage = branchRegisterResponseProto
            .getAbstractTransactionResponse().getAbstractResultMessage();
        branchRegisterResponse.setMsg(abstractResultMessage.getMsg());
        branchRegisterResponse.setResultCode(ResultCode.valueOf(abstractResultMessage.getResultCode().name()));
        branchRegisterResponse.setTransactionExceptionCode(TransactionExceptionCode.valueOf(
            branchRegisterResponseProto.getAbstractTransactionResponse().getTransactionExceptionCode().name()));

        return branchRegisterResponse;
    }
}
