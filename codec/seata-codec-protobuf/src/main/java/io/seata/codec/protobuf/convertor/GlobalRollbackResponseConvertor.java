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

import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.GlobalStatus;
import io.seata.core.protocol.ResultCode;
import io.seata.codec.protobuf.generated.AbstractGlobalEndResponseProto;
import io.seata.codec.protobuf.generated.AbstractMessageProto;
import io.seata.codec.protobuf.generated.AbstractResultMessageProto;
import io.seata.codec.protobuf.generated.AbstractTransactionResponseProto;
import io.seata.codec.protobuf.generated.GlobalRollbackResponseProto;
import io.seata.codec.protobuf.generated.GlobalStatusProto;
import io.seata.codec.protobuf.generated.MessageTypeProto;
import io.seata.codec.protobuf.generated.ResultCodeProto;
import io.seata.codec.protobuf.generated.TransactionExceptionCodeProto;
import io.seata.core.protocol.transaction.GlobalRollbackResponse;

/**
 * @author leizhiyuan
 */
public class GlobalRollbackResponseConvertor
    implements PbConvertor<GlobalRollbackResponse, GlobalRollbackResponseProto> {
    @Override
    public GlobalRollbackResponseProto convert2Proto(GlobalRollbackResponse globalRollbackResponse) {
        final short typeCode = globalRollbackResponse.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final String msg = globalRollbackResponse.getMsg();
        final AbstractResultMessageProto abstractResultMessageProto = AbstractResultMessageProto.newBuilder().setMsg(
            msg == null ? "" : msg)
            .setResultCode(ResultCodeProto.valueOf(globalRollbackResponse.getResultCode().name())).setAbstractMessage(
                abstractMessage).build();

        AbstractTransactionResponseProto abstractTransactionResponseProto = AbstractTransactionResponseProto
            .newBuilder().setAbstractResultMessage(abstractResultMessageProto)
            .setTransactionExceptionCode(
                TransactionExceptionCodeProto.valueOf(globalRollbackResponse.getTransactionExceptionCode().name()))
            .build();

        AbstractGlobalEndResponseProto abstractGlobalEndResponseProto = AbstractGlobalEndResponseProto.newBuilder()
            .setAbstractTransactionResponse(abstractTransactionResponseProto)
            .setGlobalStatus(GlobalStatusProto.valueOf(globalRollbackResponse.getGlobalStatus().name()))
            .build();

        GlobalRollbackResponseProto result = GlobalRollbackResponseProto.newBuilder().setAbstractGlobalEndResponse(
            abstractGlobalEndResponseProto).build();

        return result;

    }

    @Override
    public GlobalRollbackResponse convert2Model(GlobalRollbackResponseProto globalRollbackResponseProto) {
        GlobalRollbackResponse branchRegisterResponse = new GlobalRollbackResponse();
        final AbstractGlobalEndResponseProto abstractGlobalEndResponse = globalRollbackResponseProto
            .getAbstractGlobalEndResponse();
        AbstractTransactionResponseProto abstractResultMessage = abstractGlobalEndResponse
            .getAbstractTransactionResponse();
        branchRegisterResponse.setMsg(
            abstractResultMessage.getAbstractResultMessage().getMsg());
        branchRegisterResponse.setResultCode(
            ResultCode.valueOf(abstractResultMessage.getAbstractResultMessage().getResultCode().name()));
        branchRegisterResponse.setTransactionExceptionCode(TransactionExceptionCode.valueOf(
            abstractResultMessage.getTransactionExceptionCode().name()));
        branchRegisterResponse.setGlobalStatus(
            GlobalStatus.valueOf(abstractGlobalEndResponse.getGlobalStatus().name()));

        return branchRegisterResponse;

    }
}