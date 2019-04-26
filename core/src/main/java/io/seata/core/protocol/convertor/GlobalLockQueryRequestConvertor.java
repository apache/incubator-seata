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

import io.seata.core.model.BranchType;
import io.seata.core.protocol.protobuf.AbstractMessageProto;
import io.seata.core.protocol.protobuf.AbstractTransactionRequestProto;
import io.seata.core.protocol.protobuf.BranchRegisterRequestProto;
import io.seata.core.protocol.protobuf.BranchTypeProto;
import io.seata.core.protocol.protobuf.GlobalLockQueryRequestProto;
import io.seata.core.protocol.protobuf.MessageTypeProto;
import io.seata.core.protocol.transaction.GlobalLockQueryRequest;

/**
 * @author leizhiyuan
 */
public class GlobalLockQueryRequestConvertor
    implements PbConvertor<GlobalLockQueryRequest, GlobalLockQueryRequestProto> {
    @Override
    public GlobalLockQueryRequestProto convert2Proto(GlobalLockQueryRequest globalLockQueryRequest) {
        final short typeCode = globalLockQueryRequest.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final AbstractTransactionRequestProto abstractTransactionRequestProto = AbstractTransactionRequestProto
            .newBuilder().setAbstractMessage(
                abstractMessage).build();

        BranchRegisterRequestProto branchRegisterRequestProto = BranchRegisterRequestProto.newBuilder()
            .setAbstractTransactionRequest(abstractTransactionRequestProto)
            .setApplicationData(globalLockQueryRequest.getApplicationData())
            .setBranchType(BranchTypeProto.valueOf(globalLockQueryRequest.getBranchType().name()))
            .setLockKey(globalLockQueryRequest.getLockKey())
            .setResourceId(globalLockQueryRequest.getResourceId())
            .setXid(globalLockQueryRequest.getXid())
            .build();

        GlobalLockQueryRequestProto result = GlobalLockQueryRequestProto.newBuilder().setBranchRegisterRequest(
            branchRegisterRequestProto).build();

        return result;
    }

    @Override
    public GlobalLockQueryRequest convert2Model(GlobalLockQueryRequestProto globalLockQueryRequestProto) {
        GlobalLockQueryRequest branchRegisterRequest = new GlobalLockQueryRequest();
        BranchRegisterRequestProto branchRegisterRequestProto = globalLockQueryRequestProto.getBranchRegisterRequest();
        branchRegisterRequest.setApplicationData(branchRegisterRequestProto.getApplicationData());
        branchRegisterRequest.setBranchType(BranchType.valueOf(branchRegisterRequestProto.getBranchType().name()));
        branchRegisterRequest.setLockKey(branchRegisterRequestProto.getLockKey());
        branchRegisterRequest.setResourceId(branchRegisterRequestProto.getResourceId());
        branchRegisterRequest.setXid(branchRegisterRequestProto.getXid());
        return branchRegisterRequest;
    }
}