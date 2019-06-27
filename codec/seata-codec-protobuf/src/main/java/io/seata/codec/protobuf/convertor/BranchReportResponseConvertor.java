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
import io.seata.codec.protobuf.generated.BranchReportResponseProto;
import io.seata.codec.protobuf.generated.MessageTypeProto;
import io.seata.codec.protobuf.generated.ResultCodeProto;
import io.seata.codec.protobuf.generated.TransactionExceptionCodeProto;
import io.seata.core.protocol.transaction.BranchReportResponse;

/**
 * @author leizhiyuan
 */
public class BranchReportResponseConvertor implements PbConvertor<BranchReportResponse, BranchReportResponseProto> {
    @Override
    public BranchReportResponseProto convert2Proto(BranchReportResponse branchReportResponse) {
        final short typeCode = branchReportResponse.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final String msg = branchReportResponse.getMsg();
        final AbstractResultMessageProto abstractResultMessageProto = AbstractResultMessageProto.newBuilder().setMsg(
            msg == null ? "" : msg)
            .setResultCode(ResultCodeProto.valueOf(branchReportResponse.getResultCode().name())).setAbstractMessage(
                abstractMessage).build();

        AbstractTransactionResponseProto abstractTransactionResponseProto = AbstractTransactionResponseProto
            .newBuilder().setAbstractResultMessage(abstractResultMessageProto)
            .setTransactionExceptionCode(
                TransactionExceptionCodeProto.valueOf(branchReportResponse.getTransactionExceptionCode().name()))
            .build();

        BranchReportResponseProto result = BranchReportResponseProto.newBuilder()
            .setAbstractTransactionResponse(abstractTransactionResponseProto).build();

        return result;
    }

    @Override
    public BranchReportResponse convert2Model(BranchReportResponseProto branchReportResponseProto) {
        BranchReportResponse branchRegisterResponse = new BranchReportResponse();
        final AbstractResultMessageProto abstractResultMessage = branchReportResponseProto
            .getAbstractTransactionResponse().getAbstractResultMessage();
        branchRegisterResponse.setMsg(
            abstractResultMessage.getMsg());
        branchRegisterResponse.setResultCode(ResultCode.valueOf(abstractResultMessage.getResultCode().name()));
        branchRegisterResponse.setTransactionExceptionCode(TransactionExceptionCode.valueOf(
            branchReportResponseProto.getAbstractTransactionResponse().getTransactionExceptionCode().name()));

        return branchRegisterResponse;
    }
}