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
import io.seata.core.protocol.ResultCode;
import io.seata.codec.protobuf.generated.AbstractMessageProto;
import io.seata.codec.protobuf.generated.AbstractResultMessageProto;
import io.seata.codec.protobuf.generated.AbstractTransactionResponseProto;
import io.seata.codec.protobuf.generated.GlobalLockQueryResponseProto;
import io.seata.codec.protobuf.generated.MessageTypeProto;
import io.seata.codec.protobuf.generated.ResultCodeProto;
import io.seata.codec.protobuf.generated.TransactionExceptionCodeProto;
import io.seata.core.protocol.transaction.GlobalLockQueryResponse;

/**
 * @author leizhiyuan
 */
public class GlobalLockQueryResponseConvertor
    implements PbConvertor<GlobalLockQueryResponse, GlobalLockQueryResponseProto> {
    @Override
    public GlobalLockQueryResponseProto convert2Proto(GlobalLockQueryResponse globalLockQueryResponse) {
        final short typeCode = globalLockQueryResponse.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final String msg = globalLockQueryResponse.getMsg();
        final AbstractResultMessageProto abstractResultMessageProto = AbstractResultMessageProto.newBuilder().setMsg(
            msg == null ? "" : msg)
            .setResultCode(ResultCodeProto.valueOf(globalLockQueryResponse.getResultCode().name())).setAbstractMessage(
                abstractMessage).build();

        AbstractTransactionResponseProto abstractTransactionResponseProto = AbstractTransactionResponseProto
            .newBuilder().setAbstractResultMessage(abstractResultMessageProto)
            .setTransactionExceptionCode(
                TransactionExceptionCodeProto.valueOf(globalLockQueryResponse.getTransactionExceptionCode().name()))
            .build();

        GlobalLockQueryResponseProto result = GlobalLockQueryResponseProto.newBuilder().setLockable(
            globalLockQueryResponse.isLockable())
            .setAbstractTransactionResponse(abstractTransactionResponseProto).build();

        return result;
    }

    @Override
    public GlobalLockQueryResponse convert2Model(GlobalLockQueryResponseProto globalLockQueryResponseProto) {

        GlobalLockQueryResponse branchRegisterResponse = new GlobalLockQueryResponse();
        AbstractTransactionResponseProto branchRegisterResponseProto = globalLockQueryResponseProto
            .getAbstractTransactionResponse();
        final AbstractResultMessageProto abstractResultMessage = branchRegisterResponseProto.getAbstractResultMessage();
        branchRegisterResponse.setMsg(
            abstractResultMessage.getMsg());
        branchRegisterResponse.setResultCode(ResultCode.valueOf(abstractResultMessage.getResultCode().name()));
        branchRegisterResponse.setTransactionExceptionCode(TransactionExceptionCode.valueOf(
            branchRegisterResponseProto.getTransactionExceptionCode().name()));
        branchRegisterResponse.setLockable(globalLockQueryResponseProto.getLockable());
        return branchRegisterResponse;
    }
}