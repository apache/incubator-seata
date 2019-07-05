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

import io.seata.core.model.BranchType;
import io.seata.codec.protobuf.generated.AbstractBranchEndRequestProto;
import io.seata.codec.protobuf.generated.AbstractMessageProto;
import io.seata.codec.protobuf.generated.AbstractTransactionRequestProto;
import io.seata.codec.protobuf.generated.BranchRollbackRequestProto;
import io.seata.codec.protobuf.generated.BranchTypeProto;
import io.seata.codec.protobuf.generated.MessageTypeProto;
import io.seata.core.protocol.transaction.BranchRollbackRequest;

/**
 * @author leizhiyuan
 */
public class BranchRollbackRequestConvertor implements PbConvertor<BranchRollbackRequest, BranchRollbackRequestProto> {
    @Override
    public BranchRollbackRequestProto convert2Proto(BranchRollbackRequest branchRollbackRequest) {
        final short typeCode = branchRollbackRequest.getTypeCode();

        final AbstractMessageProto abstractMessage = AbstractMessageProto.newBuilder().setMessageType(
            MessageTypeProto.forNumber(typeCode)).build();

        final AbstractTransactionRequestProto abstractTransactionRequestProto = AbstractTransactionRequestProto
            .newBuilder().setAbstractMessage(
                abstractMessage).build();

        final String applicationData = branchRollbackRequest.getApplicationData();
        final String resourceId = branchRollbackRequest.getResourceId();
        final AbstractBranchEndRequestProto abstractBranchEndRequestProto = AbstractBranchEndRequestProto
            .newBuilder().
                setAbstractTransactionRequest(abstractTransactionRequestProto)
            .setXid(branchRollbackRequest.getXid())
            .setBranchId(branchRollbackRequest.getBranchId())
            .setBranchType(BranchTypeProto.valueOf(branchRollbackRequest.getBranchType().name()))
            .setApplicationData(applicationData==null?"":applicationData)
            .setResourceId(resourceId == null ? "" : resourceId)
            .build();

        BranchRollbackRequestProto result = BranchRollbackRequestProto.newBuilder().setAbstractBranchEndRequest(
            abstractBranchEndRequestProto).build();

        return result;
    }

    @Override
    public BranchRollbackRequest convert2Model(BranchRollbackRequestProto branchRollbackRequestProto) {
        BranchRollbackRequest branchCommitRequest = new BranchRollbackRequest();
        branchCommitRequest.setApplicationData(
            branchRollbackRequestProto.getAbstractBranchEndRequest().getApplicationData());
        branchCommitRequest.setBranchId(branchRollbackRequestProto.getAbstractBranchEndRequest().getBranchId());
        branchCommitRequest.setResourceId(branchRollbackRequestProto.getAbstractBranchEndRequest().getResourceId());
        branchCommitRequest.setXid(branchRollbackRequestProto.getAbstractBranchEndRequest().getXid());
        branchCommitRequest.setBranchType(
            BranchType.valueOf(branchRollbackRequestProto.getAbstractBranchEndRequest().getBranchType().name()));

        return branchCommitRequest;
    }
}